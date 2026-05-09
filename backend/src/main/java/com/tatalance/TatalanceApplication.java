package com.tatalance;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@OpenAPIDefinition(info = @Info(
        title = "Tatalance API",
        version = "0.0.1-SNAPSHOT",
        description = "Chauffeur platform — client & job management"
))
@SpringBootApplication
public class TatalanceApplication {
    private static final Logger log = LoggerFactory.getLogger(TatalanceApplication.class);

    public static void main(String[] args) {
        String osName = System.getProperty("os.name", "");
        String osArch = System.getProperty("os.arch", "");

        // Flapdoodle has no Windows ARM64 MongoDB binary. Force x86_64 so it
        // downloads the x86_64 binary, which runs under Windows on ARM emulation.
        if (osName.contains("Windows") && "aarch64".equals(osArch)) {
            System.setProperty("os.arch", "amd64");
        }

        // Flapdoodle has no Linux ARM64 binary for Ubuntu 24.04 (Noble).
        // Native Linux ARM64 (WSL2) is not a supported run path — use IntelliJ
        // or a Windows terminal where Maven runs as a Windows process.
        if (osName.startsWith("Linux") && "aarch64".equals(osArch)) {
            System.err.println("""
                    ERROR: Flapdoodle embedded MongoDB has no binary for Linux ARM64 / Ubuntu 24.04.
                    Run the app via one of these supported paths instead:
                      - IntelliJ IDEA on Windows (uses Windows JVM, ARM64 fix applied automatically)
                      - Windows Terminal: cd backend && mvn spring-boot:run
                      - WSL2 shell invoking Windows Maven: /mnt/c/path/to/mvn.cmd spring-boot:run
                    See README.md — "Running" section for details.
                    """);
            System.exit(1);
        }

        ConfigurableApplicationContext ctx = SpringApplication.run(TatalanceApplication.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        log.info("UI ready → http://localhost:{}/index.html", port);
    }
}
