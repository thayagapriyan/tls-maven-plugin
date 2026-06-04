# Testing

> **Moved.** The canonical testing guide is now [docs/testing.md](docs/testing.md), indexed from
> [AGENTS.md](AGENTS.md).

Quick reference:

| Layer | Command | AWS? |
|-------|---------|------|
| Unit | `mvn test` | No |
| Invoker (wiring) | `mvn verify` | No |
| Download IT (LocalStack) | `mvn verify -Daws.it.bucket=... -Daws.it.key=... -Daws.it.region=...` with `AWS_ENDPOINT_URL_S3` set | LocalStack |

This repo proves the plugin **in isolation** with LocalStack — no real AWS account or OIDC.
Real-world integration (Anypoint Exchange + real S3 via OIDC, including the Terraform that
provisions the IAM role) lives in the consuming Mule app repo,
[`tls-mule-maven-project`](../tls-mule-maven-project/docs/testing.md).

See [docs/testing.md](docs/testing.md) for full local + e2e steps.
