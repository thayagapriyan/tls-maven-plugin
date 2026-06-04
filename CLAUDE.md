# CLAUDE.md

Guidance for Claude Code (and other AI agents) when working in this repository.

> **Start with [AGENTS.md](AGENTS.md).** It is the documentation index for this repo and links
> the design docs (`idea.md`, `docs/architecture.md`, `docs/HLD.md`, `docs/LLD.md`,
> `docs/develop.md`, `docs/testing.md`, `docs/deploy.md`). **You must follow the
> doc-maintenance rule in AGENTS.md**: when you change code, config, workflows, or infra, update
> the affected docs and append a [CHANGELOG.md](CHANGELOG.md) entry in the same change.

## What this project is

A custom **Apache Maven plugin** that downloads TLS keystore/truststore files from an **AWS S3
bucket** at build time and writes them into the Maven build output directory
(`target/classes`). It exists so Mule 4 applications never commit `.jks`/`.p12`/`.pem` files to
Git. The full design lives in [idea.md](idea.md) — read it before making structural decisions.

> Note: an earlier iteration used AWS Secrets Manager; the project deliberately pivoted to S3
> (objects download as raw bytes — no Base64/binary handling needed). Don't reintroduce Secrets
> Manager.

> **Status:** Implemented. The plugin builds and unit tests pass (`mvn clean test`).
> `maven-invoker-plugin` integration tests are wired up in `src/it/` (`skip-flag`,
> `local-fake-s3` run with no AWS; `aws-download` runs against real S3 when `-Daws.it.bucket`
> is set). A LocalStack-backed download IT and a dummy Mule app are still TODO.

## Project structure

```
tls-maven-plugin/
├── pom.xml                                  # packaging = maven-plugin
├── src/
│   ├── main/java/com/priyan/maven/
│   │   ├── FetchTlsContextMojo.java          # @Mojo(name="fetch-tls-context")
│   │   ├── config/S3FileConfig.java           # <file> element binding (bucket/key/target)
│   │   └── aws/
│   │       ├── S3ObjectResolver.java          # interface (test seam)
│   │       ├── AwsS3ObjectResolver.java        # wraps S3Client (SDK v2)
│   │       └── S3FetchException.java
│   └── test/java/com/priyan/maven/
│       ├── FetchTlsContextMojoTest.java       # Mojo behavior (fake resolver)
│       └── aws/AwsS3ObjectResolverTest.java    # download/exception mapping (mocked client)
└── src/it/                                   # maven-invoker integration tests (skip-flag, local-fake-s3, aws-download)
```

## Key technical conventions

- **Java + AWS SDK v2.** Use `software.amazon.awssdk:s3` (v2), not the v1 `aws-java-sdk`.
- **Credentials are never stored by the plugin.** Resolve from the environment:
  `DefaultCredentialsProvider`, or `ProfileCredentialsProvider` when `awsProfile` is set. Never
  add `accessKey`/`secretKey` config parameters — that would leak keys into `pom.xml`.
- **Mojo goal:** `fetch-tls-context`, default phase `GENERATE_RESOURCES`.
- **S3 objects are raw bytes.** Stream them straight to disk — no Base64/binary distinction.
- **Never log file material.** Log only the S3 location, file path, and byte count. Hard rule.
- **Write under `${project.build.outputDirectory}`** (overridable via `outputDirectory`), create
  parent directories as needed, and reject `targetFileName` values that escape the output dir.

## Build & test commands

```powershell
mvn clean install          # build + run unit tests + install locally
mvn test                   # unit tests only
mvn verify                 # includes integration tests (src/it) if configured
mvn -Dtls.skip=true ...    # the plugin supports a skip flag
```

This is a Windows environment with PowerShell — use PowerShell syntax in examples
(`$env:VAR`, not `$VAR`).

## Working agreements for agents

- **Read [idea.md](idea.md) first.** It is the source of truth for design intent and the
  configuration contract (see its Configuration Reference table).
- **Do not commit certificate files** (`*.jks`, `*.p12`, `*.pem`, `*.keystore`, `*.truststore`).
  Ensure `.gitignore` covers them and `target/`.
- **Do not hardcode AWS account ids, bucket names with secrets, or credentials** in source or
  tests — use mocks.
- **Keep file material out of logs and test output.**
- Match the surrounding code style; prefer small, focused commits. Only commit or push when the
  user explicitly asks.
