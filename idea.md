# Idea: AWS S3 TLS Context Injector Maven Plugin

## 1. Overview

When building Mule 4 applications, handling TLS contexts (such as `keystore.jks` or
`truststore.pem`) securely can be challenging. Hardcoding these files inside the application
source code (`src/main/resources`) risks exposing sensitive cryptographic keys in version
control systems (VCS) like GitHub.

This project develops a custom Maven plugin that securely downloads keystore and truststore
files directly from an **AWS S3 bucket** at build time and injects them dynamically into the
application archive (`target/classes`), ensuring sensitive credentials never touch the
repository.

## 2. Problem Statement

- **VCS Security Risk:** Committing `.jks`, `.p12`, or `.pem` files to Git violates security
  compliance.
- **Complex CI/CD Pipelines:** Injecting these files via external pipeline scripts (e.g.,
  GitHub Actions or Jenkins steps) can be messy, fragmented, and hard to replicate locally for
  developers.
- **Environment Drift:** Managing separate keystores for dev, test, and prod often leads to
  configuration errors if handled manually via build scripts.

## 3. Proposed Solution

A unified, developer-friendly solution embedded directly into the Maven lifecycle. The custom
plugin will:

1. Authenticate with AWS using standard credential provider chains (no keys stored by the plugin).
2. Download the keystore/truststore objects from a configured S3 bucket and key.
3. Automatically write the downloaded files to the Maven build directory
   (`${project.build.outputDirectory}`) during the `generate-resources` phase, making them
   seamlessly available to the Mule runtime packaging process.

S3 objects are returned as raw bytes, so `.jks` (binary) and `.pem` (text) files both stream
straight to disk — no Base64 encoding/decoding is required.

## 4. High-Level Architecture & Workflow

```
[ Developer / CI/CD ]
         │
         ▼ (runs 'mvn clean package')
┌────────────────────────────────────────────────────────┐
│ Custom Maven Plugin                                     │
│  1. Authenticates via AWS SDK (default cred chain)      │
│  2. GetObject (e.g. 'mule-app/prod/keystore.jks')       │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼ (S3 GetObject)
             ┌──────────────────────┐
             │ AWS S3 Bucket        │
             │  • keystore.jks      │
             │  • truststore.pem    │
             └───────────┬──────────┘
                         │
                         ▼ (Returns raw bytes)
┌────────────────────────────────────────────────────────┐
│ Custom Maven Plugin                                     │
│  3. Writes object bytes to:                             │
│     target/classes/certificates/                        │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼ (Mule Maven Plugin Execution)
┌────────────────────────────────────────────────────────┐
│ Final Deployable Artifact                               │
│  • Includes dynamic TLS files in the .jar mule-app      │
└────────────────────────────────────────────────────────┘
```

## 5. Sample Plugin Configuration (pom.xml)

The plugin is configurable via standard Maven XML. Below is the target design for how
developers will use it:

```xml
<plugin>
    <groupId>com.yourcompany.maven</groupId>
    <artifactId>aws-tls-injector-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <phase>generate-resources</phase>
            <goals>
                <goal>fetch-tls-context</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <region>us-east-1</region>
        <awsProfile>default</awsProfile>
        <failOnMissingFile>true</failOnMissingFile>
        <skip>false</skip>
        <files>
            <file>
                <bucket>my-tls-bucket</bucket>
                <key>mule-app/dev/keystore.jks</key>
                <targetFileName>certificates/keystore.jks</targetFileName>
            </file>
            <file>
                <bucket>my-tls-bucket</bucket>
                <key>mule-app/dev/truststore.pem</key>
                <targetFileName>certificates/truststore.pem</targetFileName>
            </file>
        </files>
    </configuration>
</plugin>
```

### 5.1 Configuration Reference

| Parameter             | Required | Default                              | Description                                                               |
| --------------------- | -------- | ------------------------------------ | ------------------------------------------------------------------------- |
| `region`              | No       | AWS SDK default chain                | AWS region of the bucket.                                                 |
| `awsProfile`          | No       | (none)                               | Named profile for local `ProfileCredentialsProvider`. Non-secret label.   |
| `outputDirectory`     | No       | `${project.build.outputDirectory}`   | Base directory the files are written under.                              |
| `failOnMissingFile`   | No       | `true`                               | Fail the build if an object cannot be downloaded.                        |
| `skip`                | No       | `false`                              | Skip plugin execution entirely (`-Dtls.skip=true`).                       |
| `files`               | Yes      | —                                    | One or more `<file>` entries to download.                                |
| `file.bucket`         | Yes      | —                                    | The S3 bucket name.                                                      |
| `file.key`            | Yes      | —                                    | The S3 object key (path within the bucket).                             |
| `file.targetFileName` | Yes      | —                                    | Path (relative to `outputDirectory`) the file is written to.            |
| `file.versionId`      | No       | latest                               | Object version id (for versioned buckets).                              |

## 6. Key Technical Implementation Details

### Maven Mojo Design

- **Goal Name:** `fetch-tls-context`
- **Default Phase:** `LifecyclePhase.GENERATE_RESOURCES`
- **Dependencies:** AWS S3 Java SDK v2 (`software.amazon.awssdk:s3`).
- **Mojo class:** annotated with `@Mojo(name = "fetch-tls-context", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)`.

### Credentials — the plugin stores nothing

The single most important security decision: **the plugin never accepts or stores raw AWS access
keys.** Identity is resolved from the environment via the AWS default credential provider chain:

| Environment | Mechanism                                                                       |
| ----------- | ------------------------------------------------------------------------------- |
| Local dev   | `ProfileCredentialsProvider` via `aws configure` / `aws sso login` (`awsProfile`). |
| CI/CD       | `DefaultCredentialsProvider` — IAM roles via GitHub OIDC, ECS/EKS task roles.    |
| Env vars    | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN`.             |

The only credential-related knob exposed in `pom.xml` is `awsProfile`, which is a non-secret
label. A future enhancement could read static keys from a Maven `settings.xml` `<server>` entry
(encryptable, outside the repo) for teams without roles/SSO — but never from `pom.xml`.

### Error Handling & Idempotency

- Missing objects/buckets throw a `MojoExecutionException` when `failOnMissingFile = true`;
  otherwise log a warning and continue.
- Parent directories of `targetFileName` are created automatically.
- A path-traversal guard rejects any `targetFileName` that resolves outside `outputDirectory`.
- Writes are overwrite-on-each-build; a `mvn clean` removes them entirely.
- S3 SDK exceptions (`NoSuchKey`, `NoSuchBucket`, `AccessDenied`) are surfaced with actionable
  messages — never echoing object contents.

## 7. Security Considerations

- **`.gitignore` Integration:** Files are written to the build `target` folder (already ignored
  by convention). Documentation must enforce that `target` artifacts are never committed.
- **No Logging of File Material:** The plugin must never log object bytes — only the S3 location,
  resolved file path, and byte count.
- **Cache Management:** Downloaded certificates reside exclusively in the ephemeral `target`
  directory and are wiped during a `mvn clean` cycle.
- **Least Privilege:** The AWS IAM policy for the build environment should restrict access to
  only the specific bucket/prefix, e.g. `s3:GetObject` on
  `arn:aws:s3:::my-tls-bucket/mule-app/*`.

## 8. Testing Strategy

- **Unit tests:** Mock the `S3Client` to verify byte passthrough and exception mapping; fake the
  `S3ObjectResolver` to verify the Mojo's file writing, directory creation, fail/continue modes,
  skip, and path-traversal guard.
- **Maven Plugin Testing Harness:** Use `maven-plugin-testing-harness` to exercise the Mojo with
  synthetic `pom.xml` configurations.
- **Integration test:** Run against a real (or LocalStack) S3 bucket and a dummy Mule 4
  application using local AWS credentials, asserting the file lands in
  `target/classes/certificates/`.

## 9. Next Steps & Timeline

- **Phase 1:** Bootstrap the Maven Plugin project (Java + `maven-plugin-plugin` packaging). ✅
- **Phase 2:** Integrate AWS SDK v2 S3 and implement the credential/authentication logic. ✅
- **Phase 3:** Implement object download + file-stream writing into the output directory. ✅
- **Phase 4:** Add unit + harness tests; integration-test against S3 (LocalStack) + a Mule app.
- **Phase 5:** Publish to an internal/Maven Central repository and document usage in `README.md`.
