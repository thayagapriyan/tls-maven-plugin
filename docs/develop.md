# Development Guide — aws-tls-injector-maven-plugin

## Prerequisites

- JDK 11+ (the plugin targets `maven.compiler.release=11`)
- Maven 3.9+
- (For LocalStack e2e) Docker + AWS CLI

## Project layout

```
tls-maven-plugin/
├── pom.xml                     packaging=maven-plugin; exchange-release profile
├── idea.md                     design source of truth
├── docs/                       architecture, HLD, LLD, develop, testing, deploy
├── AGENTS.md  CHANGELOG.md  CLAUDE.md
├── src/main/java/com/priyan/maven/   Mojo + config + aws
├── src/test/java/...                 unit tests
├── src/it/                           invoker ITs (skip-flag, local-fake-s3, aws-download)
└── .github/workflows/                ci.yml, publish-exchange.yml
```

## Build & test

```powershell
mvn clean test                       # unit tests only
mvn clean install                    # + invoker ITs (skip-flag, local-fake-s3) + install to ~/.m2
mvn clean install -Dinvoker.skip=true  # fast: skip ITs, just install the plugin
```

> `aws-download` IT is excluded by default; it runs only under the `aws-it` profile
> (activated by `-Daws.it.bucket=...`). See [testing.md](testing.md).

## Conventions

- **AWS SDK v2 only** (`software.amazon.awssdk`), never v1.
- **Never** add `accessKey`/`secretKey` parameters — identity is environment-resolved.
- **Never log object bytes** — only S3 location, path, byte count.
- Write under `${project.build.outputDirectory}` and keep the path-traversal guard intact.
- Match surrounding style; small, focused commits; commit only when asked.

## Adding a configuration parameter

1. Add a `@Parameter` field to `FetchTlsContextMojo` (and a package-private setter if tests need it).
2. Thread it into `AwsS3ObjectResolver.fromConfig` / `fetch` as needed.
3. Document it in [idea.md §5.1](../idea.md) and [LLD.md](LLD.md).
4. Add/extend a unit test and, if it affects download, an IT.
5. Update [CHANGELOG.md](../CHANGELOG.md).

## Adding a file binding field

Edit `config/S3FileConfig` (field + getters/setters + `toString` without secrets), then surface
it in `fetch()`.

## Related docs

[architecture.md](architecture.md) · [LLD.md](LLD.md) · [testing.md](testing.md) · [deploy.md](deploy.md)
