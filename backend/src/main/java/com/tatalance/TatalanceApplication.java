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
        // Flapdoodle has no Windows ARM64 MongoDB binary. Force x86_64 so it
        // downloads the x86_64 binary, which runs under Windows on ARM emulation.
        if (System.getProperty("os.name", "").contains("Windows")
                && "aarch64".equals(System.getProperty("os.arch"))) {
            System.setProperty("os.arch", "amd64");
        }
        ConfigurableApplicationContext ctx = SpringApplication.run(TatalanceApplication.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        log.info("UI ready → http://localhost:{}/index.html", port);
    }
}
