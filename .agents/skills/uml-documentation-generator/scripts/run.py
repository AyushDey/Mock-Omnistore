#!/usr/bin/env python3

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


def run_command(command: list[str]) -> None:
    print("Running:", " ".join(command))
    result = subprocess.run(command, text=True)
    if result.returncode != 0:
        raise SystemExit(result.returncode)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".", help="Repository root")
    parser.add_argument("--force", action="store_true", help="Force full re-analysis")
    parser.add_argument("--skip-readme", action="store_true", help="Do not update README.md")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    script_dir = Path(__file__).resolve().parent

    state_dir = root / "docs" / ".architecture"
    output_dir = root / "docs" / "generated"

    model_path = state_dir / "architecture_model.json"
    cache_path = state_dir / "architecture_cache.json"
    delta_path = state_dir / "architecture_delta.json"
    readme_path = root / "README.md"

    analyze_cmd = [
        sys.executable,
        str(script_dir / "analyze_repo.py"),
        "--root",
        str(root),
        "--model",
        str(model_path),
        "--cache",
        str(cache_path),
        "--delta",
        str(delta_path),
    ]

    if args.force:
        analyze_cmd.append("--force")

    generate_cmd = [
        sys.executable,
        str(script_dir / "generate_docs.py"),
        "--model",
        str(model_path),
        "--output",
        str(output_dir),
    ]

    validate_cmd = [
        sys.executable,
        str(script_dir / "validate_mermaid.py"),
        "--docs",
        str(output_dir),
    ]

    readme_cmd = [
        sys.executable,
        str(script_dir / "update_readme.py"),
        "--model",
        str(model_path),
        "--readme",
        str(readme_path),
        "--docs-root",
        "docs/generated",
    ]

    run_command(analyze_cmd)
    run_command(generate_cmd)
    run_command(validate_cmd)

    if not args.skip_readme:
        run_command(readme_cmd)

    print("")
    print("Architecture documentation generated successfully.")
    print(f"Documentation: {output_dir}")
    print(f"Architecture model: {model_path}")
    print(f"Delta report: {delta_path}")
    print(f"README update: {'skipped' if args.skip_readme else readme_path}")


if __name__ == "__main__":
    main()