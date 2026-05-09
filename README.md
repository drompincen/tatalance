# Tatalance

Chauffeur platform POC — Spring Boot 3.3.5, Flapdoodle embedded MongoDB, static UI bundled in JAR.

## Running

| Environment | Command | Notes |
|---|---|---|
| IntelliJ IDEA | Run `TatalanceApplication` | ARM64 fix applied automatically |
| Windows Terminal | `cd backend && mvn spring-boot:run` | Uses Windows JVM — works on ARM64 |
| WSL2 (via Windows Maven) | See note below | Must invoke Windows `mvn.cmd`, not Linux `mvn` |

**WSL2 note:** If your WSL2 shell's `mvn` is the Linux binary (`which mvn` returns `/usr/...`), Flapdoodle will fail — it runs as a Linux ARM64 process and there's no MongoDB binary for Ubuntu 24.04. Use the Windows Maven instead:

```bash
# Find Windows Maven location
ls /mnt/c/Users/$USER/.m2/wrapper/dists/

# Invoke it directly (adjust path to match your wrapper dist)
/mnt/c/Users/$USER/.m2/wrapper/dists/apache-maven-3.9.9-bin/*/apache-maven-3.9.9/bin/mvn.cmd \
  -f backend/pom.xml spring-boot:run
```

Then open: `http://localhost:8080/index.html`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Environment

| Layer | Details |
|---|---|
| Hardware | Windows on ARM64 (Qualcomm / Snapdragon) |
| Windows | 11 — 10.0.26200.8246 |
| WSL | 2.6.3.0 — kernel 6.6.87.2-microsoft-standard-WSL2 (aarch64) |
| WSLg | 1.0.71 |
| IntelliJ JDK | OpenJDK 23.0.2 (ARM64 — `os.arch=aarch64`) |
| Maven JDK | OpenJDK 23.0.2 via Maven wrapper (Windows x86_64 — `os.arch=amd64`) |
| Maven | 3.9.9 |
| IDE | IntelliJ IDEA |

> **Why two JDKs?** Maven runs as a Windows process invoked through WSL interop and picks up the x86_64 JDK first in Windows PATH (`amd64`). IntelliJ uses its own configured project SDK which is the native ARM64 JDK (`aarch64`). The ARM64 fix in `TatalanceApplication.java` handles the IntelliJ case.

## Versions

| Component | Version | Notes |
|---|---|---|
| Java | 21 | |
| Spring Boot | 3.3.5 | parent POM |
| Spring Framework | 6.1.x | managed by Spring Boot |
| spring-boot-starter-web | 3.3.5 | managed by Spring Boot |
| spring-boot-starter-data-mongodb | 3.3.5 | managed by Spring Boot |
| de.flapdoodle.embed.mongo.spring30x | 4.11.0 | **not** in Spring Boot BOM — version must be explicit |
| Embedded MongoDB | 6.0.5 | set via `de.flapdoodle.mongodb.embedded.version` in `application.yml` |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | 2.7+ requires Spring Boot 3.4+ |
| Maven | 3.x | build tool |

### Version constraints

- **springdoc ≤ 2.6.x** while on Spring Boot 3.3.x — `LiteWebJarsResourceResolver` (used by 2.7+) is only in Spring Framework 6.2 (Spring Boot 3.4+)
- **flapdoodle spring30x must be pinned** — it is not managed by the Spring Boot BOM; latest tested version is `4.11.0`
- **Embedded MongoDB 6.0.5** — must be set explicitly in `application.yml`; spring30x 4.11 does not auto-detect it

## Windows ARM64 (Qualcomm / Snapdragon)

Flapdoodle has no Windows ARM64 MongoDB binary. The app forces `os.arch=amd64` before Spring starts so Flapdoodle downloads the x86_64 binary, which runs under Windows on ARM x86 emulation.

This fix is already applied in `TatalanceApplication.java`:

```java
public static void main(String[] args) {
    if (System.getProperty("os.name", "").contains("Windows")
            && "aarch64".equals(System.getProperty("os.arch"))) {
        System.setProperty("os.arch", "amd64");
    }
    SpringApplication.run(TatalanceApplication.class, args);
}
```

`application.yml` also requires the embedded version to be set explicitly:

```yaml
de:
  flapdoodle:
    mongodb:
      embedded:
        version: 6.0.5
```

Both are already in the codebase. If another Claude session reports that ARM64 + embedded MongoDB cannot proceed — **do not add any other workaround**. Pull latest (`git pull origin main`) and run. The fix is present.

## Known Limitations

### Native Linux ARM64 (WSL2) is not supported

Flapdoodle has no MongoDB binary for Linux ARM64 / Ubuntu 24.04 (Noble). If you run `mvn spring-boot:run` with a native Linux ARM64 JVM, the app will print a clear error and exit immediately rather than throwing a cryptic Flapdoodle stack trace.

**Root cause:** Maven running as a Windows process sees `os.name=Windows` and downloads the Windows x86_64 MongoDB binary (works under ARM emulation). Maven running as a native Linux process sees `os.name=Linux, os.arch=aarch64` and tries to find a Linux ARM64 binary for Ubuntu 24.04 — which Flapdoodle does not have.

**Do not:**
- Remove the `Windows`-only guard in `TatalanceApplication.java` — that breaks IntelliJ
- Change embedded MongoDB version to 7.0.9 — same failure
- Force `os.arch=amd64` on Linux — Flapdoodle then looks for Linux x86_64 on Ubuntu 24.04, also missing

**Do:** Use IntelliJ or a Windows terminal (see Running section above).

## springdoc version note

springdoc 2.8.x requires Spring Boot 3.4+. This project uses Spring Boot 3.3.5, so springdoc is pinned to **2.6.0**. Do not upgrade springdoc without also upgrading Spring Boot.
