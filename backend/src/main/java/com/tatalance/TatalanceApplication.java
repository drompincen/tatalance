package com.tatalance;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
        title = "Tatalance API",
        version = "0.0.1-SNAPSHOT",
        description = "Chauffeur platform — client & job management"
))
@SpringBootApplication
public class TatalanceApplication {
    public static void main(String[] args) {
        // Flapdoodle has no Windows ARM64 MongoDB binary. Force x86_64 so it
        // downloads the x86_64 binary, which runs under Windows on ARM emulation.
        if (System.getProperty("os.name", "").contains("Windows")
                && "aarch64".equals(System.getProperty("os.arch"))) {
            System.setProperty("os.arch", "amd64");
        }
        SpringApplication.run(TatalanceApplication.class, args);
    }
}
