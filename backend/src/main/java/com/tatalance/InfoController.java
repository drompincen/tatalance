package com.tatalance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    @Value("${app.db.type:embedded}")
    private String dbType;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping
    public Map<String, Object> info() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dbType", dbType);
        out.put("googleOAuthEnabled", clientRegistrationRepository != null);
        return out;
    }
}
