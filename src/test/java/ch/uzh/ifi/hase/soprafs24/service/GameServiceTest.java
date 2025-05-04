package ch.uzh.ifi.hase.soprafs24.service;


import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UtilService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@ExtendWith(MockitoExtension.class)

public class GameServiceTest {
    @Autowired
    private UtilService utilService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    
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
    
    @Test
    public void userJoinGame_successfullyJoinsGame() {
        Long userId = 2L;

        Game mockGame = new Game();
        mockGame.setGameId(100L);
        mockGame.setPassword("pass123");
        mockGame.setGameRunning(false);
        mockGame.setPlayersNumber(4);
        mockGame.setRealPlayersNumber(1);
        mockGame.setPlayers(new ArrayList<>(List.of(1L)));

        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setUsername("newPlayer");

        Game incoming = new Game();
        incoming.setGameId(100L);
        incoming.setPassword("pass123");

        // Mocks
        when(gameRepository.findBygameId(100L)).thenReturn(mockGame);
        when(userRepository.findByUserId(userId)).thenReturn(mockUser);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));
        doReturn(List.of(mockUser)).when(gameService).getGamePlayers(100L); // Mocks getGamePlayers

        // Act
        gameService.userJoinGame(incoming, userId);

        // Assert
        assertEquals(2, mockGame.getRealPlayersNumber());
        verify(gameRepository).save(any(Game.class));
        verify(userRepository).save(any(User.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/ready/100/players"), any(Object.class));
    }

    @Test
    public void userJoinGame_gameRunning_throwsUnauthorized() {
        Game runningGame = new Game();
        runningGame.setGameId(100L);
        runningGame.setGameRunning(true); // the game is running

        when(gameRepository.findBygameId(100L)).thenReturn(runningGame);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.userJoinGame(runningGame, 2L);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("game is running"));
    }

    @Test
    public void userJoinGame_gameFull_throwsBadRequest() {
        Game fullGame = new Game();
        fullGame.setGameId(100L);
        fullGame.setGameRunning(false);
        fullGame.setPlayersNumber(4);
        fullGame.setRealPlayersNumber(4); // Max players reached
    
        when(gameRepository.findBygameId(100L)).thenReturn(fullGame);
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.userJoinGame(fullGame, 2L);
        });
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("this game is full"));
    }
    
    @Test
    public void userJoinGame_wrongPassword_throwsUnauthorized() {
        Game dbGame = new Game();
        dbGame.setGameId(100L);
        dbGame.setGameRunning(false);
        dbGame.setPlayersNumber(4);
        dbGame.setRealPlayersNumber(2);
        dbGame.setPassword("correct123");

        Game joinRequest = new Game();
        joinRequest.setGameId(100L);
        joinRequest.setPassword("wrongPassword");

        when(gameRepository.findBygameId(100L)).thenReturn(dbGame);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.userJoinGame(joinRequest, 2L);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("Wrong Password"));
    }

    @Test
    void chatChecksForGame_validData_passes() {
        Long gameId = 1L;
        String username = "Player1";
        Long userId = 100L;

        Game game = new Game();
        game.setPlayers(Collections.singletonList(userId));
        game.setEndTime(null);

        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);

        when(gameRepository.findBygameId(gameId)).thenReturn(game);
        when(userRepository.findByUsername(username)).thenReturn(user);

        assertDoesNotThrow(() -> gameService.chatChecksForGame(gameId, username));
    }

    @Test
    void chatChecksForGame_gameNotFound_throwsNotFound() {
        when(gameRepository.findBygameId(1L)).thenReturn(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                gameService.chatChecksForGame(1L, "Player1"));
        assertEquals(404, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Game Not Found"));
    }

    @Test
    void chatChecksForGame_userNotFound_throwsNotFound() {
        Game game = new Game();
        game.setPlayers(Collections.singletonList(1L));
        when(gameRepository.findBygameId(1L)).thenReturn(game);
        when(userRepository.findByUsername("Player1")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                gameService.chatChecksForGame(1L, "Player1"));
        assertEquals(404, ex.getStatus().value());
        assertTrue(ex.getReason().contains("User Not Found"));
    }

    @Test
    void chatChecksForGame_userNotInGame_throwsForbidden() {
        Game game = new Game();
        game.setPlayers(Collections.singletonList(2L));  // different user ID

        User user = new User();
        user.setUserId(1L);
        user.setUsername("Player1");

        when(gameRepository.findBygameId(1L)).thenReturn(game);
        when(userRepository.findByUsername("Player1")).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                gameService.chatChecksForGame(1L, "Player1"));
        assertEquals(403, ex.getStatus().value());
        assertTrue(ex.getReason().contains("User is not a participant"));
    }

    @Test
    void chatChecksForGame_gameEnded_throwsForbidden() {
        Long userId = 1L;
        Game game = new Game();
        game.setPlayers(Collections.singletonList(userId));
        game.setEndTime(LocalDateTime.now());  // ended game

        User user = new User();
        user.setUserId(userId);
        user.setUsername("Player1");

        when(gameRepository.findBygameId(1L)).thenReturn(game);
        when(userRepository.findByUsername("Player1")).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                gameService.chatChecksForGame(1L, "Player1"));
        assertEquals(403, ex.getStatus().value());
        assertTrue(ex.getReason().contains("Game endeded"));
    }

    // @Test
    // public void userStartGame_successfullyStartGame() {
    //     Long gameId = 100L;
    //     Long ownerId = 1L;
    //     Long playerId = 2L;

    //     Game mockGame = new Game();
    //     mockGame.setGameId(gameId);
    //     mockGame.setOwnerId(ownerId);
    //     mockGame.setPlayers(new ArrayList<>(List.of(ownerId, playerId)));
    //     mockGame.setGameRunning(false);
    //     mockGame.setTime(5); // 5 minutes game duration

    //     User owner = new User();
    //     owner.setUserId(ownerId);
    //     owner.setUsername("ownerUser");
    //     User player = new User();
    //     player.setUserId(playerId);
    //     player.setUsername("playerUser");

    //     Map<Country, List<String>> generatedHints = new HashMap<>();
    //     Country country = Country.Afghanistan;
    //     generatedHints.put(country, List.of("Hint1", "Hint2","Hint3","Hint4","Hint5"));

    //     when(gameRepository.findBygameId(gameId)).thenReturn(mockGame);
    //     when(userRepository.findByUserId(ownerId)).thenReturn(owner);
    //     when(userRepository.findByUserId(playerId)).thenReturn(player);
    //     when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

    //     doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));
    //     doNothing().when(utilService).timingCounter(anyInt(), anyLong());

    //     gameService.startGame(gameId);

    //     assertTrue(mockGame.getGameRunning());
    //     assertEquals(0, mockGame.getScore(ownerId)); 
    //     assertEquals(0, mockGame.getScore(playerId));
    //     assertNotNull(mockGame.getGameCreationDate()); 

    //     verify(messagingTemplate).convertAndSend(eq("/topic/start/" + gameId + "/ready-time"), eq(5));
    //     verify(messagingTemplate).convertAndSend(eq("/topic/start/" + gameId + "/hints"), any(GameGetDTO.class));
    //     verify(utilService).timingCounter(eq(5 * 60), eq(gameId)); // 5 minutes converted to seconds
    //     verify(gameRepository, atLeastOnce()).save(mockGame);
    //     verify(gameRepository).flush();
    // }

    @Test
    public void joinGamebyCode_successfully() {
        Game mockGame = new Game();
        mockGame.setGameId(100L);
        mockGame.setGameRunning(false);
        mockGame.setPlayersNumber(4);
        mockGame.setRealPlayersNumber(2);
        mockGame.setGameCode("123456");

        GamePostDTO joinRequest = new GamePostDTO();
        joinRequest.setGameCode("123456");

        when(gameRepository.findBygameCode("123456")).thenReturn(mockGame);

        GameGetDTO joinedGame = gameService.joinGamebyCode(joinRequest);

        assertEquals(100L, joinedGame.getGameId());
        assertEquals(false, joinedGame.getGameRunning());
        assertEquals("123456", joinedGame.getGameCode());
    }

    @Test
    public void joinGamebyCode_wrongGameCode_throwNotFound() {
        Game mockGame = new Game();
        mockGame.setGameId(100L);
        mockGame.setGameRunning(false);
        mockGame.setPlayersNumber(4);
        mockGame.setRealPlayersNumber(2);
        mockGame.setGameCode("123456");

        GamePostDTO joinRequest = new GamePostDTO();
        joinRequest.setGameCode("234567");

        when(gameRepository.findBygameCode("123456")).thenReturn(mockGame);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.joinGamebyCode(joinRequest);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getReason().contains("Game with your gameCode doesn't exist! Please try another gameCode!"));
    }

    @Test
    void toggleReadyStatus_success() {
        Long gameId = 100L;
        Long userId = 2L;
    
        Game mockGame = mock(Game.class);
        given(mockGame.getGameId()).willReturn(gameId);
        given(mockGame.getOwnerId()).willReturn(1L);
    
        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setReady(false);
        mockUser.setGame(mockGame);
    
        User ownerUser = new User();
        ownerUser.setUserId(1L);
        ownerUser.setReady(true);
    
        given(userRepository.findByUserId(userId)).willReturn(mockUser);
        given(userRepository.findByUserId(1L)).willReturn(ownerUser);
        given(gameRepository.findBygameId(gameId)).willReturn(mockGame);
    
        doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));
    
        doReturn(List.of(ownerUser, mockUser)).when(gameService).getGamePlayers(gameId);

        gameService.toggleReadyStatus(gameId, userId);
    
        assertTrue(mockUser.isReady());

        verify(userRepository).save(mockUser);
    
        verify(messagingTemplate).convertAndSend(eq("/topic/ready/" + gameId + "/status"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/ready/" + gameId + "/canStart"), eq(true));
    }

    @Test
    void toggleReadyStatus_userNotInGame_throwsBadRequest() {
        Long gameId = 100L;
        Long userId = 2L;
    
        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setGame(null);
    
        given(userRepository.findByUserId(userId)).willReturn(mockUser);
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.toggleReadyStatus(gameId, userId);
        });
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid user or game"));
    }

    @Test
    void toggleReadyStatus_ownerCannotReady_throwsBadRequest() {
        Long gameId = 100L;
        Long userId = 1L;
    
        Game mockGame = mock(Game.class);
        given(mockGame.getGameId()).willReturn(gameId);
        given(mockGame.getOwnerId()).willReturn(userId);
    
        User mockOwner = new User();
        mockOwner.setUserId(userId);
        mockOwner.setGame(mockGame);
    
        given(userRepository.findByUserId(userId)).willReturn(mockOwner);
        given(gameRepository.findBygameId(gameId)).willReturn(mockGame);
    
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.toggleReadyStatus(gameId, userId);
        });
    
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Owner cannot toggle ready"));
    }

    @Test
    void checkAllReady_allPlayersReady_returnsTrue() {
        Long gameId = 1L;
        Long ownerId = 100L;
    
        User owner = new User();
        owner.setUserId(ownerId);
    
        User player1 = new User();
        player1.setUserId(101L);
        player1.setReady(true);
    
        User player2 = new User();
        player2.setUserId(102L);
        player2.setReady(true);
    
        Game game = new Game();
        game.setOwnerId(ownerId);
    
        List<User> players = List.of(owner, player1, player2);
    
        given(gameRepository.findBygameId(gameId)).willReturn(game);
        doReturn(players).when(gameService).getGamePlayers(gameId);
    
        boolean result = gameService.checkAllReady(gameId);
    
        assertTrue(result);
    }
    
    @Test
    void checkAllReady_somePlayersNotReady_returnsFalse() {
        Long gameId = 1L;
        Long ownerId = 100L;
    
        User owner = new User();
        owner.setUserId(ownerId);
    
        User player1 = new User();
        player1.setUserId(101L);
        player1.setReady(true);
    
        User player2 = new User();
        player2.setUserId(102L);
        player2.setReady(false);
    
        Game game = new Game();
        game.setOwnerId(ownerId);
    
        List<User> players = List.of(owner, player1, player2);
    
        given(gameRepository.findBygameId(gameId)).willReturn(game);
        doReturn(players).when(gameService).getGamePlayers(gameId);
    
        boolean result = gameService.checkAllReady(gameId);
    
        assertFalse(result);
    }
    
    @Test
    void checkAllReady_onlyOwner_returnsTrue() {
        Long gameId = 1L;
        Long ownerId = 100L;
    
        User owner = new User();
        owner.setUserId(ownerId);
    
        Game game = new Game();
        game.setOwnerId(ownerId);
    
        List<User> players = List.of(owner);
    
        given(gameRepository.findBygameId(gameId)).willReturn(game);
        doReturn(players).when(gameService).getGamePlayers(gameId);
    
        boolean result = gameService.checkAllReady(gameId);
    
        assertTrue(result);
    }
}