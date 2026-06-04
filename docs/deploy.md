# Deploy / Publish — aws-tls-injector-maven-plugin

The plugin is distributed via **Anypoint Exchange** so Mule apps can resolve it as a Maven asset.

## The groupId constraint

Exchange stores every asset under `groupId = <your Anypoint org/business-group GUID>`. This
project's `groupId` **is** that GUID (`3075da4c-6c1a-46d3-984a-191b16b7e34e`), so the build
publishes under exactly the coordinates consumers reference —
`3075da4c-6c1a-46d3-984a-191b16b7e34e:aws-tls-injector-maven-plugin:<version>`. No coordinate
rewriting happens at publish time. (The Java package stays `com.priyan.maven`; groupId and
package are independent.)

## Manual publish

```powershell
# settings.xml needs <server id="anypoint-exchange-v3"> with a connected app:
#   username: ~~~Client~~~     password: <clientId>~?~<clientSecret>

# groupId is already the org GUID, so just deploy:
mvn -Pexchange-release clean deploy -Danypoint.orgId=<your-org-guid>
```

The `exchange-release` profile (in `pom.xml`) supplies `distributionManagement` pointing at:
`https://maven.anypoint.mulesoft.com/api/v3/organizations/<orgId>/maven`.

Exchange's v3 Maven facade publishes this as a **custom asset** (per MuleSoft's custom-asset
sample), driven by `exchange-mule-maven-plugin` in the `exchange-release` profile:

- `<type>custom</type>` property + `<inherited>false</inherited>` on the plugin — mandatory for
  the handshake.
- `exchange-pre-deploy` (phase `validate`) registers the asset/version with Exchange; then
  `exchange-deploy` (phase `deploy`) uploads.
- `maven-jar-plugin` attaches the built plugin JAR under `classifier=custom` so it ships as the
  asset's file (the JAR remains a fully usable `maven-plugin`).
- The stock `maven-deploy-plugin` is **skipped** — it is incompatible with the facade (a plain
  PUT returns **412**); `exchange-deploy` is the sole uploader.

The profile is self-contained, so ordinary CI (`test`/`verify`) never invokes the Exchange plugin
and needs no credentials.

> **412 Precondition Failed on deploy** has two causes here: (a) the stock `maven-deploy-plugin`
> ran instead of `exchange-deploy` (the facade rejects a plain PUT — `exchange-pre-deploy` must
> register the version first), or (b) the target `<version>` already exists (Exchange versions are
> **immutable**). For (b), bump `pom.xml` `<version>` and the consumer's
> `tls.injector.plugin.version`.
>
> `exchange-pre-deploy` cannot run locally without Exchange credentials in `settings.xml` — it
> round-trips a `preConditions-*.json` through the authenticated `anypoint-exchange-v3` endpoint.
> Run the publish from CI.

### The id-alignment rule (critical)

The **`<server>` id (settings.xml), the `<repository>` id, and the `<distributionManagement>`
repository id must all be identical** — here, `anypoint-exchange-v3`. `exchange-pre-deploy`
uploads its `preConditions-*.json` and then **reads it back through the matching `<repository>`**;
that read needs the `<server>` credentials. If the repository id differs from the server id (this
project originally had the repo as `anypoint-exchange` while the server/distmgmt were
`anypoint-exchange-v3`), the read is unauthenticated and fails with `Artifact could not be
resolved` — even in CI. The repository URL is the **org-scoped** v3 endpoint
(`.../organizations/<orgId>/maven`) so it matches the `runId/...` base the plugin resolves against.

## CI publish

`.github/workflows/publish-exchange.yml` runs on a `v*` tag (or manual dispatch):
1. write `settings.xml` (Exchange connected app)
2. `mvn -Pexchange-release clean deploy` — groupId already equals the org GUID, no rewriting

> `clean` matters: a build left over under a different groupId would cause a
> "Goal: help already exists" plugin-descriptor (`HelpMojo`) conflict.

**Required config:**
- secrets: `EXCHANGE_CLIENT_ID`, `EXCHANGE_CLIENT_SECRET` (connected app, Exchange Contributor)
- variable: `ANYPOINT_ORG_ID`

## Release flow

```powershell
# bump version in pom.xml (e.g. 1.0.0), commit, then:
git tag v1.0.0
git push --tags        # triggers publish-exchange.yml
```

Use semantic versions; Exchange asset versions are immutable once published.

## Consuming the published plugin

See the Mule app repo (`tls-mule-maven-project`): it declares the `anypoint-exchange-v3`
plugin repository and references the plugin under `${anypoint.orgId}`.

## Related docs

[testing.md](testing.md) · [HLD.md](HLD.md) · [architecture.md](architecture.md)
