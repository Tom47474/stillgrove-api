package com.stillgrove.stillgroveapi;

import com.stillgrove.stillgroveapi.auth.service.AuthService;
import com.stillgrove.stillgroveapi.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ChatServiceIntegrationTests {
    @Autowired AuthService auth;
    @Autowired ChatService chat;

    @Test
    void conversationLifecycleIsScopedToOwner() {
        long userId = auth.register("chat-" + UUID.randomUUID() + "@example.com", "test-password-123").id();
        long otherId = auth.register("other-" + UUID.randomUUID() + "@example.com", "test-password-123").id();
        long id = ((Number) chat.create(userId).get("id")).longValue();
        assertEquals(1, chat.list(userId).size());
        assertThrows(Exception.class, () -> chat.get(otherId, id));
        assertEquals("新的标题", chat.rename(userId, id, "新的标题").get("title"));
        assertTrue(chat.messages(userId, id, Integer.MAX_VALUE).isEmpty());
        chat.delete(userId, id);
        assertTrue(chat.list(userId).isEmpty());
    }
}
