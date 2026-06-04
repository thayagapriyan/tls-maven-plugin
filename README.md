# AWS S3 TLS Context Injector — Maven Plugin

A custom Maven plugin that securely downloads TLS keystore/truststore files from an **AWS S3
bucket** at build time and injects them into your Maven build output
(`target/classes`) — so sensitive `.jks`, `.p12`, and `.pem` files never get committed to
version control.

Built primarily for **Mule 4** applications, but usable by any Maven project that needs
certificates materialized at build time.

> **Status:** Implemented (1.0.0-SNAPSHOT) — builds and unit-tested. See [idea.md](idea.md) for
> the full design, [CHANGELOG.md](CHANGELOG.md) for history, and [CLAUDE.md](CLAUDE.md) for
> contributor/agent guidance.

## Why

Committing keystores and truststores to Git is a compliance risk, and injecting them through
bespoke CI/CD scripts is fragmented and hard to reproduce locally. This plugin moves that logic
into the Maven lifecycle itself: the same `mvn package` works identically on a developer laptop
and in CI.

## How it works

1. Authenticates to AWS using the standard credential provider chain (the plugin stores no keys).
2. Downloads each configured object from S3 (`s3://bucket/key`) as raw bytes.
3. Writes the files into `${project.build.outputDirectory}` during the `generate-resources`
   phase, before packaging.

A `mvn clean` wipes the downloaded files — they only ever live in the ephemeral `target`
directory.

## Usage

Add the plugin to your application's `pom.xml`:

```xml
<plugin>
    <groupId>com.priyan.maven</groupId>
    <artifactId>aws-tls-injector-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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

See the [Configuration Reference](idea.md#51-configuration-reference) for all parameters.

## AWS credentials

The plugin **never accepts or stores raw access keys** — identity is resolved from the
environment via the AWS default credential provider chain:

| Environment | Mechanism                                                                          |
| ----------- | --------------------------------------------------------------------------------- |
| Local dev   | `ProfileCredentialsProvider` via `aws configure` / `aws sso login` (set `<awsProfile>`). |
| CI/CD       | `DefaultCredentialsProvider` — IAM roles via GitHub OIDC, ECS/EKS task roles, etc. |
| Env vars    | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN`.               |

The only credential knob in `pom.xml` is `<awsProfile>` — a non-secret label. Do **not** put
access keys in `pom.xml`; that re-creates the leak this plugin exists to prevent.

### Uploading the TLS files to S3

```powershell
aws s3 cp keystore.jks   s3://my-tls-bucket/mule-app/dev/keystore.jks
aws s3 cp truststore.pem s3://my-tls-bucket/mule-app/dev/truststore.pem
```

No encoding needed — the plugin streams the object bytes straight to disk.

### IAM least privilege

Grant the build identity read access only to the specific bucket/prefix it needs:

```json
{
  "Effect": "Allow",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::my-tls-bucket/mule-app/*"
}
```

## Building from source

```powershell
mvn clean install   # build, run unit tests, install to local ~/.m2
mvn test            # unit tests only
```

The build version is `1.0.0-SNAPSHOT`; install it locally and reference that version in your
app's `pom.xml` until a release is published.

## Security

- Never commit certificate files. The provided [.gitignore](.gitignore) excludes `target/` and
  common keystore extensions.
- The plugin never logs file material — only the S3 location, file path, and byte count.

## License

TBD.
