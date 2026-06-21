package com.tatalance;

import com.tatalance.user.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserDetailsService userDetailsService) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login.html", "/logout",
                                 "/register.html", "/api/users/register",
                                 "/forgot-password.html", "/api/users/forgot-password/**",
                                 "/api/info",
                                 "/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html")
                .permitAll()
            )
            .logout(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/**")
            )
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> {
                oauth2.loginPage("/login.html")
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService));
                if (oauth2LoginSuccessHandler != null) {
                    oauth2.successHandler(oauth2LoginSuccessHandler);
                } else {
                    oauth2.defaultSuccessUrl("/index.html", true);
                }
            });
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
