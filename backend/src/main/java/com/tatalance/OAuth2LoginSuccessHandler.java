package com.tatalance;

import com.tatalance.user.AppUserRepository;
import com.tatalance.user.BusinessMode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After Google OAuth, redirect by {@link BusinessMode} (same rule as form login on login.html).
 */
@Component
@ConditionalOnProperty(name = "GOOGLE_CLIENT_ID")
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppUserRepository userRepository;

    public OAuth2LoginSuccessHandler(AppUserRepository userRepository) {
        this.userRepository = userRepository;
        setDefaultTargetUrl("/index.html");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String target = userRepository.findByUsername(authentication.getName())
                .map(u -> u.getBusinessMode() == BusinessMode.FREELANCE
                        ? "/freelance.html"
                        : "/index.html")
                .orElse("/index.html");
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}