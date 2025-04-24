package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private User owner;

    @BeforeEach
    public void setup() {
        testGame = new Game();
        testGame.setGameName("Test Game");
        testGame.setOwnerId(1L);
        testGame.setPlayersNumber(4);
        testGame.setTime(5);
        testGame.setModeType("solo");
        testGame.setPassword("1234");

        owner = new User();
        owner.setUserId(1L);

        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findByownerId(1L)).thenReturn(null);
        when(gameRepository.findBygameName("Test Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(gameService, "messagingTemplate", messagingTemplate);
    }

    @Test
    public void checkIfOwnerExists_ownerNotFound_throwsException() {
        when(userRepository.findByUserId(99L)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.checkIfOwnerExists(99L);
        });

        assertTrue(exception.getReason().toLowerCase().contains("owner"));
    }

    @Test
    public void checkIfGameHaveSameOwner_ownerHasGame_throwsException() {
        when(gameRepository.findByownerId(1L)).thenReturn(new Game());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.checkIfGameHaveSameOwner(1L);
        });

        assertTrue(exception.getReason().toLowerCase().contains("already create a game"));
    }

    @Test
    public void checkIfGameNameExists_duplicateName_throwsException() {
        when(gameRepository.findBygameName("Test Game")).thenReturn(new Game());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.checkIfGameNameExists("Test Game");
        });

        assertTrue(exception.getReason().toLowerCase().contains("gamename"));
    }

    @Test
    public void createGame_invalidModeType_throwsException() {
        testGame.setModeType("invalid-mode");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.createGame(testGame);
        });

        assertTrue(exception.getReason().toLowerCase().contains("invalid mode"));
    }

    @Test
    public void createGame_validInput_success() {
        Game result = gameService.createGame(testGame);

        assertNotNull(result);
        assertEquals("Test Game", result.getGameName());
        assertEquals(1L, result.getOwnerId());
        assertEquals(1, result.getRealPlayersNumber());
    }
}
