package com.stillgrove.stillgroveapi.auth.controller;

import com.stillgrove.stillgroveapi.auth.dto.Credentials;
import com.stillgrove.stillgroveapi.auth.dto.UserView;
import com.stillgrove.stillgroveapi.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService service) { this.service = service; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public UserView register(@Valid @RequestBody Credentials body, HttpServletRequest request, HttpServletResponse response) {
        UserView user = service.register(body.email(), body.password());
        signIn(user, request, response);
        return user;
    }

    @PostMapping("/login")
    public UserView login(@Valid @RequestBody Credentials body, HttpServletRequest request, HttpServletResponse response) {
        UserView user = service.login(body.email(), body.password());
        signIn(user, request, response);
        return user;
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) {
        return service.currentUser(Long.parseLong(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private void signIn(UserView user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(String.valueOf(user.id()), null, java.util.List.of()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
