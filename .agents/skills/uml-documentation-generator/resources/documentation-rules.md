# Documentation Rules

## Always Do

- Use Markdown.
- Use Mermaid for diagrams.
- Reference source files where possible.
- Clearly separate discovered facts from assumptions.
- Keep generated files under `docs/generated/`.
- Keep cache and delta files under `docs/.architecture/`.
- Include evidence for important relationships.
- Update README.md using only the managed architecture documentation block.

## README Rules

The README architecture section must be wrapped with:

```markdown
<!-- ARCHITECTURE-DOCS:START -->
<!-- ARCHITECTURE-DOCS:END -->
```

Only content between those markers may be replaced.

If the markers do not exist, append the generated section near the end of README.md.

The README section should include:

- Incremental update summary
- Repository architecture summary
- Detected technologies
- Compact Mermaid component diagram
- Links to full generated documentation

## Avoid

- Do not document vendor libraries.
- Do not document generated files.
- Do not include build output.
- Do not invent relationships.
- Do not invent APIs.
- Do not invent database tables.
- Do not claim runtime behavior without evidence.
- Do not rewrite human-authored README content outside the managed markers.

## Labels

Use these labels:

- Discovered
- Inferred
- Not found
- Needs confirmation