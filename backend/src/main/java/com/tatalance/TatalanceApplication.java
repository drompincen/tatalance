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
        SpringApplication.run(TatalanceApplication.class, args);
    }
}
