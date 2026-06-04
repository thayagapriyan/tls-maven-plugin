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

Exchange treats this `maven-plugin` JAR as a **custom asset**. The v3 Maven facade therefore
requires `exchange-mule-maven-plugin` (`org.mule.tools.maven`, 0.1.7) with two executions:
`exchange-pre-deploy` bound to the **`validate`** phase (it registers the asset/version with
Exchange before any artifact is uploaded) and `exchange-deploy` bound to `deploy` (it performs
the upload). The plugin lives **inside the `exchange-release` profile** so ordinary CI builds
(`test`/`verify`) never invoke it and never need Exchange credentials. The stock
`maven-deploy-plugin` is skipped in that profile so the upload is driven solely by
`exchange-deploy`.

> **412 Precondition Failed on deploy** means `exchange-pre-deploy` did not run (or could not
> reach Exchange) — the facade rejects the upload because the asset version was never
> pre-registered. Ensure the `validate`-phase execution is present and the connected-app
> credentials resolve. Note: `exchange-pre-deploy` cannot succeed locally without Exchange
> credentials in `settings.xml`; it round-trips a `preConditions-*.json` through the authenticated
> `anypoint-exchange-v3` endpoint, so run it from CI (or with real creds).

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
