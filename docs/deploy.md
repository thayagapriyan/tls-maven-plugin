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

This `maven-plugin` is published as a **plain Maven artifact** straight to the Exchange v3 Maven
endpoint using the **stock `maven-deploy-plugin`** — no `exchange-mule-maven-plugin` and no
`exchange-pre-deploy`. That Exchange-plugin flow is for `<packaging>pom</packaging>` *custom*
assets (`<type>custom</type>`); forcing it onto `maven-plugin` packaging makes `exchange-pre-deploy`
fail to resolve its generated `preConditions-*.json` (`Artifact could not be resolved`). Publishing
as a normal Maven artifact keeps it a real, consumable `<plugin>` and lets the consuming Mule app
resolve it from the Exchange Maven repo by coordinates.

> **412 Precondition Failed on deploy** = the target version already exists. Exchange versions are
> **immutable**, so you can never re-publish the same `<version>`. Bump `pom.xml` `<version>`
> (and the consumer's `tls.injector.plugin.version`) for every publish. Pushing a `v*` tag whose
> version was already (even partially) published will 412.

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
