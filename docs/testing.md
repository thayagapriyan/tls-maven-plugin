# Testing — aws-tls-injector-maven-plugin

The plugin is proven correct **in isolation** here (no real AWS). Real-world integration
(Exchange + real S3 via OIDC) is exercised in the consuming Mule app repo.

## Test layers

| Layer | What | Command | AWS? |
|-------|------|---------|------|
| Unit | Mojo behavior (fake resolver) + SDK mapping (mock S3Client) | `mvn test` | No |
| Invoker (local) | `skip-flag`, `local-fake-s3` lifecycle wiring | `mvn verify` | No |
| Invoker (download) | `aws-download` real GetObject | `mvn verify -Daws.it.bucket=...` | LocalStack or real |

## Local — unit + wiring ITs

```powershell
mvn clean verify
```
Runs unit tests + `skip-flag` (skip flag honored) + `local-fake-s3` (fake bucket,
`failOnMissingFile=false` → graceful warning, no file written). `aws-download` is excluded.

## Local — end-to-end with LocalStack

```powershell
# 1. Start LocalStack + seed a keystore
docker run -d --name localstack -p 4566:4566 -e SERVICES=s3 localstack/localstack:3
keytool -genkeypair -alias mule-tls -keyalg RSA -keysize 2048 -validity 365 `
  -dname "CN=localhost" -keystore ks.jks -storepass changeit -keypass changeit
$env:AWS_ACCESS_KEY_ID="test"; $env:AWS_SECRET_ACCESS_KEY="test"; $env:AWS_DEFAULT_REGION="us-east-1"
aws --endpoint-url http://localhost:4566 s3 mb s3://mule-tls-bucket
aws --endpoint-url http://localhost:4566 s3 cp ks.jks s3://mule-tls-bucket/mule-app/dev/keystore.jks

# 2. Point the plugin's SDK at LocalStack and run the download IT
$env:AWS_ENDPOINT_URL_S3="http://s3.localhost.localstack.cloud:4566"
mvn clean verify -Daws.it.bucket=mule-tls-bucket -Daws.it.key=mule-app/dev/keystore.jks -Daws.it.region=us-east-1
```

`-Daws.it.bucket` activates the `aws-it` profile, which clears the default `aws-download` exclude
and runs only that IT. `verify.groovy` asserts the file was downloaded and written.

## CI — e2e on every push/PR

`.github/workflows/ci.yml` runs three jobs:
- `unit-tests` — `mvn clean test`
- `invoker-tests-local` — `mvn clean verify` (no AWS)
- `e2e-localstack` — LocalStack service container → seed keystore → `mvn verify -Daws.it.bucket=...`

No AWS account or OIDC is used in this repo.

## Gotchas

- The plugin's S3 client has no `endpointOverride`; LocalStack works via `AWS_ENDPOINT_URL_S3`
  using the `s3.localhost.localstack.cloud` virtual-host domain.
- `aws-download` failing on a plain `mvn install` means the default exclude was removed — restore it.
- **No AWS credentials at all** is handled gracefully: a missing-credentials `SdkException` is
  mapped to `S3FetchException`, so `local-fake-s3` (`failOnMissingFile=false`) warns and passes on
  a clean CI runner. If you see a hard `MojoExecutionException: Unable to load credentials`, the
  `SdkException` catch in `AwsS3ObjectResolver.fetch` was removed.

## Related docs

[develop.md](develop.md) · [deploy.md](deploy.md) · [LLD.md](LLD.md)
