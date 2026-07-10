#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Dict, List


START_MARKER = "<!-- ARCHITECTURE-DOCS:START -->"
END_MARKER = "<!-- ARCHITECTURE-DOCS:END -->"


def read_json(path: Path) -> Dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def build_section(model: Dict[str, Any], docs_root: str) -> str:
    summary = model.get("summary", {})
    delta = model.get("delta", {}).get("summary", {})
    tech = model.get("technology", {})
    languages = tech.get("languages", {})

    return f"""{START_MARKER}
## 🗺️ Architecture & Developer Navigation

This repository is organized as a multi-service application containing an **Angular frontend client**, a **Spring Boot Backend API**, and a **Spring Boot Quotation Microservice**. 

### 🚀 Where to Start?
* **First Time Here?** Read the [Developer Onboarding Guide]({docs_root}/onboarding.md) to set up the project and run tests.
* **Want to understand the code structure?** Review the [System Overview]({docs_root}/system-overview.md) to see how modules, routes, and entities map.
* **Architectural Review?** Check the [Architecture Review]({docs_root}/architecture-review.md) for code quality indicators and coupling analyses.

---

### 📂 Repository Documentation Index

| Component / Document | Description | Key Reference Diagrams |
| :--- | :--- | :--- |
| 📄 [Architecture Overview]({docs_root}/architecture.md) | High-level system goals, technologies & stacks | 🗺️ [UML Component Diagram]({docs_root}/diagrams/component-diagram.md) |
| 📄 [System Overview]({docs_root}/system-overview.md) | Discovered codebase modules, API endpoints & schemas | 🔄 [UML Sequence Diagram]({docs_root}/diagrams/sequence-diagrams.md) |
| 📄 [Components & Classes]({docs_root}/components.md) | Full catalog of identified classes and methods | 🏷️ [UML Class Diagram Index]({docs_root}/diagrams/class-diagram.md) |
| 📄 [API Reference]({docs_root}/api-reference.md) | Dynamic list of REST endpoint routing details | 🗄️ [UML ER Diagram]({docs_root}/diagrams/er-diagram.md) |
| 📄 [Onboarding Guide]({docs_root}/onboarding.md) | Local environment startup and testing commands | |
| 📄 [Architecture Review]({docs_root}/architecture-review.md) | Large classes list, coupling warnings & recommendations | |

---

### 🗺️ High-Level Component Flow

{build_component_mermaid(model)}

<details>
<summary><b>🔍 System Metrics & Stats</b></summary>

* **Discovered Modules:** {summary.get("module_count", 0)} (Java, TypeScript, Python)
* **Discovered Classes:** {summary.get("class_count", 0)}
* **API Endpoints:** {summary.get("route_count", 0)}
* **Database Entities:** {summary.get("entity_count", 0)}
* **Relationships / Couplings:** {summary.get("relationship_count", 0)}
* **Codebase Language Mix:** {", ".join(f"{lang} ({count} file(s))" for lang, count in languages.items())}

_Last incremental analysis scan on {delta.get("added", 0) + delta.get("modified", 0)} changed files._
</details>

> This section is auto-managed. Do not edit between the architecture documentation markers manually.
{END_MARKER}
"""


def format_languages(languages: Dict[str, Any]) -> str:
    if not languages:
        return "- Not detected"
    return "\n".join(f"- {language}: {count} file(s)" for language, count in languages.items())


def build_component_mermaid(model: Dict[str, Any]) -> str:
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


def update_readme(readme_path: Path, section: str) -> None:
    if readme_path.exists():
        existing = readme_path.read_text(encoding="utf-8")
    else:
        existing = "# Project\n"

    if START_MARKER in existing and END_MARKER in existing:
        before = existing.split(START_MARKER)[0].rstrip()
        after = existing.split(END_MARKER, 1)[1].lstrip()
        updated = before + "\n\n" + section.strip() + "\n\n" + after
    else:
        updated = existing.rstrip() + "\n\n" + section.strip() + "\n"

    readme_path.write_text(updated, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--readme", required=True)
    parser.add_argument("--docs-root", default="docs/generated")
    args = parser.parse_args()

    model = read_json(Path(args.model))
    section = build_section(model, args.docs_root)
    update_readme(Path(args.readme), section)

    print(f"Updated README architecture section at {args.readme}")


if __name__ == "__main__":
    main()