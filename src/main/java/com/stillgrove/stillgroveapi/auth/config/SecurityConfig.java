package com.stillgrove.stillgroveapi.auth.config;

import com.stillgrove.stillgroveapi.auth.mapper.AuthMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final AuthMapper authMapper;
    public SecurityConfig(AuthMapper authMapper) { this.authMapper=authMapper; }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().access((authentication, context) -> {
                    var principal=authentication.get();
                    if(principal==null || !principal.isAuthenticated() || principal instanceof AnonymousAuthenticationToken)
                        return new AuthorizationDecision(false);
                    try { return new AuthorizationDecision(authMapper.findActiveById(Long.parseLong(principal.getName()))!=null); }
                    catch(NumberFormatException ex) { return new AuthorizationDecision(false); }
                }))
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${stillgrove.cors-origin:http://localhost:5173}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
