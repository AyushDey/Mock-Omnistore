---
name: uml-documentation-generator
description: Incrementally analyzes software repositories and automatically generates evidence-based architecture documentation, Mermaid UML diagrams, README architecture summaries, component diagrams, class diagrams, sequence diagrams, ER diagrams, API documentation, onboarding guides, dependency analysis, and architecture review reports. Use when users ask to generate UML, document architecture, reverse engineer a codebase, update README documentation, or refresh architecture docs after code changes.
---

# UML Documentation Generator

## Purpose

Use this skill to incrementally analyze a software repository and generate maintainable, evidence-based architecture documentation.

This skill works with Google Antigravity and similar coding agents that can inspect files, edit files, run commands, and update repository documentation.

The user should only need to ask for architecture documentation, UML diagrams, or README documentation updates. The agent should perform the workflow automatically.

## When To Use This Skill

Use this skill when the user asks to:

- Generate UML diagrams
- Generate Mermaid diagrams
- Create architecture documentation
- Reverse engineer a codebase
- Understand repository structure
- Document a codebase
- Create developer onboarding documentation
- Analyze dependencies
- Review architecture quality
- Generate class diagrams
- Generate component diagrams
- Generate sequence diagrams
- Generate ER diagrams
- Document APIs
- Update architecture documentation after code changes
- Incrementally refresh documentation
- Add generated architecture docs to README
- Show architecture diagrams in README
- Create GitHub-friendly architecture documentation

## Required Behavior

When this skill is selected, the agent must run this automatically from the repository root:

```bash
python .agents/skills/uml-documentation-generator/scripts/run.py --root .
```

The agent must not ask the user to manually run commands unless execution is unavailable.

After running, summarize:

- Added files
- Modified files
- Removed files
- Unchanged files reused from cache
- Location of generated documentation
- Whether README.md was updated

## Output Locations

Generated documentation must be written to:

```text
docs/generated/
```

Internal architecture state must be written to:

```text
docs/.architecture/
```

README architecture summary must be written to:

```text
README.md
```

Expected generated structure:

```text
docs/
├── generated/
│   ├── architecture.md
│   ├── system-overview.md
│   ├── components.md
│   ├── api-reference.md
│   ├── onboarding.md
│   ├── architecture-review.md
│   └── diagrams/
│       ├── component-diagram.md
│       ├── class-diagram.md
│       ├── sequence-diagrams.md
│       └── er-diagram.md
└── .architecture/
    ├── architecture_model.json
    ├── architecture_cache.json
    └── architecture_delta.json
```

## README Update Rules

The README must be updated using a managed block only.

Use these exact markers:

```markdown
<!-- ARCHITECTURE-DOCS:START -->
<!-- ARCHITECTURE-DOCS:END -->
```

Rules:

- If the markers exist, replace only the content between them.
- If the markers do not exist, append the managed section near the end of `README.md`.
- Never rewrite the full README.
- Never delete human-written README content.
- Never modify README content outside the managed markers.
- Use GitHub-compatible Markdown.
- Use Mermaid fenced code blocks for diagrams.
- Link to the detailed generated docs under `docs/generated/`.

## Incremental Analysis Rules

The analyzer must:

- Hash each relevant source file.
- Re-analyze only added or modified files.
- Reuse cached analysis for unchanged files.
- Remove deleted files from the architecture model.
- Generate a delta report.
- Regenerate documentation from the updated architecture model.
- Update only the managed architecture section in README.md.

Use full re-analysis only when:

- No cache exists.
- The cache is invalid or corrupt.
- The repository structure changed significantly.
- The user explicitly asks for a full refresh.
- Incremental analysis is not reliable for the repository.

If true incremental analysis is not possible, clearly state that a full refresh was performed.

## Repository Analysis Priority

Analyze the repository in this order:

1. Build files
2. Dependency files
3. Application entry points
4. Controllers, routes, handlers, or API endpoints
5. Services
6. Domain models
7. Persistence layer
8. Database schema, migrations, or ORM files
9. Configuration files
10. Deployment files
11. Tests

## Technology Detection

Detect and analyze these technologies when present.

### Python

Look for:

- `*.py`
- `pyproject.toml`
- `requirements.txt`
- `setup.py`
- `Pipfile`

### Node.js / JavaScript / TypeScript

Look for:

- `package.json`
- `tsconfig.json`
- `*.js`
- `*.jsx`
- `*.ts`
- `*.tsx`

### Java

Look for:

- `*.java`
- `pom.xml`
- `build.gradle`
- `settings.gradle`

### .NET / C#

Look for:

- `*.cs`
- `*.csproj`
- `*.sln`

### Database

Look for:

- `*.sql`
- migration folders
- Prisma schemas
- ORM models
- entity annotations

### Deployment

Look for:

- `Dockerfile`
- `docker-compose.yml`
- `docker-compose.yaml`
- Kubernetes manifests
- Helm charts
- GitHub Actions workflows
- CI/CD configuration

## Evidence Rules

Do not invent classes.

Do not invent relationships.

Do not invent APIs.

Do not invent services.

Do not invent database tables.

Do not invent flows that are not supported by source evidence.

Every important claim should include source-file evidence where possible.

When uncertain, mark the item as one of:

```text
Inferred
Needs confirmation
Not found
```

## Documentation Quality Rules

Generated documentation should be:

- Accurate
- Evidence-based
- GitHub-friendly
- Easy for new developers to understand
- Easy to maintain
- Concise but useful
- Structured with clear headings
- Written in Markdown
- Safe to regenerate

Avoid:

- Vendor code
- Generated files
- Build output
- `node_modules`
- `.git`
- `.venv`
- `target`
- `dist`
- `build`
- `coverage`

## Diagram Rules

Use Mermaid by default.

Do not generate PNG, SVG, PDF, DOCX, or PPTX unless the user explicitly requests exported files.

Every diagram must include:

- Purpose
- Mermaid source
- Key observations
- Assumptions or uncertainty, if any

### Readability & Detail Rules:
- **No Monolithic Diagrams:** Large codebases must split class diagrams logically (e.g. by subproject, directory boundary, or domain boundary) rather than generating a single cluttered diagram.
- **Accurate Flow Syncing:** Sequence and communication flow diagrams must not use generic placeholders. They must depict actual system operations, capturing detailed interactions such as price syncing, client-server data synchronization, database updates, state changes, and feedback back to the UI.
- **Legibility for New Developers:** Code blocks, label names, and connection descriptions should use plain English descriptions along with route or API details to allow any developer new to the project to immediately grasp the software architecture and interaction flows.

## Required Generated Documents

### `docs/generated/architecture.md`

Must include:

- Purpose
- Scope
- Incremental analysis summary
- Repository summary
- Detected technology stack
- High-level component diagram
- Assumptions
- Risks
- Recommendations

### `docs/generated/system-overview.md`

Must include:

- Main modules
- Source layout
- Entry points
- API routes
- Entities
- External integrations where found

### `docs/generated/components.md`

Must include:

- Components/classes/services discovered
- Kind of component
- Layer
- Source file evidence
- Important relationships


### `docs/generated/api-reference.md`

Must include:

- Detected endpoints
- Method
- Path
- Handler
- Source file
- Request/response marked as `Needs confirmation` unless explicitly discovered

### `docs/generated/onboarding.md`

Must include:

- Project overview
- Technology stack
- Repository structure
- Build files
- How to find local setup
- How to find tests
- Links to generated docs

### `docs/generated/architecture-review.md`

Must include:

- Strengths
- Risks
- Potential large classes/services
- Coupling concerns
- Missing boundaries
- Recommendations

## Final Response Requirements

After completing the workflow, summarize:

```markdown
## Architecture Documentation Updated

- Added files:
- Modified files:
- Removed files:
- Unchanged files reused from cache:

## Generated Files

- docs/generated/architecture.md
- docs/generated/system-overview.md
- docs/generated/components.md
- docs/generated/api-reference.md
- docs/generated/onboarding.md
- docs/generated/architecture-review.md
- docs/generated/diagrams/component-diagram.md
- docs/generated/diagrams/class-diagram.md
- docs/generated/diagrams/sequence-diagrams.md
- docs/generated/diagrams/er-diagram.md

## README

- README.md updated: yes/no

## Notes

- Mention important assumptions.
- Mention if full refresh was used instead of incremental refresh.
```

## Safety And Accuracy Rules

The agent must not:

- Invent undocumented runtime behavior.
- Invent APIs.
- Invent database tables.
- Invent authentication flows.
- Delete user-authored documentation.
- Overwrite README content outside managed markers.
- Claim full production accuracy without review.

The agent should:

- Prefer source evidence.
- Mark uncertainty clearly.
- Keep generated documentation easy to review.
- Keep generated files separated from human-authored docs.
- Recommend human review for production architecture decisions.