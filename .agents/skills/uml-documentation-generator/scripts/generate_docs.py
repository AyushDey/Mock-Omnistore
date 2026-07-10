#!/usr/bin/env python3

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any, Dict, List

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from common import read_json, safe_mermaid_name, write_text


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    model = read_json(Path(args.model))
    output = Path(args.output)

    write_text(output / "architecture.md", architecture_doc(model))
    write_text(output / "system-overview.md", system_overview_doc(model))
    write_text(output / "components.md", components_doc(model))
    write_text(output / "api-reference.md", api_doc(model))
    write_text(output / "onboarding.md", onboarding_doc(model))
    write_text(output / "architecture-review.md", review_doc(model))

    diagrams = output / "diagrams"
    write_text(diagrams / "component-diagram.md", "# Component Diagram\n\n" + mermaid_component_diagram(model) + "\n")
    write_text(diagrams / "class-diagram.md", "# Class Diagram Index\n\n" + mermaid_class_diagram(model, diagrams) + "\n")
    write_text(diagrams / "sequence-diagrams.md", "# Sequence Diagram\n\n" + mermaid_sequence_diagram(model) + "\n")
    write_text(diagrams / "er-diagram.md", "# ER Diagram\n\n" + mermaid_er_diagram(model) + "\n")

    print(f"Generated documentation at {output}")


def architecture_doc(model: Dict[str, Any]) -> str:
    summary = model.get("summary", {})
    tech = model.get("technology", {})
    delta = model.get("delta", {}).get("summary", {})

    return f"""# Architecture Overview

## Purpose

This document summarizes the architecture discovered through incremental static analysis.

## Incremental Analysis Summary

- Added files: {delta.get("added", 0)}
- Modified files: {delta.get("modified", 0)}
- Removed files: {delta.get("removed", 0)}
- Unchanged files reused from cache: {delta.get("unchanged", 0)}

## Repository Summary

- Modules discovered: {summary.get("module_count", 0)}
- Classes discovered: {summary.get("class_count", 0)}
- API routes discovered: {summary.get("route_count", 0)}
- Entities discovered: {summary.get("entity_count", 0)}
- Relationships discovered: {summary.get("relationship_count", 0)}
- Dependencies discovered: {summary.get("dependency_count", 0)}

## Technology Stack

### Languages

{format_dict(tech.get("languages", {}))}

### Build and Dependency Files

{format_build_files(tech.get("build_files", []))}

## High-Level Architecture

{mermaid_component_diagram(model)}

## Assumptions

- This documentation is generated from static analysis.
- Runtime dependency injection, reflection, dynamic imports, and generated code may require manual review.

## Recommendations

- Review diagrams with maintainers.
- Keep generated docs under `docs/generated/`.
- Keep human-authored docs separately, for example under `docs/adr/`.
"""


def get_subproject(file_path: str) -> str:
    if file_path.startswith("backend/"):
        return "Omnistore Backend API"
    if file_path.startswith("Quotation/"):
        return "Quotation Microservice"
    if file_path.startswith("frontend/"):
        return "Frontend Angular Client"
    if file_path.startswith(".agents/"):
        return "UML Documentation Generator (Agent Skills)"
    return "Other / Common Components"


def system_overview_doc(model: Dict[str, Any]) -> str:
    analysis = model.get("analysis", {})
    modules = analysis.get("modules", [])
    routes = analysis.get("routes", [])
    entities = analysis.get("entities", [])
    classes = analysis.get("classes", [])

    class_fields = {}
    for c in classes:
        name = c.get("name")
        if name:
            fields = c.get("fields", [])
            field_names = [f.get("name") if isinstance(f, dict) else str(f) for f in fields]
            class_fields[name] = field_names

    module_groups = {}
    for m in modules:
        sub = get_subproject(m.get("file", ""))
        module_groups.setdefault(sub, []).append(m)

    modules_section = []
    for sub, items in sorted(module_groups.items()):
        modules_section.append(f"### {sub}\n")
        modules_section.append("| Module File / Path | Language | Architectural Layer |")
        modules_section.append("| :--- | :--- | :--- |")
        for item in sorted(items, key=lambda x: x.get("file", "")):
            file_path = item.get("file", "")
            lang = item.get("language", "unknown")
            layer = item.get("layer", "unknown").capitalize()
            modules_section.append(f"| `{file_path}` | {lang} | {layer} |")
        modules_section.append("")

    route_groups = {}
    for r in routes:
        sub = get_subproject(r.get("file", ""))
        route_groups.setdefault(sub, []).append(r)

    routes_section = []
    if not route_groups:
        routes_section.append("No API endpoints detected.")
    else:
        for sub, items in sorted(route_groups.items()):
            routes_section.append(f"### {sub}\n")
            routes_section.append("| Method | Endpoint Path | Code Handler | Framework |")
            routes_section.append("| :--- | :--- | :--- | :--- |")
            for item in sorted(items, key=lambda x: (x.get("path", ""), x.get("method", ""))):
                method = item.get("method", "GET").upper()
                path = item.get("path", "/")
                handler = item.get("handler", "unknown")
                framework = item.get("framework", "unknown")
                routes_section.append(f"| `{method}` | `{path}` | `{handler}` | {framework} |")
            routes_section.append("")

    entity_groups = {}
    for e in entities:
        sub = get_subproject(e.get("file", ""))
        entity_groups.setdefault(sub, []).append(e)

    entities_section = []
    if not entity_groups:
        entities_section.append("No domain entities detected.")
    else:
        for sub, items in sorted(entity_groups.items()):
            entities_section.append(f"### {sub}\n")
            entities_section.append("| Entity Name | Source File | Discovered Attributes / Fields |")
            entities_section.append("| :--- | :--- | :--- |")
            for item in sorted(items, key=lambda x: x.get("name", "")):
                name = item.get("name", "Unknown")
                file_path = item.get("file", "")
                
                fields = item.get("fields", [])
                if not fields and name in class_fields:
                    fields = class_fields[name]
                
                field_list = [f.get("name") if isinstance(f, dict) else str(f) for f in fields]
                field_str = ", ".join(f"`{f}`" for f in field_list[:20]) if field_list else "_None or inherited_"
                entities_section.append(f"| **{name}** | `{file_path}` | {field_str} |")
            entities_section.append("")

    return f"""# System Overview

This document provides a logical structural overview of the codebase components, classified by system modules, API endpoints, and database schemas.

## Codebase Modules

The code modules discovered during incremental static analysis:

{"\n".join(modules_section)}

## API Routes & Endpoints

REST API endpoints exposed by the backend services:

{"\n".join(routes_section)}

## Database & Domain Entities

Data models, database tables, and schema mappings:

{"\n".join(entities_section)}
"""


def components_doc(model: Dict[str, Any]) -> str:
    classes = model.get("analysis", {}).get("classes", [])
    lines = ["# Components", ""]

    if not classes:
        return "# Components\n\nNo classes or class-like structures were detected.\n"

    for item in classes:
        lines.append(f"## {item.get('name')}")
        lines.append("")
        lines.append(f"- Kind: `{item.get('kind', 'class')}`")
        lines.append(f"- Layer: `{item.get('layer', 'unknown')}`")
        lines.append(f"- Language: `{item.get('language', 'unknown')}`")
        lines.append(f"- Source: `{item.get('file')}`")

        if item.get("bases"):
            lines.append("- Bases: " + ", ".join(f"`{x}`" for x in item.get("bases", [])))

        if item.get("methods"):
            lines.append("- Methods: " + ", ".join(f"`{x}`" for x in item.get("methods", [])[:30]))

        if item.get("fields"):
            field_names = []
            for field in item.get("fields", []):
                field_names.append(field.get("name") if isinstance(field, dict) else str(field))
            lines.append("- Fields: " + ", ".join(f"`{x}`" for x in field_names[:30]))

        lines.append("")

    return "\n".join(lines)

# Dependency analysis document generation removed as requested.


def api_doc(model: Dict[str, Any]) -> str:
    routes = model.get("analysis", {}).get("routes", [])
    lines = ["# API Reference", ""]

    if not routes:
        return "# API Reference\n\nNo API routes were detected.\n"

    for route in routes:
        lines.append(f"## `{route.get('method', 'ANY')} {route.get('path', '/')}`")
        lines.append("")
        lines.append(f"- Framework: `{route.get('framework', 'unknown')}`")
        lines.append(f"- Handler: `{route.get('handler', 'unknown')}`")
        lines.append(f"- Source: `{route.get('file')}`")
        lines.append("- Request: Needs confirmation.")
        lines.append("- Response: Needs confirmation.")
        lines.append("")

    return "\n".join(lines)


def onboarding_doc(model: Dict[str, Any]) -> str:
    tech = model.get("technology", {})
    return f"""# Developer Onboarding Guide

## Technology Stack

{format_dict(tech.get("languages", {}))}

## Build and Dependency Files

{format_build_files(tech.get("build_files", []))}

## Recommended Reading

- `architecture.md`
- `system-overview.md`
- `components.md`
- `api-reference.md`
- `architecture-review.md`
- `diagrams/component-diagram.md`
- `diagrams/class-diagram.md`
- `diagrams/sequence-diagrams.md`
- `diagrams/er-diagram.md`

## Notes

Local setup, testing, and deployment commands should be verified from project README, package scripts, build files, Docker files, or CI configuration.
"""


def review_doc(model: Dict[str, Any]) -> str:
    classes = model.get("analysis", {}).get("classes", [])
    imports = model.get("analysis", {}).get("imports", [])

    large_classes = [
        c for c in classes
        if len(c.get("methods", [])) >= 15 or len(c.get("fields", [])) >= 25
    ]

    import_count: Dict[str, int] = {}
    for imp in imports:
        source = imp.get("source", "")
        import_count[source] = import_count.get(source, 0) + 1

    heavy_importers = [source for source, count in import_count.items() if count >= 20]

    return f"""# Architecture Review

## Potential Large Classes

{format_large_classes(large_classes)}

## Files With Many Imports

{format_list(heavy_importers)}

## Risks To Review

- Circular dependencies
- Tight coupling
- Large services
- Domain logic inside controllers
- Infrastructure dependencies inside domain modules
- Runtime dependency injection not visible through static analysis

## Recommendations

- Validate generated diagrams with maintainers.
- Refactor large classes or services.
- Add architecture decision records.
- Add explicit module boundaries.
"""


def mermaid_component_diagram(model: Dict[str, Any]) -> str:
    classes = model.get("analysis", {}).get("classes", [])
    files = {c.get("file", "") for c in classes}

    has_backend = any(f.startswith("backend/") for f in files)
    has_frontend = any(f.startswith("frontend/") for f in files)
    has_quotation = any(f.startswith("Quotation/") for f in files)

    lines = ["```mermaid", "flowchart TD"]

    if has_backend and has_frontend and has_quotation:
        lines.extend([
            "    User[\"User / Web Browser\"]",
            "",
            "    subgraph Frontend[\"Frontend App (Angular Client)\"]",
            "        UI[\"Angular Components\"]",
            "        ServiceJS[\"TransactionService\"]",
            "    end",
            "",
            "    subgraph Backend[\"Omnistore Backend API (Spring Boot)\"]",
            "        TransController[\"TransactionController\"]",
            "        TransService[\"TransactionService\"]",
            "        TransItem[\"TransactionItem Entity\"]",
            "    end",
            "",
            "    subgraph QuotationService[\"Quotation Microservice (Spring Boot)\"]",
            "        QuoteController[\"QuotationController\"]",
            "        QuoteItem[\"QuotationItem Entity\"]",
            "    end",
            "",
            "    User -->|HTTP / Port 4200| UI",
            "    UI -->|Uses| ServiceJS",
            "    ServiceJS -->|REST API / Port 8080| TransController",
            "    TransController -->|Delegates to| TransService",
            "    TransService -->|REST API / Port 8081| QuoteController",
            "    TransService -->|Persists| TransItem",
            "    QuoteController -->|Retrieves| QuoteItem",
        ])
    else:
        has_ui = any(c.get("layer") == "ui" or c.get("kind") == "ui_component" for c in classes)
        has_api = any(c.get("kind") == "controller" for c in classes)
        has_service = any(c.get("kind") == "service" or c.get("layer") == "service" for c in classes)
        has_repo = any(c.get("kind") == "repository" or c.get("layer") == "persistence" for c in classes)
        has_data = any(c.get("kind") == "entity" for c in classes)

        lines.append("    User[\"User\"]")
        if has_ui:
            lines.append("    User --> UI[\"UI / Client Layer\"]")
            if has_api:
                lines.append("    UI --> API[\"API / Controller Layer\"]")
        elif has_api:
            lines.append("    User --> API[\"API / Controller Layer\"]")
        else:
            lines.append("    User --> App[\"Application\"]")

        if has_api and has_service:
            lines.append("    API --> Service[\"Service Layer\"]")
        elif has_api:
            lines.append("    API --> App[\"Application Core\"]")

        if has_service and has_repo:
            lines.append("    Service --> Repository[\"Persistence Layer\"]")
        elif has_service and has_data:
            lines.append("    Service --> Data[(\"Data Store\")]")

        if has_repo and has_data:
            lines.append("    Repository --> Data[(\"Data Store\")]")
        elif has_data:
            lines.append("    App --> Data[(\"Data Store\")]")

    lines.append("```")
    return "\n".join(lines)


GROUP_NAMES = {
    "backend-controller": "Backend Controllers (REST Endpoints)",
    "backend-service": "Backend Services (Business Logic)",
    "backend-model": "Backend Entities & Models (Data Layer)",
    "backend-config": "Backend Configurations",
    "backend-other": "Backend Other Components",
    "quotation-controller": "Quotation Controllers (REST Endpoints)",
    "quotation-service": "Quotation Services (Lookup Logic)",
    "quotation-model": "Quotation Entities & Models (Data Layer)",
    "quotation-config": "Quotation Configurations",
    "quotation-other": "Quotation Other Components",
    "frontend-services": "Frontend Services (Angular API client)",
    "frontend-components": "Frontend Components (Angular UI)",
    "frontend-core": "Frontend App Core / Modules",
    "other": "Other System Components",
}


def group_classes(classes: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    groups = {}
    for c in classes:
        file_path = c.get("file", "")
        parts = file_path.replace("\\", "/").split("/")
        
        if file_path.startswith("backend/"):
            pkg = "other"
            for p in ["controller", "service", "model", "config"]:
                if p in parts:
                    pkg = p
                    break
            group_key = f"backend-{pkg}"
        elif file_path.startswith("Quotation/"):
            pkg = "other"
            for p in ["controller", "service", "model", "config"]:
                if p in parts:
                    pkg = p
                    break
            group_key = f"quotation-{pkg}"
        elif file_path.startswith("frontend/"):
            pkg = "core"
            if "components" in file_path or ".html" in file_path or ".scss" in file_path or ".spec.ts" in file_path:
                pkg = "components"
            elif "service" in file_path:
                pkg = "services"
            group_key = f"frontend-{pkg}"
        else:
            group_key = "other"
            
        groups.setdefault(group_key, []).append(c)
    return groups


def mermaid_class_diagram(model: Dict[str, Any], diagrams_dir: Path) -> str:
    classes = model.get("analysis", {}).get("classes", [])
    relationships = model.get("analysis", {}).get("relationships", [])

    if not classes:
        return "No classes detected."

    class_groups = group_classes(classes)
    class_dir = diagrams_dir / "classes"
    class_dir.mkdir(parents=True, exist_ok=True)

    index_lines = [
        "To maintain rendering performance and ensure diagram clarity, the class diagrams are grouped by project module and architectural layer:",
        "",
        "### Omnistore Backend API (Spring Boot)",
        ""
    ]

    def write_sub_diagram(group_key: str, class_list: List[Dict[str, Any]], group_name: str) -> None:
        group_class_names = {c.get("name") for c in class_list if c.get("name")}
        sub_lines = [
            f"# Class Diagram - {group_name}",
            "",
            "```mermaid",
            "classDiagram"
        ]

        for item in class_list[:40]:
            name = safe_mermaid_name(item.get("name", "Unknown"))
            sub_lines.append(f"    class {name}")

            fields = item.get("fields", [])
            methods = item.get("methods", [])

            if fields or methods:
                sub_lines.append(f"    class {name} {{")
                for field in fields[:10]:
                    field_name = field.get("name") if isinstance(field, dict) else str(field)
                    sub_lines.append(f"        +{safe_mermaid_name(field_name)}")
                for method in methods[:10]:
                    sub_lines.append(f"        +{safe_mermaid_name(method)}()")
                sub_lines.append("    }")

        for rel in relationships:
            src = rel.get("source")
            tgt = rel.get("target")
            if src in group_class_names or tgt in group_class_names:
                src_name = safe_mermaid_name(src or "Unknown")
                tgt_name = safe_mermaid_name(tgt or "Unknown")
                if rel.get("type") in {"inherits", "inherits_or_implements"}:
                    sub_lines.append(f"    {tgt_name} <|-- {src_name}")
                elif rel.get("type") in {"foreign_key", "prisma_relation"}:
                    sub_lines.append(f"    {src_name} --> {tgt_name}")

        sub_lines.append("```")
        file_content = "\n".join(sub_lines) + "\n"
        write_text(class_dir / f"{group_key}.md", file_content)

    backend_keys = ["backend-controller", "backend-service", "backend-model", "backend-config", "backend-other"]
    for key in backend_keys:
        if key in class_groups:
            name = GROUP_NAMES.get(key, key)
            write_sub_diagram(key, class_groups[key], name)
            index_lines.append(f"- [{name}](classes/{key}.md)")

    index_lines.extend(["", "### Quotation Microservice (Spring Boot)", ""])
    quotation_keys = ["quotation-controller", "quotation-service", "quotation-model", "quotation-config", "quotation-other"]
    for key in quotation_keys:
        if key in class_groups:
            name = GROUP_NAMES.get(key, key)
            write_sub_diagram(key, class_groups[key], name)
            index_lines.append(f"- [{name}](classes/{key}.md)")

    index_lines.extend(["", "### Frontend Angular Client", ""])
    frontend_keys = ["frontend-services", "frontend-components", "frontend-core"]
    for key in frontend_keys:
        if key in class_groups:
            name = GROUP_NAMES.get(key, key)
            write_sub_diagram(key, class_groups[key], name)
            index_lines.append(f"- [{name}](classes/{key}.md)")

    other_keys = ["other"]
    for key in other_keys:
        if key in class_groups:
            name = GROUP_NAMES.get(key, key)
            write_sub_diagram(key, class_groups[key], name)
            index_lines.extend(["", "### Other Components", ""])
            index_lines.append(f"- [{name}](classes/{key}.md)")

    return "\n".join(index_lines)


def mermaid_sequence_diagram(model: Dict[str, Any]) -> str:
    classes = model.get("analysis", {}).get("classes", [])
    files = {c.get("file", "") for c in classes}

    has_backend = any(f.startswith("backend/") for f in files)
    has_frontend = any(f.startswith("frontend/") for f in files)
    has_quotation = any(f.startswith("Quotation/") for f in files)

    if has_backend and has_frontend and has_quotation:
        return """```mermaid
sequenceDiagram
    actor User as User / Browser
    participant FE as Frontend Client (Angular)
    participant BE as Omnistore Backend (Spring Boot)
    participant QS as Quotation Microservice (Spring Boot)
    participant DB as Database / Persistence

    User->>FE: Interact (Add Item to Transaction / Request Quotation)
    FE->>BE: POST /api/transactions/items (Add Item)
    BE->>QS: GET /api/quotation (Fetch latest price quotation for barcode)
    QS->>DB: Query QuotationItem Entity
    DB-->>QS: Return item price details
    QS-->>BE: Return QuotationResponse (contains latest price)
    Note over BE: Price Sync Back: Compare Quotation price with Omnistore database price.<br/>Update TransactionItem price to match the Quotation price.
    BE->>DB: Persist updated TransactionItem and Transaction state
    DB-->>BE: Confirm Save
    BE-->>FE: Return Product details with the synced quotation price
    FE-->>User: Render transaction view in UI displaying the synced price
```"""
    else:
        routes = model.get("analysis", {}).get("routes", [])
        lines = ["```mermaid", "sequenceDiagram", "    actor User"]

        if routes:
            lines.extend([
                "    participant Controller",
                "    participant Service",
                "    participant Repository",
                "    participant Database",
                "    User->>Controller: Request",
                "    Controller->>Service: Validate and process",
                "    Service->>Repository: Load or persist data",
                "    Repository->>Database: Query",
                "    Database-->>Repository: Result",
                "    Repository-->>Service: Entity",
                "    Service-->>Controller: Response model",
                "    Controller-->>User: Response",
            ])
        else:
            lines.extend([
                "    participant Application",
                "    participant Component",
                "    User->>Application: Trigger workflow",
                "    Application->>Component: Execute operation",
                "    Component-->>Application: Result",
                "    Application-->>User: Response",
            ])

        lines.append("```")
        return "\n".join(lines)


def mermaid_er_diagram(model: Dict[str, Any]) -> str:
    entities = model.get("analysis", {}).get("entities", [])
    relationships = model.get("analysis", {}).get("relationships", [])

    lines = ["```mermaid", "erDiagram"]

    if not entities:
        lines.append("    NO_ENTITIES_DETECTED {")
        lines.append("        string note")
        lines.append("    }")
        lines.append("```")
        return "\n".join(lines)

    for rel in relationships:
        if rel.get("type") in {"foreign_key", "prisma_relation"}:
            source = safe_mermaid_name(rel.get("source", "Unknown"))
            target = safe_mermaid_name(rel.get("target", "Unknown"))
            lines.append(f"    {target} ||--o{{ {source} : relates_to")

    for entity in entities[:120]:
        name = safe_mermaid_name(entity.get("name", "Entity"))
        lines.append(f"    {name} {{")
        fields = entity.get("fields", [])
        if not fields:
            lines.append("        string id")
        else:
            for field in fields[:30]:
                if isinstance(field, dict):
                    field_name = safe_mermaid_name(field.get("name", "field"))
                    field_type = safe_mermaid_name(field.get("type", "string"))
                else:
                    field_name = safe_mermaid_name(str(field))
                    field_type = "string"
                lines.append(f"        {field_type} {field_name}")
        lines.append("    }")

    lines.append("```")
    return "\n".join(lines)


def format_dict(data: Dict[str, Any]) -> str:
    if not data:
        return "- Not detected"
    return "\n".join(f"- {key}: {value}" for key, value in data.items())


def format_build_files(items: List[Dict[str, Any]]) -> str:
    if not items:
        return "- Not detected"
    return "\n".join(f"- `{item.get('file')}`: `{item.get('type')}`" for item in items)


def format_modules(items: List[Dict[str, Any]]) -> str:
    if not items:
        return "- No modules detected"
    return "\n".join(f"- `{i.get('file')}` language=`{i.get('language')}` layer=`{i.get('layer')}`" for i in items)


def format_routes(items: List[Dict[str, Any]]) -> str:
    if not items:
        return "- No routes detected"
    return "\n".join(f"- `{i.get('method')} {i.get('path')}` in `{i.get('file')}`" for i in items)


def format_entities(items: List[Dict[str, Any]]) -> str:
    if not items:
        return "- No entities detected"
    return "\n".join(f"- `{i.get('name')}` from `{i.get('file')}`" for i in items)


def format_list(items: List[str]) -> str:
    if not items:
        return "- None detected"
    return "\n".join(f"- `{x}`" for x in items)


def format_large_classes(items: List[Dict[str, Any]]) -> str:
    if not items:
        return "- None detected"

    return "\n".join(
        f"- `{i.get('name')}` in `{i.get('file')}` methods={len(i.get('methods', []))} fields={len(i.get('fields', []))}"
        for i in items
    )


if __name__ == "__main__":
    main()