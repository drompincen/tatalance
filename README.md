# Tatalance

Chauffeur platform POC — Spring Boot 3.3.5, Flapdoodle embedded MongoDB, static UI bundled in JAR.

## Running

```bash
cd backend
mvn spring-boot:run
```

Then open: `http://localhost:8080/index.html`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Stack

- Java 21, Spring Boot 3.3.5
- Flapdoodle embedded MongoDB (`de.flapdoodle.embed.mongo.spring30x:4.11.0`) — no external MongoDB needed
- springdoc-openapi 2.6.0 — OpenAPI spec at `/v3/api-docs`
- Static UI bundled in `src/main/resources/static/`

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
