# parent-config

Shared build and runtime baseline for the Dimo backend services. This repository publishes two
Maven artifacts to GitHub Packages (`https://maven.pkg.github.com/dimo-platform/parent-config`):

| Artifact | Type | Purpose |
|---|---|---|
| `com.dimo:dimo-parent` | pom | Parent POM of every service: Spring Boot 3.5 and Spring Cloud management, Java 21, shared dependencies (validation, JWT, MapStruct, Lombok, test starter) |
| `com.dimo:parent-config` | jar | Shared library: response envelope, exception handling, logging and masking, JWT security, action authorization, Feign clients, observability counters, utilities |

The full-history reference is the monorepo [`dimo`](https://github.com/dimo-platform/dimo).
This repository starts from a curated history of its `parent-config/` module and root POM.

## Build

```sh
./mvnw verify        # builds and tests dimo-parent and parent-config
```

Requires Java 21. Layout: `pom.xml` (local aggregator, never published), `dimo-parent/`, `parent-config/`.

## Consuming from a service

1. Configure `~/.m2/settings.xml` with a GitHub personal access token (classic) that has the
   `read:packages` scope, from an account that can read this repository. Never commit it. Maven
   cannot use a repository declared in a service POM to resolve that same POM's parent, so the
   repository is declared in a settings profile:

   ```xml
   <settings>
     <servers>
       <server>
         <id>github-parent-config</id>
         <username>YOUR_GITHUB_USERNAME</username>
         <password>YOUR_TOKEN_WITH_read_packages</password>
       </server>
     </servers>
     <profiles>
       <profile>
         <id>dimo-parent-config</id>
         <repositories>
           <repository>
             <id>github-parent-config</id>
             <url>https://maven.pkg.github.com/dimo-platform/parent-config</url>
             <snapshots><enabled>true</enabled></snapshots>
           </repository>
         </repositories>
       </profile>
     </profiles>
     <activeProfiles>
       <activeProfile>dimo-parent-config</activeProfile>
     </activeProfiles>
   </settings>
   ```

2. In the service `pom.xml`, use the parent and depend on the library with an **exact** version:

   ```xml
   <parent>
     <groupId>com.dimo</groupId>
     <artifactId>dimo-parent</artifactId>
     <version>1.0.0</version>
     <relativePath/>
   </parent>

   <dependency>
     <groupId>com.dimo</groupId>
     <artifactId>parent-config</artifactId>
     <version>1.0.0</version>
   </dependency>
   ```

3. In GitHub Actions, give the job a token with `packages: read` on this package (a repository
   secret holding a PAT, or `GITHUB_TOKEN` once the service repo is granted access to the package)
   and write the same `settings.xml` (server id `github-parent-config`, plus the profile above)
   before running Maven, for example with `actions/setup-java` (`server-id`, `server-username`,
   `server-password`) and a checked-in `.github/maven-settings.xml` that holds the profile only.

## Versioning

- SemVer per repository: `MAJOR.MINOR.PATCH`, release candidates as `1.1.2-rc.2`.
- `develop` always carries `1.0.0-SNAPSHOT`-style development versions; releases exist only as tags.
- The three artifacts of one release share one version. Services pin it exactly; never use ranges.
- MAJOR: breaking change to a public class, auto-configuration, property or the parent POM contract.
  MINOR: backwards-compatible feature. PATCH: backwards-compatible fix.

## Releasing

1. Make sure `develop` is green (`CI` workflow).
2. Tag and push:

   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. The `Publish` workflow sets the Maven version from the tag, runs the tests and deploys both
   artifacts to GitHub Packages with the workflow's `GITHUB_TOKEN` (`packages: write`).
4. Grant each consuming service repository read access on the package
   (Package settings → Manage Actions access) so its CI can resolve it.

A published version cannot be overwritten; fix forward with a new tag.
