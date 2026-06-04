# Deploy / Publish — aws-tls-injector-maven-plugin

The plugin is published to **GitHub Packages** (a real Maven registry) so Mule apps can resolve
and execute it as a build `<plugin>`.

> **Why not Anypoint Exchange?** Exchange's Maven facade hosts *assets* (files), not consumable
> build plugins. We tried it and hit a wall: a plain `maven-deploy` PUT returns **412 Precondition
> Failed**; the `exchange-mule-maven-plugin` custom-asset flow only works for
> `<packaging>pom</packaging>` and rejects `maven-plugin` packaging with **"Could not determine
> asset type."** A Maven *build plugin* must live in a real Maven repo, which GitHub Packages is.

## Coordinates

`3075da4c-6c1a-46d3-984a-191b16b7e34e:aws-tls-injector-maven-plugin:<version>`. The `groupId` is
the historical Anypoint org GUID — kept only so consumer coordinates didn't churn during the move
off Exchange. GitHub Packages accepts any groupId; it could be simplified to `com.priyan.maven`
(the Java package) as a follow-up, which would also require updating the consumer's
`tls.injector.groupId`. The registry is keyed by the repo URL, not the groupId.

## Manual publish

```powershell
# settings.xml needs <server id="github"> (id MUST match the distributionManagement id):
#   <username>your-github-user</username>
#   <password>a PAT with write:packages</password>

mvn -Pgithub-release clean deploy
```

The `github-release` profile (in `pom.xml`) supplies `distributionManagement` pointing at
`https://maven.pkg.github.com/thayagapriyan/tls-maven-plugin`, and the stock `maven-deploy-plugin`
uploads the `maven-plugin` JAR + POM there. Ordinary CI (`test`/`verify`) never activates this
profile and needs no credentials.

## CI publish

`.github/workflows/publish-plugin.yml` runs on a `v*` tag (or manual dispatch):
1. `actions/setup-java` writes `settings.xml` with `<server id="github">` from the built-in
   `GITHUB_TOKEN` (`packages: write`).
2. `mvn -Pgithub-release clean deploy`.

No extra secrets are required — `GITHUB_TOKEN` authenticates the publish.

## Release flow

```powershell
# bump <version> in pom.xml (and the consumer's tls.injector.plugin.version), commit, then:
git tag v1.0.6
git push --tags        # triggers publish-plugin.yml
```

GitHub Packages versions are immutable once published; bump for every release.

## Consuming the published plugin

See the Mule app repo (`tls-mule-maven-project`): it declares a `github` `<pluginRepository>`
(`https://maven.pkg.github.com/thayagapriyan/tls-maven-plugin`) and references the plugin under the
org-GUID groupId. GitHub Packages requires auth even to **read**, so the consumer's build needs a
`<server id="github">` (username = GitHub user, password = a token with `read:packages`); its CI
uses the built-in `GITHUB_TOKEN`.

## Related docs

[testing.md](testing.md) · [HLD.md](HLD.md) · [architecture.md](architecture.md)
