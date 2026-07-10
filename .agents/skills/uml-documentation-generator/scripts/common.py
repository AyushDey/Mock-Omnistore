#!/usr/bin/env python3

from __future__ import annotations

import hashlib
import json
import os
import re
from pathlib import Path
from typing import Any, Dict, Iterable, List


IGNORED_DIRS = {
    ".git",
    ".hg",
    ".svn",
    "node_modules",
    "dist",
    "build",
    "target",
    "__pycache__",
    ".venv",
    "venv",
    "env",
    ".idea",
    ".vscode",
    "coverage",
    ".next",
    ".nuxt",
    "bin",
    "obj",
    ".terraform",
    ".gradle",
    ".mypy_cache",
    ".pytest_cache",
    ".angular",
    ".agents",
    "docs",
}

ANALYZED_EXTENSIONS = {
    ".py",
    ".java",
    ".cs",
    ".js",
    ".jsx",
    ".ts",
    ".tsx",
    ".sql",
    ".prisma",
    ".json",
    ".xml",
    ".gradle",
    ".toml",
    ".yml",
    ".yaml",
}

BUILD_FILE_NAMES = {
    "package.json",
    "pom.xml",
    "build.gradle",
    "settings.gradle",
    "pyproject.toml",
    "requirements.txt",
    "setup.py",
    "Pipfile",
    "go.mod",
    "Cargo.toml",
    "Dockerfile",
    "docker-compose.yml",
    "docker-compose.yaml",
}


def is_ignored(path: Path) -> bool:
    return (
        any(part in IGNORED_DIRS for part in path.parts)
        or path.name in {"package-lock.json", "yarn.lock", "pnpm-lock.yaml", "bun.lockb"}
    )


def is_relevant_file(path: Path) -> bool:
    return path.suffix in ANALYZED_EXTENSIONS or path.name in BUILD_FILE_NAMES


def collect_relevant_files(root: Path) -> List[Path]:
    files: List[Path] = []

    for current_root, dirs, filenames in os.walk(root):
        current_path = Path(current_root)

        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS]

        try:
            relative_current = current_path.relative_to(root)
        except ValueError:
            continue

        if is_ignored(relative_current):
            continue

        for filename in filenames:
            file_path = current_path / filename

            try:
                relative_file = file_path.relative_to(root)
            except ValueError:
                continue

            if is_ignored(relative_file):
                continue

            if is_relevant_file(file_path):
                files.append(file_path)

    return sorted(files)


def sha256_file(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def read_json(path: Path, default: Dict[str, Any] | None = None) -> Dict[str, Any]:
    if not path.exists():
        return default or {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default or {}


def write_json(path: Path, data: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, sort_keys=True), encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def relpath(path: Path, root: Path) -> str:
    return str(path.relative_to(root)).replace("\\", "/")


def safe_mermaid_name(name: Any) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9_]", "_", str(name))
    if not cleaned:
        return "Unknown"
    if cleaned[0].isdigit():
        cleaned = "_" + cleaned
    return cleaned


def guess_layer(path: str) -> str:
    lower = path.lower()

    if any(x in lower for x in ["controller", "controllers", "route", "routes", "handler", "handlers"]):
        return "api"
    if any(x in lower for x in ["service", "services", "usecase", "usecases"]):
        return "service"
    if any(x in lower for x in ["repository", "repositories", "dao", "persistence", "infra"]):
        return "persistence"
    if any(x in lower for x in ["model", "models", "entity", "entities", "domain"]):
        return "domain"
    if any(x in lower for x in ["component", "components", "page", "pages", "view", "views"]):
        return "ui"
    if any(x in lower for x in ["config", "configuration", "settings"]):
        return "configuration"
    if any(x in lower for x in ["app", "application", "main", "entrypoint"]):
        return "application"

    return "unknown"


def classify_class(name: str, file_path: str, annotations: List[str] | None = None) -> str:
    annotations = annotations or []
    lower_name = name.lower()
    lower_file = file_path.lower()
    joined = " ".join(annotations).lower()

    if "controller" in lower_name or "controller" in lower_file or "restcontroller" in joined:
        return "controller"
    if "service" in lower_name or "service" in lower_file:
        return "service"
    if "repository" in lower_name or "repository" in lower_file:
        return "repository"
    if "entity" in lower_file or "model" in lower_file or "entity" in joined or "table" in joined:
        return "entity"
    if lower_name.endswith("dto"):
        return "dto"
    if "component" in lower_file or lower_name.endswith("component"):
        return "ui_component"

    return "class"


def unique_dicts(items: Iterable[Dict[str, Any]], keys: List[str]) -> List[Dict[str, Any]]:
    seen = set()
    output = []

    for item in items:
        key = tuple(item.get(k) for k in keys)
        if key not in seen:
            seen.add(key)
            output.append(item)

    return output