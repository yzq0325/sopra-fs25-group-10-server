package ch.uzh.ifi.hase.soprafs24.controller;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs24.entity.ChatMessage;
// import functions for validation
import ch.uzh.ifi.hase.soprafs24.service.GameService;

@Controller
public class GameChatController {
    private final GameService gameService;

    public GameChatController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/chat/{gameId}")
    @SendTo("/topic/chat/{gameId}")
    public ChatMessage sendMessage(@DestinationVariable Long gameId, ChatMessage message) {
        gameService.chatChecksForGame(gameId, message.getSender());
        message.setGameId(gameId);
        message.setTimestamp(LocalDateTime.now());
        // System.out.println("In chat");
        // System.out.println("Message sent to topic: /topic/chat/" + gameId);
        return message;
    }
}

