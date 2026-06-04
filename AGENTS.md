# AGENTS.md — aws-tls-injector-maven-plugin

Entry point for AI agents and contributors. Read this first, then the linked docs.

## What this project is

A custom Apache Maven plugin that downloads TLS keystore/truststore files from AWS S3 at build
time and writes them into `target/classes`, so Mule 4 apps never commit certs to Git. It is
published to **Anypoint Exchange** and consumed by the sibling `tls-mule-maven-project`.

## Documentation map

| Doc | Read it for |
|-----|-------------|
| [idea.md](idea.md) | Design intent & configuration contract (**source of truth**) |
| [docs/architecture.md](docs/architecture.md) | Components, data flow, tech stack, security model |
| [docs/HLD.md](docs/HLD.md) | High-level design: context, responsibilities, lifecycle |
| [docs/LLD.md](docs/LLD.md) | Classes, parameters, methods, error handling |
| [docs/develop.md](docs/develop.md) | Build, conventions, how to extend |
| [docs/testing.md](docs/testing.md) | Unit, invoker ITs, LocalStack e2e (local + CI) |
| [docs/deploy.md](docs/deploy.md) | Publishing to Anypoint Exchange |
| [CHANGELOG.md](CHANGELOG.md) | Chronological history of changes |
| [CLAUDE.md](CLAUDE.md) | Repo-specific agent instructions |

## Working agreements (must follow)

- **Read [idea.md](idea.md) first** for any structural decision.
- AWS SDK **v2** only; never store credentials; never log file material; keep the path-traversal guard.
- Do not commit cert files; match surrounding style; commit only when asked.

## 📌 Documentation-maintenance rule (REQUIRED)

When you change code, config, workflows, or infra, you **must** update the docs in the same change:

1. Update the affected doc(s) above so they stay accurate (e.g. a new `@Parameter` → `idea.md` +
   `docs/LLD.md`; a workflow change → `docs/testing.md`/`docs/deploy.md`).
2. Append an entry to [CHANGELOG.md](CHANGELOG.md) under the current date describing the change.
3. If you add a new doc, link it from this map and from the relevant sibling docs.

Treat docs as part of the definition of done — a code change without its doc update is incomplete.

## Sibling project

[`tls-mule-maven-project`](../tls-mule-maven-project/AGENTS.md) — the Mule app that consumes this plugin.
