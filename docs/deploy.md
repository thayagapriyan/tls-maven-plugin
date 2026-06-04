# Deploy / Publish — aws-tls-injector-maven-plugin

The plugin is distributed via **Anypoint Exchange** so Mule apps can resolve it as a Maven asset.

## The groupId constraint

Exchange stores every asset under `groupId = <your Anypoint org/business-group GUID>`. This
project's source groupId is `com.priyan.maven`, so the publish step **rewrites the project
groupId to the org GUID** before deploying. Consumers then reference it as
`<orgId>:aws-tls-injector-maven-plugin:<version>`.

## Manual publish

```powershell
# settings.xml needs <server id="anypoint-exchange-v3"> with a connected app:
#   username: ~~~Client~~~     password: <clientId>~?~<clientSecret>

# (groupId must be the org id for Exchange — change <groupId> or sed it)
mvn -Pexchange-release deploy -DskipTests -Danypoint.orgId=<your-org-guid>
```

The `exchange-release` profile (in `pom.xml`) supplies `distributionManagement` pointing at:
`https://maven.anypoint.mulesoft.com/api/v3/organizations/<orgId>/maven`.

## CI publish

`.github/workflows/publish-exchange.yml` runs on a `v*` tag (or manual dispatch):
1. build & test the plugin
2. `sed` the project groupId → `ANYPOINT_ORG_ID`
3. `mvn -Pexchange-release deploy`

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
