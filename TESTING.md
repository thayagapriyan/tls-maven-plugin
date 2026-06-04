# Testing the TLS Maven Plugin

This document covers how to test the plugin at every level: unit tests, local integration
tests (no AWS), and full E2E tests against real AWS infrastructure.

---

## 1. Unit Tests (No AWS Required)

Unit tests mock the S3 client and verify all Mojo logic. Run them anywhere:

```powershell
mvn clean test
```

These cover: file writing, path traversal rejection, skip flag, failOnMissingFile behavior,
S3 exception mapping.

---

## 2. Local Integration Tests (No AWS Required)

Integration tests use the **Maven Invoker Plugin** to run the plugin inside a real Maven
build lifecycle. Two tests run without any AWS connection:

- **`local-fake-s3`** — Configures the plugin with a non-existent bucket and
  `failOnMissingFile=false`. Proves the plugin binds to the lifecycle, executes, and handles
  the failure gracefully.
- **`skip-flag`** — Uses `-Dtls.skip=true` and verifies the plugin short-circuits.

### Run locally on your AWS Workspace:

```powershell
# Set dummy AWS env vars so the SDK client can be constructed
$env:AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE"
$env:AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
$env:AWS_DEFAULT_REGION = "us-east-1"

mvn clean verify
```

The dummy credentials are never used for actual API calls — the `local-fake-s3` test uses
`failOnMissingFile=false` so the S3 request fails but the build passes.

---

## 3. E2E Tests Against Real AWS

### Prerequisites

1. **AWS infrastructure** deployed via Terraform (see below)
2. **AWS credentials** configured (your AWS Workspace likely already has them via instance
   profile or SSO)

### Run E2E locally:

```powershell
# If you're on AWS Workspace with IAM role already attached:
mvn clean verify -Daws.it.bucket=tls-maven-plugin-test-ACCOUNT_ID `
                 -Daws.it.key=test/dummy-keystore.jks `
                 -Daws.it.region=us-east-1

# Or with a named profile:
$env:AWS_PROFILE = "your-profile"
mvn clean verify -Daws.it.bucket=YOUR_BUCKET `
                 -Daws.it.key=test/dummy-keystore.jks `
                 -Daws.it.region=us-east-1
```

This runs the `aws-download` integration test which actually downloads from S3.

---

## 4. Deploy AWS Infrastructure (Terraform)

The `terraform/` directory creates everything needed for E2E testing:

| Resource | Purpose |
|----------|---------|
| S3 Bucket (versioned, encrypted) | Holds test keystore/truststore fixtures |
| S3 Objects | `test/dummy-keystore.jks` and `test/dummy-truststore.pem` |
| IAM OIDC Provider | GitHub Actions identity federation |
| IAM Role + Policy | Allows GitHub Actions to read from the test bucket |

### Steps:

```powershell
cd terraform

# 1. Copy and edit the variables file
Copy-Item terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars — set github_org to your GitHub username/org

# 2. Initialize and apply
terraform init
terraform plan
terraform apply

# 3. Note the outputs
terraform output
```

Outputs you'll need:
- **`bucket_name`** → set as GitHub repo variable `TLS_TEST_BUCKET`
- **`test_object_key`** → set as GitHub repo variable `TLS_TEST_KEY`
- **`aws_region`** → set as GitHub repo variable `AWS_REGION`
- **`github_actions_role_arn`** → set as GitHub repo secret `AWS_ROLE_ARN`

---

## 5. Configure GitHub Actions

After deploying Terraform, configure your GitHub repository:

### Repository Variables (Settings → Secrets and variables → Actions → Variables):

| Variable | Value (from Terraform output) |
|----------|-------------------------------|
| `TLS_TEST_BUCKET` | `tls-maven-plugin-test-123456789012` |
| `TLS_TEST_KEY` | `test/dummy-keystore.jks` |
| `AWS_REGION` | `us-east-1` |

### Repository Secrets:

| Secret | Value |
|--------|-------|
| `AWS_ROLE_ARN` | `arn:aws:iam::123456789012:role/tls-maven-plugin-github-actions` |

### What the CI pipeline does:

1. **`unit-tests`** — Runs `mvn test` (always, no AWS)
2. **`integration-tests-local`** — Runs `mvn verify` with dummy creds (tests plugin wiring)
3. **`e2e-aws`** — Runs `mvn verify` with real S3 via OIDC (only if `TLS_TEST_BUCKET` is set)

The E2E job uses GitHub OIDC → STS AssumeRoleWithWebIdentity, so **no long-lived AWS keys**
are stored in GitHub.

---

## 6. Quick Start Summary

| What | Command | AWS Needed? |
|------|---------|-------------|
| Unit tests | `mvn test` | No |
| Integration (wiring) | `mvn verify` (with dummy env vars) | No |
| E2E (real download) | `mvn verify -Daws.it.bucket=... -Daws.it.key=... -Daws.it.region=...` | Yes |
| Full CI | Push to `main` or open PR | Auto (OIDC) |

---

## 7. Troubleshooting

### "Unable to load credentials"
The SDK needs *some* credentials to create the S3 client, even if the request will fail.
For local integration tests without AWS, set the dummy env vars shown in Section 2.

### "Access Denied" on E2E
- Verify the IAM role trust policy includes your branch (`main`, `develop`, or `pull_request`)
- Check the bucket policy doesn't restrict the role
- Run `aws sts get-caller-identity` to confirm which identity is active

### Integration test not running
The `aws-download` test only runs when `-Daws.it.bucket` is passed (gated by the `aws-it`
Maven profile). Without that property, only `local-fake-s3` and `skip-flag` run.
