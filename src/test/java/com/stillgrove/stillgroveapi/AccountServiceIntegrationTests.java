package com.stillgrove.stillgroveapi;

import com.stillgrove.stillgroveapi.account.AccountService;
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
class AccountServiceIntegrationTests {
    @Autowired AuthService auth;
    @Autowired AccountService account;

    @Test void deletionDisablesLoginAndErasesCredentials() {
        String email="delete-"+UUID.randomUUID()+"@example.com";
        long id=auth.register(email,"test-password-123").id();
        account.delete(id,"test-password-123");
        assertThrows(ResponseStatusException.class,()->auth.login(email,"test-password-123"));
    }
}
