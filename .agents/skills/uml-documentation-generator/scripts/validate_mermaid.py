#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import List


VALID_STARTERS = {
    "flowchart",
    "graph",
    "classDiagram",
    "sequenceDiagram",
    "erDiagram",
    "stateDiagram",
    "journey",
    "gantt",
    "pie",
}


def validate_file(path: Path) -> List[str]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    errors: List[str] = []

    blocks = re.findall(r"```mermaid\n(.*?)```", text, flags=re.DOTALL)

    for index, block in enumerate(blocks, start=1):
        stripped = block.strip()

        if not stripped:
            errors.append(f"{path}: Mermaid block {index} is empty")
            continue

        first_line = stripped.splitlines()[0].strip()
        first_word = first_line.split()[0] if first_line.split() else ""

        if first_word not in VALID_STARTERS:
            errors.append(f"{path}: Mermaid block {index} starts with unexpected declaration `{first_line}`")

    return errors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--docs", required=True)
    args = parser.parse_args()

    docs = Path(args.docs)
    errors: List[str] = []

    for path in docs.rglob("*.md"):
        errors.extend(validate_file(path))

    if errors:
        print("Mermaid validation warnings:")
        for error in errors:
            print(f"- {error}")
    else:
        print("Mermaid validation passed.")


if __name__ == "__main__":
    main()