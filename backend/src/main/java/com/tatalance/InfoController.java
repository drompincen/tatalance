package com.tatalance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    @Value("${app.db.type:embedded}")
    private String dbType;

    @GetMapping
    public Map<String, String> info() {
        return Map.of("dbType", dbType);
    }
}
