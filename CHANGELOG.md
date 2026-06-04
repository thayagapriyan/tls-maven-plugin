# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Each entry also records the **prompt** that drove the change, so the project's history of intent
stays alongside its history of code.

## [Unreleased]

### Changed
- **Source pivot: AWS Secrets Manager → AWS S3.** TLS files are now downloaded from an S3 bucket
  (`s3://bucket/key`) instead of Secrets Manager secrets. S3 objects are raw bytes, so the
  `isBinary`/Base64 handling was removed entirely — `.jks` and `.pem` stream straight to disk.
  - Dependency `secretsmanager` → `s3`.
  - `SecretConfig` → `S3FileConfig` (`bucket`, `key`, `targetFileName`, optional `versionId`);
    `<secrets>/<secret>` config → `<files>/<file>`.
  - `SecretResolver`/`AwsSecretResolver`/`SecretResolutionException` →
    `S3ObjectResolver`/`AwsS3ObjectResolver`/`S3FetchException`.
  - Mojo param `failOnMissingSecret` → `failOnMissingFile` (property `tls.failOnMissingFile`).
- **Credentials decision:** confirmed the plugin stores no AWS keys — identity comes from the
  default provider chain (profile / IAM role / OIDC / env vars); `awsProfile` is the only,
  non-secret, knob. Documented best-practice per environment in `idea.md`/`README.md`.

### Added
- Maven plugin scaffold (`pom.xml`, packaging `maven-plugin`) with AWS SDK v2
  (`secretsmanager`), Maven plugin API, JUnit 5, and Mockito.
- `FetchTlsContextMojo` — goal `fetch-tls-context`, default phase `generate-resources`.
  Parameters: `region`, `awsProfile`, `outputDirectory`, `failOnMissingSecret`, `skip`,
  `secrets`.
- `SecretConfig` — binds each `<secret>` element (`secretName`, `targetFileName`, `isBinary`,
  `versionStage`).
- `SecretResolver` interface + `AwsSecretResolver` — fetches `SecretBinary` or Base64
  `SecretString` from AWS Secrets Manager; never logs secret material.
- `SecretResolutionException` for actionable, secret-free error reporting.
- Unit tests for the Mojo (write, nested dirs, fail/continue modes, skip, path-traversal guard,
  validation) and the resolver (binary, Base64, mismatch, invalid Base64).
- Project docs: `idea.md` (enhanced with config reference, error-handling, testing strategy),
  `README.md`, `CLAUDE.md`, `.gitignore`.
- **Anypoint Exchange publishing** — `exchange-release` profile (`pom.xml`) with
  `distributionManagement` pointing at the Exchange Maven endpoint, and
  `.github/workflows/publish-exchange.yml` (build → rewrite groupId to the org GUID → deploy) on
  a `v*` tag.
- **LocalStack end-to-end CI** — reworked `.github/workflows/ci.yml` into three jobs:
  `unit-tests`, `invoker-tests-local`, and `e2e-localstack` (LocalStack service container seeds a
  keystore and runs the `aws-download` IT via `AWS_ENDPOINT_URL_S3`). Real-AWS/OIDC e2e was
  removed here and is owned by the consuming Mule app repo.
- **Documentation set** — `AGENTS.md` (doc index + required doc-maintenance rule), and
  `docs/architecture.md`, `docs/HLD.md`, `docs/LLD.md`, `docs/develop.md`, `docs/testing.md`,
  `docs/deploy.md`. `CLAUDE.md` now points agents at `AGENTS.md`.

### Fixed
- **Missing AWS credentials crashed the build instead of honoring `failOnMissingFile`.** On a CI
  runner with no creds, `AwsS3ObjectResolver.fetch` let an `SdkClientException` ("Unable to load
  credentials") escape — it is not an `S3Exception` — failing the `local-fake-s3` IT. Broadened the
  catch to `SdkException` so client-side failures (credentials, connectivity, endpoint) map to
  `S3FetchException` and respect `failOnMissingFile`. Added a unit test
  (`wrapsClientErrorAsS3FetchException`); `mvn verify` passes with creds cleared (12 unit tests,
  ITs Passed: 2, Failed: 0).
- **Stale root `TESTING.md`** reduced to a pointer to `docs/testing.md`. It described a
  `terraform/` dir and real-AWS/OIDC CI that were moved out of this repo (plugin CI is now
  LocalStack-only; OIDC/Terraform live in the Mule app repo).
- **`aws-download` IT ran on plain `mvn install` and failed** (no bucket configured). Added
  `pomExcludes` for `aws-download/pom.xml` to the base `maven-invoker-plugin` config; the
  `aws-it` profile clears it with `combine.self="override"`. `mvn clean install` now passes
  (Passed: 2, Failed: 0) and `aws-download` runs only under `-Daws.it.bucket`.

### Prompts
- *"understand idea.md and enhance if you need and create all necessary .md files like
  claude.md"* → enhanced `idea.md`; created `CLAUDE.md`, `README.md`, `.gitignore`.
- *"go ahead and implement but changelog.mg to keep prompts and track changes"* → scaffolded the
  Maven plugin (pom + Java sources + tests) and added this `CHANGELOG.md` tracking prompts and
  changes.
- *"actually we need to revisit our implementation ... we actually need to use aws s3 bucket to
  get tls files .jks or .pem. i have questions how to get aws s3 iam credentials from user or we
  will keep it inside custom plugin."* → pivoted the whole implementation from Secrets Manager to
  S3; recommended and adopted the environment-resolved credential model (no keys stored in the
  plugin); updated all docs.
- *"revisit two repo codespace completely ... tls-maven-plugin should publish custom plugin to
  exchange ... use localstack even in github actions to test that custom plugin working
  correctly."* → added `exchange-release` profile + `publish-exchange.yml`; reworked `ci.yml`
  into unit/invoker/`e2e-localstack` (no real AWS in this repo).
- *"give me steps by steps what should i do to test custom plugin working correctly in my mule
  app"* (during which `mvn clean install` surfaced the IT bug) → fixed the `aws-download` invoker
  IT so it is excluded by default and only runs under the `aws-it` profile.
- *"add different .md file like idea.md, architecture.md, HLD.md, LLD.md, develop.md, testing.md,
  deploy.md ... add these files reference into agents.md then refer agents.md into claude.md ...
  also update changelog.md for all session changes history"* → created the `docs/` set + `AGENTS.md`,
  linked it from `CLAUDE.md`, and recorded this session here.

---

<!--
Maintenance notes for future entries:
- Add new work under [Unreleased] in Added/Changed/Fixed/Removed, plus a Prompts bullet.
- On release, rename [Unreleased] to the version + date and start a fresh [Unreleased] block.
- Keep prompts verbatim (quoted) so intent is traceable.
-->
