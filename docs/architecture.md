# Architecture — aws-tls-injector-maven-plugin

## Purpose

A custom Apache Maven plugin that downloads TLS keystore/truststore objects from AWS S3 at
build time and writes them into the Maven build output directory (`target/classes`), so Mule 4
apps never commit `.jks`/`.p12`/`.pem` files to Git.

## Component overview

```
┌────────────────────────────────────────────────────────────┐
│ FetchTlsContextMojo   (@Mojo "fetch-tls-context")           │
│  • binds to generate-resources                              │
│  • validates config, path-traversal guard, write to disk    │
└───────────────┬───────────────────────────┬────────────────┘
                │ uses                       │ binds
                ▼                            ▼
┌──────────────────────────┐     ┌──────────────────────────┐
│ S3ObjectResolver (iface) │     │ S3FileConfig             │
│  fetch(config) -> bytes  │     │  bucket/key/targetFile/  │
│  close()                 │     │  versionId               │
└───────────┬──────────────┘     └──────────────────────────┘
            │ impl
            ▼
┌──────────────────────────┐         ┌──────────────────┐
│ AwsS3ObjectResolver      │ ──────▶ │ AWS S3 (SDK v2)  │
│  wraps S3Client          │ GetObj  │  raw object bytes │
│  maps SDK exceptions     │         └──────────────────┘
└──────────────────────────┘
            │ on failure
            ▼
   S3FetchException (actionable, never logs bytes)
```

## Tech stack

| Concern        | Choice                                            |
| -------------- | ------------------------------------------------- |
| Language       | Java 11                                            |
| Packaging      | `maven-plugin` (maven-plugin-tools 3.13.1)        |
| AWS access     | AWS SDK **v2** (`software.amazon.awssdk:s3` 2.28.16) |
| Credentials    | AWS default chain / named profile — never stored  |
| Tests          | JUnit 5, Mockito, `maven-invoker-plugin` ITs      |
| Distribution   | Anypoint Exchange (Maven endpoint)                |

## Data flow (build time)

1. `mvn ... package` enters the `generate-resources` phase.
2. The Mojo builds an `AwsS3ObjectResolver` from `region` + `awsProfile` (or a test-injected resolver).
3. For each `<file>`: `GetObject` → raw bytes → write to `outputDirectory/targetFileName`.
4. The Mule Maven plugin (in the consuming app) packages those bytes into the artifact.

## Security model

- The plugin **stores no credentials**. Identity comes from the environment (profile, IAM role,
  GitHub OIDC, env vars). The only credential-related knob is `awsProfile` (a non-secret label).
- **No file material is ever logged** — only the S3 location, target path, and byte count.
- A **path-traversal guard** rejects any `targetFileName` resolving outside `outputDirectory`.
- Downloaded files live only in the ephemeral `target/` directory (wiped by `mvn clean`).

## Related docs

- [idea.md](../idea.md) — design intent & configuration contract (source of truth)
- [HLD.md](HLD.md) · [LLD.md](LLD.md) · [develop.md](develop.md) · [testing.md](testing.md) · [deploy.md](deploy.md)
