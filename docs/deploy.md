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

A `maven-plugin` artifact publishes to Exchange as a standard Maven artifact via the normal
`deploy` lifecycle — no `exchange-mule-maven-plugin` is involved. (That plugin is for Mule
app/policy/connector asset types, not a `maven-plugin` JAR, and its `exchange-pre-deploy`
goal fails to resolve its descriptor artifacts when run against this packaging.)

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
