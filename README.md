# Tatalance

Chauffeur platform POC — Spring Boot 3.3.5, Flapdoodle embedded MongoDB, static UI bundled in JAR.

## Running

```bash
cd backend
mvn spring-boot:run
```

Then open: `http://localhost:8080/index.html`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

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

## springdoc version note

springdoc 2.8.x requires Spring Boot 3.4+. This project uses Spring Boot 3.3.5, so springdoc is pinned to **2.6.0**. Do not upgrade springdoc without also upgrading Spring Boot.
