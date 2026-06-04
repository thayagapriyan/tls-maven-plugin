# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Each entry also records the **prompt** that drove the change, so the project's history of intent
stays alongside its history of code.

## [Unreleased]

### Changed
- **Publish the plugin to GitHub Packages instead of Anypoint Exchange.** A Maven *build plugin*
  must be a resolvable `maven-plugin` artifact in a real Maven repo so the consuming Mule app can
  download and execute its goal. Anypoint Exchange's Maven facade cannot host that — it publishes
  file "assets" and rejected this `maven-plugin` outright with **"Could not determine asset type"**
  (after earlier rejecting plain `maven-deploy` with **HTTP 412**). Exchange was the wrong home.
  - Replaced the `exchange-release` profile with a `github-release` profile whose
    `distributionManagement` is `https://maven.pkg.github.com/thayagapriyan/tls-maven-plugin`;
    the stock `maven-deploy-plugin` uploads there.
  - Removed `exchange-mule-maven-plugin`, the `maven-jar-plugin` `classifier=custom` attach, the
    `<type>custom</type>` / `exchange.mule.maven.plugin.version` / `anypoint.orgId` properties, and
    the `anypoint-exchange*` repositories.
  - Rewrote the publish workflow (`publish-exchange.yml` → `publish-plugin.yml`) to deploy with the
    built-in `GITHUB_TOKEN` — no Exchange secrets needed.
  - `<packaging>maven-plugin</packaging>` and the org-GUID `groupId` are unchanged, so consumer
    coordinates didn't churn; only the *host* moved.
  - Version stays `1.0.5` (already bumped); future releases bump `<version>` + the consumer's
    `tls.injector.plugin.version`.
  - _Prompt:_ fix the failing Exchange publish (`exchange-pre-deploy ... Artifact could not be
    resolved`, `deploy ... 412`, then `Could not determine asset type`).

### Changed
- **groupId is now the Anypoint org GUID** (`3075da4c-6c1a-46d3-984a-191b16b7e34e`) instead of
  `com.priyan.maven`, so the plugin builds, publishes to Exchange, and is consumed under one set
  of coordinates. The Java package stays `com.priyan.maven` (independent of groupId). The `src/it`
  poms now reference the plugin via `@project.groupId@` so they track the groupId automatically,
  and the consuming Mule app resolves it under `${anypoint.orgId}` with no groupId override.
  `publish-exchange.yml` dropped the `sed` rewrite step — it now does a single
  `mvn -Pexchange-release clean deploy`.
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
- **Exchange publish failed with `Goal: help already exists ... HelpMojo`.** The old
  `publish-exchange.yml` built once under `com.priyan.maven`, then `sed`-rewrote the groupId and
  ran `deploy` without `clean`, so the plugin-descriptor step generated a second `HelpMojo` under
  the GUID package and conflicted with the stale one. Resolved by making the groupId permanently
  the org GUID and deploying with a single `clean deploy` (no rewrite). Verified `mvn clean
  install` is green under the GUID groupId (12 unit tests, ITs Passed: 2).
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
- *(CI log: `local-fake-s3` failed — "Unable to load credentials")* → broadened
  `AwsS3ObjectResolver.fetch` to catch `SdkException` so missing-credential failures honor
  `failOnMissingFile`.
- *(CI log: publish failed — "Goal: help already exists ... HelpMojo")* and *"update maven plugin
  pom to use my org id as group id 3075da4c-6c1a-46d3-984a-191b16b7e34e"* → set the groupId to the
  org GUID permanently, switched ITs to `@project.groupId@`, and removed the `sed` step in favor of
  a single `clean deploy`.
- *(CI log: exchange pre-deploy failed because the artifact did not exist yet)* → moved
  `exchange-pre-deploy` from the Maven `validate` phase to `package`, so artifact validation runs
  after packaging and before `deploy`.

---

<!--
Maintenance notes for future entries:
- Add new work under [Unreleased] in Added/Changed/Fixed/Removed, plus a Prompts bullet.
- On release, rename [Unreleased] to the version + date and start a fresh [Unreleased] block.
- Keep prompts verbatim (quoted) so intent is traceable.
-->
