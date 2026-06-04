# Low-Level Design — aws-tls-injector-maven-plugin

## Packages & classes

```
com.priyan.maven
├── FetchTlsContextMojo            @Mojo(name="fetch-tls-context", defaultPhase=GENERATE_RESOURCES)
├── config.S3FileConfig           <file> binding: bucket/key/targetFileName/versionId
└── aws
    ├── S3ObjectResolver          interface: byte[] fetch(S3FileConfig); void close()
    ├── AwsS3ObjectResolver       SDK v2 impl; fromConfig(region, awsProfile)
    └── S3FetchException          unchecked; actionable messages, no object bytes
```

## FetchTlsContextMojo

**Parameters** (Maven `@Parameter`):

| Field | Property | Default | Notes |
|-------|----------|---------|-------|
| `region` | `tls.region` | SDK chain | AWS region |
| `awsProfile` | `tls.awsProfile` | — | named profile (non-secret) |
| `outputDirectory` | — | `${project.build.outputDirectory}` | base write dir |
| `failOnMissingFile` | `tls.failOnMissingFile` | `true` | fail vs warn |
| `skip` | `tls.skip` | `false` | skip execution |
| `files` | — | required | `List<S3FileConfig>` |
| `resolver` | — | injected | test seam (package-private setter) |

**execute() flow:**
1. `skip` → log and return.
2. Empty `files` → warn and return.
3. Build `AwsS3ObjectResolver.fromConfig(region, awsProfile)` unless a resolver was injected.
4. For each file → `fetchOne(...)`; close the resolver in `finally` if owned.

**fetchOne():** `validate` → `resolver.fetch` → `resolveTarget` (guard) → `Files.createDirectories`
→ `Files.write`. Logs `Downloaded <location> (<n> bytes) -> <path>`.

**resolveTarget() guard:**
```java
Path base = outputDirectory.normalize();
Path target = base.resolve(file.getTargetFileName()).normalize();
if (!target.startsWith(base)) throw new IllegalArgumentException(...);  // path traversal
```

**Error mapping:**
- `S3FetchException` → `handleFailure` (throw `MojoExecutionException` if `failOnMissingFile`, else warn).
- `IOException` (disk write) → always fatal `MojoExecutionException`.

## AwsS3ObjectResolver

- `fromConfig(region, awsProfile)`: `ProfileCredentialsProvider` when `awsProfile` set, else
  `DefaultCredentialsProvider`; sets `region` when provided. **No `endpointOverride`** — LocalStack
  is reached via the `AWS_ENDPOINT_URL_S3` env var (SDK v2 native).
- `fetch()`: `getObjectAsBytes` with optional `versionId`. Maps `NoSuchKeyException`,
  `NoSuchBucketException`, generic `S3Exception` → `S3FetchException` using
  `awsErrorDetails().errorMessage()` (never object bytes).

## S3FileConfig

Fields `bucket`, `key`, `targetFileName`, `versionId` with getters/setters; `location()` →
`s3://bucket/key`; `toString()` excludes file material.

## Maven plugin descriptor

`maven-plugin-plugin` with `goalPrefix=tls`; `descriptor` (process-classes) + `helpmojo` goals.

## Tests (mirror the seams)

- `FetchTlsContextMojoTest` — fake `S3ObjectResolver`: write, dir creation, fail/continue, skip, traversal guard.
- `AwsS3ObjectResolverTest` — mocked `S3Client`: byte passthrough + exception mapping.

## Related docs

[architecture.md](architecture.md) · [HLD.md](HLD.md) · [develop.md](develop.md) · [testing.md](testing.md)
