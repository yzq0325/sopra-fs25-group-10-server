package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Arrays;
import java.util.Arrays;
import java.lang.reflect.Field;

public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private User owner;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Initialize a game
        testGame = new Game();
        testGame.setGameName("Test Game");
        testGame.setOwnerId(1L);
        testGame.setPlayersNumber(4);
        testGame.setTime(5);
        testGame.setModeType("solo");

        // Initialize a user
        owner = new User();
        owner.setUserId(1L);
        owner.setUsername("owner");

        // Mock user lookup
        Mockito.when(userRepository.findByUserId(1L)).thenReturn(owner);
        
        // Mock game name and owner uniqueness checks
        Mockito.when(gameRepository.findByownerId(1L)).thenReturn(null);
        Mockito.when(gameRepository.findBygameName("Test Game")).thenReturn(null);
        
        // Mock saving the game
        Mockito.when(gameRepository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void createGame_validInputs_success() {
        // when
        Game createdGame = gameService.createGame(testGame);

        // then
        assertNotNull(createdGame);
        assertEquals("Test Game", createdGame.getGameName());
        assertEquals(1L, createdGame.getOwnerId());
        assertEquals(1, createdGame.getRealPlayersNumber());
        assertFalse(createdGame.getGameRunning());
    }

    @Test
    public void createGame_invalidModeType_throwsException() {
        // given
        testGame.setModeType("invalidMode");

        // then
        assertThrows(ResponseStatusException.class, () -> gameService.createGame(testGame));
    }

    @Test
    public void createGame_duplicateGameName_throwsException() {
        // given
        Mockito.when(gameRepository.findBygameName("Test Game")).thenReturn(new Game());

        // then
        assertThrows(ResponseStatusException.class, () -> gameService.createGame(testGame));
    }



    
  
}
