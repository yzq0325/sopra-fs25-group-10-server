package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameChatControllerTest {

    @Test
    void testSendMessage() {
        // Arrange
        GameService mockGameService = mock(GameService.class);
        GameChatController controller = new GameChatController(mockGameService);

        Long gameId = 1L;
        ChatMessage inputMessage = new ChatMessage();
        inputMessage.setSender("Alice");
        inputMessage.setContent("Hello!");

        // Act
        ChatMessage result = controller.sendMessage(gameId, inputMessage);

        // Assert
        verify(mockGameService, times(1)).chatChecksForGame(gameId, "Alice");
        assertEquals(gameId, result.getGameId());
        assertEquals("Alice", result.getSender());
        assertEquals("Hello!", result.getContent());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
