package ch.uzh.ifi.hase.soprafs24.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ChatMessageTest {

    @Test
    void testGetContent() {
        ChatMessage msg = new ChatMessage();
        msg.setContent("Hello");
        assertEquals("Hello", msg.getContent());
    }

    @Test
    void testGetGameId() {
        ChatMessage msg = new ChatMessage();
        msg.setGameId(42L);
        assertEquals(42L, msg.getGameId());
    }

    @Test
    void testGetSender() {
        ChatMessage msg = new ChatMessage();
        msg.setSender("Bob");
        assertEquals("Bob", msg.getSender());
    }

    @Test
    void testGetTimestamp() {
        ChatMessage msg = new ChatMessage();
        LocalDateTime now = LocalDateTime.now();
        msg.setTimestamp(now);
        assertEquals(now, msg.getTimestamp());
    }

    @Test
    void testSetContent() {
        ChatMessage msg = new ChatMessage();
        msg.setContent("Test content");
        assertEquals("Test content", msg.getContent());
    }

    @Test
    void testSetGameId() {
        ChatMessage msg = new ChatMessage();
        msg.setGameId(10L);
        assertEquals(10L, msg.getGameId());
    }

    @Test
    void testSetSender() {
        ChatMessage msg = new ChatMessage();
        msg.setSender("Alice");
        assertEquals("Alice", msg.getSender());
    }

    @Test
    void testSetTimestamp() {
        ChatMessage msg = new ChatMessage();
        LocalDateTime timestamp = LocalDateTime.of(2025, 5, 2, 15, 30);
        msg.setTimestamp(timestamp);
        assertEquals(timestamp, msg.getTimestamp());
    }
}
