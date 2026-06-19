package com.tatalance.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository repository;

    public CustomOAuth2UserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");

        AppUser appUser = repository.findByGoogleId(googleId).orElseGet(() -> {
            // Check if a user with this email as username already exists
            AppUser existing = repository.findByUsername(email).orElse(null);
            if (existing != null) {
                existing.setGoogleId(googleId);
                return repository.save(existing);
            }
            // Create new user
            AppUser newUser = new AppUser();
            newUser.setGoogleId(googleId);
            newUser.setUsername(email);
            newUser.setRole("USER");
            newUser.setCreatedAt(Instant.now());
            return repository.save(newUser);
        });

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("appUsername", appUser.getUsername());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "appUsername"
        );
    }
}
