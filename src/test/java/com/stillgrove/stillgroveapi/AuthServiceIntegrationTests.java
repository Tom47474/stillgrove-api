package com.stillgrove.stillgroveapi;

import com.stillgrove.stillgroveapi.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTests {
    @Autowired AuthService authService;

    @Test
    void registerLoginAndReadCurrentUser() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        var created = authService.register(email, "test-password-123");

        assertEquals(email, created.email());
        assertEquals(created, authService.login(email, "test-password-123"));
        assertEquals(created, authService.currentUser(created.id()));
        assertThrows(ResponseStatusException.class,
                () -> authService.login(email, "wrong-password"));
    }
}
