#!/usr/bin/env python3

from __future__ import annotations

import argparse
import ast
import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, List

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from common import (
    classify_class,
    collect_relevant_files,
    guess_layer,
    read_json,
    read_text,
    relpath,
    sha256_file,
    unique_dicts,
    write_json,
)


CACHE_SCHEMA_VERSION = "5.0"


def empty_result(relative_path: str) -> Dict[str, Any]:
    return {
        "file": relative_path,
        "modules": [],
        "classes": [],
        "functions": [],
        "imports": [],
        "routes": [],
        "relationships": [],
        "entities": [],
        "dependencies": [],
        "schema_files": [],
    }


def analyze_file(path: Path, root: Path) -> Dict[str, Any]:
    if path.suffix == ".py":
        return analyze_python(path, root)
    if path.suffix in {".js", ".jsx", ".ts", ".tsx"}:
        return analyze_node(path, root)
    if path.suffix == ".java":
        return analyze_java(path, root)
    if path.suffix == ".cs":
        return analyze_csharp(path, root)
    if path.suffix == ".sql":
        return analyze_sql(path, root)
    if path.suffix == ".prisma" or path.name == "schema.prisma":
        return analyze_prisma(path, root)
    if path.name == "package.json":
        return analyze_package_json(path, root)
    return analyze_config(path, root)


def analyze_python(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    source = read_text(path)

    result["modules"].append({
        "file": relative,
        "language": "Python",
        "layer": guess_layer(relative),
    })

    try:
        tree = ast.parse(source)
    except SyntaxError:
        result["modules"][0]["parse_error"] = "SyntaxError"
        return result

    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                result["imports"].append({
                    "source": relative,
                    "target": alias.name,
                    "kind": "import",
                    "evidence": relative,
                })

        elif isinstance(node, ast.ImportFrom):
            if node.module:
                result["imports"].append({
                    "source": relative,
                    "target": node.module,
                    "kind": "from_import",
                    "evidence": relative,
                })

        elif isinstance(node, ast.ClassDef):
            bases = [python_expr(base) for base in node.bases if python_expr(base)]
            decorators = [python_expr(dec) for dec in node.decorator_list if python_expr(dec)]
            methods: List[str] = []
            fields: List[str] = []

            for item in node.body:
                if isinstance(item, ast.FunctionDef):
                    methods.append(item.name)
                    if item.name == "__init__":
                        fields.extend(extract_python_init_fields(item))
                elif isinstance(item, ast.Assign):
                    for target in item.targets:
                        field = python_field(target)
                        if field:
                            fields.append(field)
                elif isinstance(item, ast.AnnAssign):
                    field = python_field(item.target)
                    if field:
                        fields.append(field)

            kind = classify_class(node.name, relative, decorators)

            result["classes"].append({
                "name": node.name,
                "file": relative,
                "language": "Python",
                "kind": kind,
                "layer": guess_layer(relative),
                "bases": sorted(set(bases)),
                "decorators": decorators,
                "methods": sorted(set(methods)),
                "fields": sorted(set(fields)),
                "evidence": relative,
            })

            for base in bases:
                result["relationships"].append({
                    "type": "inherits",
                    "source": node.name,
                    "target": base,
                    "evidence": relative,
                })

            if kind == "entity":
                result["entities"].append({
                    "name": node.name,
                    "file": relative,
                    "fields": sorted(set(fields)),
                    "source": "python-class-heuristic",
                    "evidence": relative,
                })

        elif isinstance(node, ast.FunctionDef):
            result["functions"].append({
                "name": node.name,
                "file": relative,
                "language": "Python",
                "layer": guess_layer(relative),
                "evidence": relative,
            })

            route = extract_python_route(node)
            if route:
                result["routes"].append({
                    "method": route["method"],
                    "path": route["path"],
                    "handler": node.name,
                    "file": relative,
                    "framework": "Python",
                    "evidence": relative,
                })

    return result


def python_expr(node: ast.AST) -> str:
    if isinstance(node, ast.Name):
        return node.id
    if isinstance(node, ast.Attribute):
        parent = python_expr(node.value)
        return f"{parent}.{node.attr}" if parent else node.attr
    if isinstance(node, ast.Call):
        return python_expr(node.func)
    if isinstance(node, ast.Subscript):
        return python_expr(node.value)
    return ""


def python_field(node: ast.AST) -> str:
    if isinstance(node, ast.Attribute) and isinstance(node.value, ast.Name) and node.value.id == "self":
        return node.attr
    if isinstance(node, ast.Name):
        return node.id
    return ""


def extract_python_init_fields(node: ast.FunctionDef) -> List[str]:
    fields: List[str] = []
    for child in ast.walk(node):
        if isinstance(child, ast.Assign):
            for target in child.targets:
                field = python_field(target)
                if field:
                    fields.append(field)
        elif isinstance(child, ast.AnnAssign):
            field = python_field(child.target)
            if field:
                fields.append(field)
    return fields


def extract_python_route(node: ast.FunctionDef) -> Dict[str, str] | None:
    route_names = {"route", "get", "post", "put", "patch", "delete", "api_route"}

    for dec in node.decorator_list:
        if not isinstance(dec, ast.Call):
            continue

        func_name = python_expr(dec.func)
        method = func_name.split(".")[-1].lower()

        if method not in route_names:
            continue

        path = None

        if dec.args and isinstance(dec.args[0], ast.Constant) and isinstance(dec.args[0].value, str):
            path = dec.args[0].value

        for keyword in dec.keywords:
            if keyword.arg in {"path", "url", "rule"}:
                if isinstance(keyword.value, ast.Constant) and isinstance(keyword.value.value, str):
                    path = keyword.value.value

        if path:
            return {
                "method": "ANY" if method == "route" else method.upper(),
                "path": path,
            }

    return None


def analyze_node(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    source = read_text(path)
    language = "TypeScript" if path.suffix in {".ts", ".tsx"} else "JavaScript"

    result["modules"].append({
        "file": relative,
        "language": language,
        "layer": guess_layer(relative),
    })

    import_patterns = [
        r"""import\s+.*?\s+from\s+["']([^"']+)["']""",
        r"""import\s+["']([^"']+)["']""",
        r"""require\(\s*["']([^"']+)["']\s*\)""",
    ]

    for pattern in import_patterns:
        for match in re.finditer(pattern, source):
            result["imports"].append({
                "source": relative,
                "target": match.group(1),
                "kind": "import",
                "evidence": relative,
            })

    class_pattern = re.compile(
        r"(?:export\s+)?(?:default\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_\.]*))?"
    )

    interface_pattern = re.compile(
        r"(?:export\s+)?interface\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_\.]*))?"
    )

    for match in class_pattern.finditer(source):
        name = match.group(1)
        base = match.group(2)
        result["classes"].append({
            "name": name,
            "file": relative,
            "language": language,
            "kind": classify_class(name, relative),
            "layer": guess_layer(relative),
            "bases": [base] if base else [],
            "methods": extract_js_methods(source[match.end():match.end() + 4000]),
            "fields": [],
            "evidence": relative,
        })

        if base:
            result["relationships"].append({
                "type": "inherits",
                "source": name,
                "target": base,
                "evidence": relative,
            })

    for match in interface_pattern.finditer(source):
        name = match.group(1)
        base = match.group(2)
        result["classes"].append({
            "name": name,
            "file": relative,
            "language": language,
            "kind": "interface",
            "layer": guess_layer(relative),
            "bases": [base] if base else [],
            "methods": [],
            "fields": [],
            "evidence": relative,
        })

    if path.suffix in {".jsx", ".tsx"}:
        component_pattern = re.compile(r"(?:export\s+)?(?:function|const)\s+([A-Z][A-Za-z0-9_]*)")
        for match in component_pattern.finditer(source):
            result["classes"].append({
                "name": match.group(1),
                "file": relative,
                "language": "React",
                "kind": "ui_component",
                "layer": "ui",
                "bases": [],
                "methods": [],
                "fields": [],
                "evidence": relative,
            })

    express_pattern = re.compile(
        r"""(?:app|router)\.(get|post|put|patch|delete)\s*\(\s*["']([^"']+)["']""",
        flags=re.IGNORECASE,
    )

    for match in express_pattern.finditer(source):
        result["routes"].append({
            "method": match.group(1).upper(),
            "path": match.group(2),
            "handler": "express-handler",
            "file": relative,
            "framework": "Express-like",
            "evidence": relative,
        })

    controller_match = re.search(r"""@Controller\(\s*["']([^"']*)["']\s*\)""", source)
    controller_prefix = controller_match.group(1).strip("/") if controller_match else ""

    nest_pattern = re.compile(
        r"""@(Get|Post|Put|Patch|Delete)\(\s*(?:["']([^"']*)["'])?\s*\)\s*"""
        r"""(?:public\s+|private\s+|protected\s+)?(?:async\s+)?([A-Za-z_][A-Za-z0-9_]*)"""
    )

    for match in nest_pattern.finditer(source):
        method = match.group(1).upper()
        route_part = match.group(2) or ""
        handler = match.group(3)
        full_path = "/" + "/".join(
            p.strip("/") for p in [controller_prefix, route_part] if p.strip("/")
        )

        result["routes"].append({
            "method": method,
            "path": full_path or "/",
            "handler": handler,
            "file": relative,
            "framework": "NestJS",
            "evidence": relative,
        })

    return result


def extract_js_methods(source: str) -> List[str]:
    methods: List[str] = []
    pattern = re.compile(r"^\s*(?:async\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\(", flags=re.MULTILINE)

    for match in pattern.finditer(source):
        name = match.group(1)
        if name not in {"if", "for", "while", "switch", "catch"}:
            methods.append(name)

    return sorted(set(methods))


def analyze_java(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    source = read_text(path)

    result["modules"].append({
        "file": relative,
        "language": "Java",
        "layer": guess_layer(relative),
    })

    for match in re.finditer(r"^\s*import\s+(?:static\s+)?([A-Za-z0-9_.*]+)\s*;", source, flags=re.MULTILINE):
        result["imports"].append({
            "source": relative,
            "target": match.group(1),
            "kind": "import",
            "evidence": relative,
        })

    class_pattern = re.compile(
        r"((?:@\w+(?:\([^)]*\))?\s*)*)"
        r"(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+)*"
        r"(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_<>.,\s]*))?"
        r"(?:\s+implements\s+([A-Za-z_][A-Za-z0-9_<>.,\s]*))?",
        flags=re.MULTILINE,
    )

    for match in class_pattern.finditer(source):
        annotations = re.findall(r"@([A-Za-z_][A-Za-z0-9_]*)", match.group(1) or "")
        kind_token = match.group(2)
        name = match.group(3)
        bases = split_type_list((match.group(4) or "") + "," + (match.group(5) or ""))
        kind = classify_class(name, relative, annotations)

        if kind_token == "interface":
            kind = "interface"
        elif kind_token == "enum":
            kind = "enum"
        elif kind_token == "record":
            kind = "record"
        elif any(a in {"Entity", "Table", "Document"} for a in annotations):
            kind = "entity"

        fields = extract_java_fields(source)
        methods = extract_java_methods(source)

        result["classes"].append({
            "name": name,
            "file": relative,
            "language": "Java",
            "kind": kind,
            "layer": guess_layer(relative),
            "annotations": annotations,
            "bases": bases,
            "methods": methods,
            "fields": fields,
            "evidence": relative,
        })

        for base in bases:
            result["relationships"].append({
                "type": "inherits_or_implements",
                "source": name,
                "target": base,
                "evidence": relative,
            })

        if kind == "entity":
            result["entities"].append({
                "name": name,
                "file": relative,
                "fields": fields,
                "source": "java-entity-heuristic",
                "evidence": relative,
            })

    mapping_pattern = re.compile(
        r"""@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)"""
        r"""\(\s*(?:(?:value|path)\s*=\s*)?["']?([^"'\)]*)["']?"""
    )

    method_map = {
        "GetMapping": "GET",
        "PostMapping": "POST",
        "PutMapping": "PUT",
        "PatchMapping": "PATCH",
        "DeleteMapping": "DELETE",
        "RequestMapping": "ANY",
    }

    for match in mapping_pattern.finditer(source):
        route_path = match.group(2).strip()
        if "=" in route_path:
            route_path = ""

        result["routes"].append({
            "method": method_map.get(match.group(1), "ANY"),
            "path": "/" + route_path.strip("/") if route_path else "/",
            "handler": match.group(1),
            "file": relative,
            "framework": "Spring-like",
            "evidence": relative,
        })

    return result


def split_type_list(value: str) -> List[str]:
    clean = re.sub(r"<[^>]+>", "", value or "")
    return [x.strip() for x in clean.split(",") if x.strip()]


def extract_java_fields(source: str) -> List[str]:
    pattern = re.compile(
        r"^\s*(?:private|protected|public)\s+(?:final\s+|static\s+)*"
        r"[A-Za-z_][A-Za-z0-9_<>.?]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*[;=]",
        flags=re.MULTILINE,
    )
    return sorted(set(match.group(1) for match in pattern.finditer(source)))


def extract_java_methods(source: str) -> List[str]:
    pattern = re.compile(
        r"^\s*(?:public|private|protected)\s+(?:static\s+|final\s+|abstract\s+)*"
        r"[A-Za-z_][A-Za-z0-9_<>.?]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(",
        flags=re.MULTILINE,
    )
    return sorted(set(match.group(1) for match in pattern.finditer(source)))


def analyze_csharp(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    source = read_text(path)

    result["modules"].append({
        "file": relative,
        "language": ".NET/C#",
        "layer": guess_layer(relative),
    })

    for match in re.finditer(r"^\s*using\s+([A-Za-z0-9_.]+)\s*;", source, flags=re.MULTILINE):
        result["imports"].append({
            "source": relative,
            "target": match.group(1),
            "kind": "using",
            "evidence": relative,
        })

    class_pattern = re.compile(
        r"((?:\[[^\]]+\]\s*)*)"
        r"(?:public|private|protected|internal)?\s*"
        r"(?:partial\s+|abstract\s+|sealed\s+)*"
        r"(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"(?:\s*:\s*([A-Za-z0-9_<>,\s.]+))?",
        flags=re.MULTILINE,
    )

    for match in class_pattern.finditer(source):
        attrs = re.findall(r"\[([A-Za-z_][A-Za-z0-9_]*)", match.group(1) or "")
        kind_token = match.group(2)
        name = match.group(3)
        bases = split_type_list(match.group(4) or "")
        kind = classify_class(name, relative, attrs)

        if kind_token == "interface":
            kind = "interface"
        elif kind_token == "enum":
            kind = "enum"
        elif kind_token == "record":
            kind = "record"

        fields = extract_csharp_properties(source)
        methods = extract_csharp_methods(source)

        result["classes"].append({
            "name": name,
            "file": relative,
            "language": ".NET/C#",
            "kind": kind,
            "layer": guess_layer(relative),
            "annotations": attrs,
            "bases": bases,
            "methods": methods,
            "fields": fields,
            "evidence": relative,
        })

        for base in bases:
            result["relationships"].append({
                "type": "inherits_or_implements",
                "source": name,
                "target": base,
                "evidence": relative,
            })

        if kind == "entity":
            result["entities"].append({
                "name": name,
                "file": relative,
                "fields": fields,
                "source": "csharp-entity-heuristic",
                "evidence": relative,
            })

    route_pattern = re.compile(r"""\[(HttpGet|HttpPost|HttpPut|HttpPatch|HttpDelete)(?:\(\s*"([^"]*)"\s*\))?\]""")
    method_map = {
        "HttpGet": "GET",
        "HttpPost": "POST",
        "HttpPut": "PUT",
        "HttpPatch": "PATCH",
        "HttpDelete": "DELETE",
    }

    for match in route_pattern.finditer(source):
        result["routes"].append({
            "method": method_map.get(match.group(1), "ANY"),
            "path": "/" + (match.group(2) or "").strip("/"),
            "handler": match.group(1),
            "file": relative,
            "framework": "ASP.NET-like",
            "evidence": relative,
        })

    return result


def extract_csharp_properties(source: str) -> List[str]:
    pattern = re.compile(
        r"^\s*(?:public|private|protected|internal)\s+"
        r"[A-Za-z_][A-Za-z0-9_<>\.?]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*{\s*get;",
        flags=re.MULTILINE,
    )
    return sorted(set(match.group(1) for match in pattern.finditer(source)))


def extract_csharp_methods(source: str) -> List[str]:
    pattern = re.compile(
        r"^\s*(?:public|private|protected|internal)\s+"
        r"(?:async\s+)?[A-Za-z_][A-Za-z0-9_<>\.?]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(",
        flags=re.MULTILINE,
    )
    return sorted(set(match.group(1) for match in pattern.finditer(source)))


def analyze_sql(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    result["schema_files"].append(relative)
    source = read_text(path)

    table_pattern = re.compile(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`\"\[]?([A-Za-z_][A-Za-z0-9_]*)[`\"\]]?\s*\((.*?)\);",
        flags=re.IGNORECASE | re.DOTALL,
    )

    for match in table_pattern.finditer(source):
        table = match.group(1)
        body = match.group(2)
        fields = []

        for line in body.splitlines():
            line = line.strip().rstrip(",")
            if not line or line.upper().startswith(("PRIMARY", "FOREIGN", "CONSTRAINT", "UNIQUE", "KEY")):
                continue

            col = re.match(r"[`\"\[]?([A-Za-z_][A-Za-z0-9_]*)[`\"\]]?\s+([A-Za-z0-9_()]+)", line)
            if col:
                fields.append({
                    "name": col.group(1),
                    "type": col.group(2),
                })

        result["entities"].append({
            "name": table,
            "file": relative,
            "fields": fields,
            "source": "sql",
            "evidence": relative,
        })

        fk_pattern = re.compile(
            r"FOREIGN\s+KEY\s*\(([^)]+)\)\s+REFERENCES\s+[`\"\[]?([A-Za-z_][A-Za-z0-9_]*)[`\"\]]?",
            flags=re.IGNORECASE,
        )

        for fk in fk_pattern.finditer(body):
            result["relationships"].append({
                "type": "foreign_key",
                "source": table,
                "target": fk.group(2),
                "field": fk.group(1).strip().strip("`\"[]"),
                "evidence": relative,
            })

    return result


def analyze_prisma(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    result["schema_files"].append(relative)
    source = read_text(path)

    model_pattern = re.compile(r"model\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{(.*?)\}", flags=re.DOTALL)
    model_names = {match.group(1) for match in model_pattern.finditer(source)}

    for match in model_pattern.finditer(source):
        model = match.group(1)
        body = match.group(2)
        fields = []

        for raw in body.splitlines():
            line = raw.strip()
            if not line or line.startswith("//"):
                continue

            parts = line.split()
            if len(parts) >= 2:
                field = parts[0]
                field_type = parts[1]
                fields.append({
                    "name": field,
                    "type": field_type,
                })

                normalized = field_type.replace("?", "").replace("[]", "")
                if normalized in model_names:
                    result["relationships"].append({
                        "type": "prisma_relation",
                        "source": model,
                        "target": normalized,
                        "field": field,
                        "evidence": relative,
                    })

        result["entities"].append({
            "name": model,
            "file": relative,
            "fields": fields,
            "source": "prisma",
            "evidence": relative,
        })

    return result


def analyze_package_json(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)

    try:
        data = json.loads(read_text(path))
    except Exception:
        return result

    for section in ["dependencies", "devDependencies", "peerDependencies"]:
        for name, version in data.get(section, {}).items():
            result["dependencies"].append({
                "name": name,
                "version": version,
                "section": section,
                "file": relative,
                "ecosystem": "npm",
                "evidence": relative,
            })

    result["modules"].append({
        "file": relative,
        "language": "Node.js manifest",
        "layer": "configuration",
    })

    return result


def analyze_config(path: Path, root: Path) -> Dict[str, Any]:
    relative = relpath(path, root)
    result = empty_result(relative)
    result["modules"].append({
        "file": relative,
        "language": "configuration",
        "layer": "configuration",
    })
    return result


def merge_results(file_results: Dict[str, Dict[str, Any]]) -> Dict[str, Any]:
    merged = {
        "modules": [],
        "classes": [],
        "functions": [],
        "imports": [],
        "routes": [],
        "relationships": [],
        "entities": [],
        "dependencies": [],
        "schema_files": [],
    }

    for result in file_results.values():
        for key in merged:
            value = result.get(key, [])
            if isinstance(value, list):
                merged[key].extend(value)

    merged["modules"] = unique_dicts(merged["modules"], ["file", "language"])
    merged["classes"] = unique_dicts(merged["classes"], ["name", "file"])
    merged["functions"] = unique_dicts(merged["functions"], ["name", "file"])
    merged["imports"] = unique_dicts(merged["imports"], ["source", "target", "kind"])
    merged["routes"] = unique_dicts(merged["routes"], ["method", "path", "file"])
    merged["relationships"] = unique_dicts(merged["relationships"], ["type", "source", "target", "evidence"])
    merged["entities"] = unique_dicts(merged["entities"], ["name", "file"])
    merged["dependencies"] = unique_dicts(merged["dependencies"], ["name", "version", "section", "file"])
    merged["schema_files"] = sorted(set(merged["schema_files"]))

    return merged


def detect_technology(file_results: Dict[str, Dict[str, Any]]) -> Dict[str, Any]:
    languages: Dict[str, int] = {}
    build_files = []

    extension_language = {
        ".py": "Python",
        ".java": "Java",
        ".cs": ".NET/C#",
        ".js": "JavaScript",
        ".jsx": "JavaScript",
        ".ts": "TypeScript",
        ".tsx": "TypeScript",
        ".sql": "SQL",
        ".prisma": "Prisma",
    }

    build_names = {
        "package.json",
        "pom.xml",
        "build.gradle",
        "settings.gradle",
        "pyproject.toml",
        "requirements.txt",
        "setup.py",
        "Pipfile",
        "Dockerfile",
        "docker-compose.yml",
        "docker-compose.yaml",
    }

    for file_path in file_results:
        path = Path(file_path)
        language = extension_language.get(path.suffix)
        if language:
            languages[language] = languages.get(language, 0) + 1

        if path.name in build_names:
            build_files.append({
                "file": file_path,
                "type": path.name,
            })

    return {
        "languages": languages,
        "build_files": build_files,
    }


def build_delta(previous_hashes: Dict[str, str], current_hashes: Dict[str, str], force: bool) -> Dict[str, Any]:
    previous_files = set(previous_hashes)
    current_files = set(current_hashes)

    added = sorted(current_files - previous_files)
    removed = sorted(previous_files - current_files)

    modified = sorted(
        file_path for file_path in current_files & previous_files
        if previous_hashes.get(file_path) != current_hashes.get(file_path)
    )

    unchanged = sorted(
        file_path for file_path in current_files & previous_files
        if previous_hashes.get(file_path) == current_hashes.get(file_path)
    )

    if force:
        modified = sorted(current_files)
        unchanged = []

    return {
        "added": added,
        "modified": modified,
        "removed": removed,
        "unchanged": unchanged,
        "summary": {
            "added": len(added),
            "modified": len(modified),
            "removed": len(removed),
            "unchanged": len(unchanged),
            "force": force,
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--model", default="docs/.architecture/architecture_model.json")
    parser.add_argument("--cache", default="docs/.architecture/architecture_cache.json")
    parser.add_argument("--delta", default="docs/.architecture/architecture_delta.json")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    model_path = Path(args.model)
    cache_path = Path(args.cache)
    delta_path = Path(args.delta)

    previous_cache = read_json(cache_path, default={})
    previous_hashes = previous_cache.get("file_hashes", {})
    previous_results = previous_cache.get("file_results", {})

    if previous_cache.get("schema_version") != CACHE_SCHEMA_VERSION:
        previous_hashes = {}
        previous_results = {}

    relevant_files = collect_relevant_files(root)
    current_hashes = {
        relpath(path, root): sha256_file(path)
        for path in relevant_files
    }

    delta = build_delta(previous_hashes, current_hashes, args.force)

    file_results: Dict[str, Dict[str, Any]] = {}

    for relative in delta["unchanged"]:
        if relative in previous_results:
            file_results[relative] = previous_results[relative]

    changed_files = set(delta["added"] + delta["modified"])

    for path in relevant_files:
        relative = relpath(path, root)
        if relative in changed_files:
            file_results[relative] = analyze_file(path, root)

    analysis = merge_results(file_results)
    technology = detect_technology(file_results)

    model = {
        "schema_version": CACHE_SCHEMA_VERSION,
        "repository": str(root),
        "technology": technology,
        "summary": {
            "module_count": len(analysis["modules"]),
            "class_count": len(analysis["classes"]),
            "route_count": len(analysis["routes"]),
            "entity_count": len(analysis["entities"]),
            "relationship_count": len(analysis["relationships"]),
            "dependency_count": len(analysis["dependencies"]),
        },
        "analysis": analysis,
        "delta": delta,
        "notes": [
            "Generated by incremental static analysis.",
            "Unchanged files were reused from cache.",
            "Runtime dependency injection and dynamic behavior may need manual verification.",
        ],
    }

    cache = {
        "schema_version": CACHE_SCHEMA_VERSION,
        "file_hashes": current_hashes,
        "file_results": file_results,
    }

    write_json(model_path, model)
    write_json(cache_path, cache)
    write_json(delta_path, delta)

    print("Incremental analysis complete.")
    print(f"Added: {delta['summary']['added']}")
    print(f"Modified: {delta['summary']['modified']}")
    print(f"Removed: {delta['summary']['removed']}")
    print(f"Unchanged reused from cache: {delta['summary']['unchanged']}")
    print(f"Model: {model_path}")
    print(f"Delta: {delta_path}")


if __name__ == "__main__":
    main()