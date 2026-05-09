# Tatalance

Chauffeur platform POC — Spring Boot 3.3.5, Flapdoodle embedded MongoDB, static UI bundled in JAR.

## Running

```bash
cd backend
mvn spring-boot:run
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

## Luciano Issues

### Cannot start backend from WSL2 Linux CLI

**Symptom:** `mvn spring-boot:run` inside WSL2 fails with Flapdoodle `PlatformPackageResolver` error — no matching MongoDB binary found.

**Error:** `java.lang.RuntimeException: rollback after error on transition to State(Package)` — Flapdoodle tries to resolve `GenericFeatureAwareVersion{6.0.5}:Platform{operatingSystem=Linux, architecture=X86_64, distribution=Ubuntu}` but finds no download URL.

**What was tried:**
1. Overriding `os.arch` to `amd64` (removed the Windows-only check) — Flapdoodle then correctly resolves architecture as X86_64, but still fails because MongoDB 6.0.5 has no x86_64 binary for Ubuntu 24.04 (Noble). Flapdoodle falls back to Ubuntu 20.04 but still cannot resolve.
2. Changing embedded MongoDB version to 7.0.9 — same failure, no matching binary.

**Theory:** The other Claude session works because Maven runs as a **Windows process** via WSL interop (see Environment table: `Maven JDK: OpenJDK 23.0.2 via Maven wrapper (Windows x86_64 — os.arch=amd64)`). As a Windows process, `os.name=Windows` and Flapdoodle downloads the **Windows x86_64** MongoDB binary, which runs natively. In this session, Maven runs as a **native Linux process** inside WSL2 (`os.name=Linux`, `os.arch=aarch64`), so Flapdoodle tries to find a Linux x86_64 binary which doesn't exist for Ubuntu 24.04.

**Environment diff from working session:**

| | Working session | This session |
|---|---|---|
| Maven process | Windows (WSL interop) | Native Linux (WSL2) |
| `os.name` | `Windows 11` | `Linux` |
| `os.arch` | `amd64` | `aarch64` |
| JDK | OpenJDK 23.0.2 (x86_64) | OpenJDK 21.0.10 (aarch64) |
| MongoDB binary resolved | Windows x86_64 | Linux x86_64 (not found) |

**Possible fixes to verify:**
1. Install Maven wrapper (`mvnw.cmd`) so it runs as a Windows process from WSL2 — matching the working session
2. Use MongoDB version that has Ubuntu 24.04 ARM64 support (e.g., 7.0+ or 8.0+) and keep native `aarch64` arch
3. Install `qemu-user-static` to actually execute x86_64 Linux MongoDB binaries under emulation

## springdoc version note

springdoc 2.8.x requires Spring Boot 3.4+. This project uses Spring Boot 3.3.5, so springdoc is pinned to **2.6.0**. Do not upgrade springdoc without also upgrading Spring Boot.
