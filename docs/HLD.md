# High-Level Design — aws-tls-injector-maven-plugin

## Context

```
        publishes                          resolves & runs
Developer ───────▶ Anypoint Exchange ◀───────────────── Mule app build
   │                                                          │
   │ tag v*                                                   │ generate-resources
   ▼                                                          ▼
GitHub Actions (plugin repo)                       custom plugin downloads
  unit + invoker + LocalStack e2e                  keystore from AWS S3
```

## Responsibilities

| # | Responsibility | Where |
|---|----------------|-------|
| 1 | Resolve AWS identity from the environment (never store keys) | `AwsS3ObjectResolver.fromConfig` |
| 2 | Download each configured S3 object as raw bytes | `AwsS3ObjectResolver.fetch` |
| 3 | Write bytes safely under the build output dir | `FetchTlsContextMojo.fetchOne` |
| 4 | Fail or warn per `failOnMissingFile` | `FetchTlsContextMojo.handleFailure` |
| 5 | Be publishable to Anypoint Exchange | `exchange-release` profile + workflow |

## External systems

- **AWS S3** — source of truth for keystore/truststore objects (raw bytes).
- **Anypoint Exchange** — Maven repository the plugin is published to and consumed from.
- **GitHub Actions** — CI (LocalStack e2e) and publish pipeline.

## Lifecycle binding

- Goal `fetch-tls-context`, default phase **`generate-resources`** — runs before
  `process-resources`/`package`, so downloaded files are on the classpath when Mule packages.
- `threadSafe = true`.

## Configuration contract (summary)

See [idea.md §5.1](../idea.md) for the authoritative table. Key parameters: `region`,
`awsProfile`, `outputDirectory`, `failOnMissingFile`, `skip`, and a required list of `<file>`
entries (`bucket`, `key`, `targetFileName`, optional `versionId`).

## Quality attributes

- **Security:** least-privilege S3 read; no secrets in `pom.xml`; no file material in logs.
- **Idempotency:** overwrite-on-build; `mvn clean` removes outputs.
- **Testability:** `S3ObjectResolver` is a seam for unit tests; SDK honors `AWS_ENDPOINT_URL_S3`
  for LocalStack without code changes.

## Related docs

[architecture.md](architecture.md) · [LLD.md](LLD.md) · [testing.md](testing.md) · [deploy.md](deploy.md)
