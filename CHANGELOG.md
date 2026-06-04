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

---

<!--
Maintenance notes for future entries:
- Add new work under [Unreleased] in Added/Changed/Fixed/Removed, plus a Prompts bullet.
- On release, rename [Unreleased] to the version + date and start a fresh [Unreleased] block.
- Keep prompts verbatim (quoted) so intent is traceable.
-->
