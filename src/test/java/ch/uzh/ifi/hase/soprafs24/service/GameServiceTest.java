package ch.uzh.ifi.hase.soprafs24.service;


import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UtilService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@ExtendWith(MockitoExtension.class)

public class GameServiceTest {
    @Mock
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
    private Game testGameSolo;
    private Game testGameCombat;
    private GameGetDTO gameGetDTO;
    private User owner;
    private User player2;
    private User ownerSolo;

    @BeforeEach
    public void setup() {
        // Setup Game entity
        testGame = new Game();
        testGame.setGameName("Test Game");
        testGame.setGameId(1L);
        testGame.setOwnerId(1L);
        testGame.setPlayersNumber(4);
        testGame.setTime(5);
        testGame.setModeType("solo");
        testGame.setPassword("1234");
        testGame.setDifficulty("easy");
        
        // Setup maps for tracking questions and answers
        Map<Long, Integer> totalQuestionsMap = new HashMap<>();
        totalQuestionsMap.put(1L, 10);
        testGame.setTotalQuestionsMap(totalQuestionsMap);
        
        Map<Long, Integer> correctAnswersMap = new HashMap<>();
        correctAnswersMap.put(1L, 0);
        testGame.setCorrectAnswersMap(correctAnswersMap);

        // Setup Solo Game entity
        testGameSolo = new Game();
        testGameSolo.setGameName("Test Solo Game");
        testGameSolo.setGameId(2L);
        testGameSolo.setOwnerId(1L);
        testGameSolo.setPlayersNumber(1);
        testGameSolo.setTime(5);
        testGameSolo.setModeType("solo"); // Solo game mode
        testGameSolo.setDifficulty("easy");
        
        // Setup User entity
        ownerSolo = new User();
        ownerSolo.setUserId(1L);
        ownerSolo.setUsername("Test Owner");
        ownerSolo.setReady(true); // Also mark owner (player1) as ready
        
        // Setup maps for tracking questions and answers for solo game
        Map<Long, Integer> totalQuestionsMapSolo = new HashMap<>();
        totalQuestionsMapSolo.put(1L, 10);
        testGameSolo.setTotalQuestionsMap(totalQuestionsMapSolo);
        
        Map<Long, Integer> correctAnswersMapSolo = new HashMap<>();
        correctAnswersMapSolo.put(1L, 0);
        testGameSolo.setCorrectAnswersMap(correctAnswersMapSolo);

         // Setup Combat Game entity
        testGameCombat = new Game();
        testGameCombat.setGameName("Test Combat Game");
        testGameCombat.setGameId(3L);
        testGameCombat.setOwnerId(1L);
        testGameCombat.setPlayersNumber(2);
        testGameCombat.setTime(5);
        testGameCombat.setModeType("combat");
        testGameCombat.setPassword("1234");
        testGameCombat.setDifficulty("easy");
        
        // Setup User entity
        owner = new User();
        owner.setUserId(1L);
        owner.setUsername("Test Owner");
        owner.setReady(true); // Also mark owner (player1) as ready

        player2 = new User();
        player2.setUserId(2L);
        player2.setUsername("PlayerTwo");
        player2.setReady(true); // Mark as ready
        
        // Add both players to the combat game
        List<Long> combatPlayers = Arrays.asList(owner.getUserId(), player2.getUserId());
        testGameCombat.setPlayers(combatPlayers);
        
        // Initialize maps for both players
        Map<Long, Integer> combatTotalQuestionsMap = new HashMap<>();
        combatTotalQuestionsMap.put(owner.getUserId(), 10);
        combatTotalQuestionsMap.put(player2.getUserId(), 10);
        testGameCombat.setTotalQuestionsMap(combatTotalQuestionsMap);
        
        Map<Long, Integer> combatCorrectAnswersMap = new HashMap<>();
        combatCorrectAnswersMap.put(owner.getUserId(), 0);
        combatCorrectAnswersMap.put(player2.getUserId(), 0);
        testGameCombat.setCorrectAnswersMap(combatCorrectAnswersMap);
 
        Country mockCountryEasy = Country.Switzerland;
        Map<String, Object> hintDataEasy = new HashMap<>();
        hintDataEasy.put("hint", "It's in Europe");
        
        List<Map<String, Object>> hintListEasy = new ArrayList<>();
        hintListEasy.add(hintDataEasy);
        
        Map<Country, List<Map<String, Object>>> mockHintMap = new HashMap<>();
        mockHintMap.put(mockCountryEasy, hintListEasy);
        
        // Configure mock behavior
        doReturn(mockHintMap).when(gameService).getHintsOfOneCountry(any(), any(), any());

        //set utilService
        ReflectionTestUtils.setField(gameService, "utilService", utilService);
        doReturn(mockHintMap).when(utilService).getFirstHint(any());

        // Mock repository behavior for both users
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(userRepository.findByUserId(2L)).thenReturn(player2);
        
        // Mock repository behavior
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findByownerId(1L)).thenReturn(null);
        when(gameRepository.findBygameName("Test Game")).thenReturn(null);
        when(gameRepository.findBygameName("Test Solo Game")).thenReturn(null);
        when(gameRepository.findBygameName("Test Combat Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Set messagingTemplate
        ReflectionTestUtils.setField(gameService, "messagingTemplate", messagingTemplate);
        
        // Initialize DTO
        gameGetDTO = new GameGetDTO();
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
    public void userExitGame_userIsNotOwner_moreThanOnePlayer() {
        Game GameToExit = new Game();
        GameToExit.setGameId(100L);
        GameToExit.setPlayersNumber(4);
        GameToExit.setRealPlayersNumber(4);
        GameToExit.setOwnerId(10L);
        GameToExit.setPlayers(new ArrayList<>(List.of(10L, 11L, 12L, 13L)));
        
        User userToExit = new User();
        userToExit.setUserId(11L);
        userToExit.setGame(GameToExit);
        userToExit.setReady(true);
        
        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(11L)).thenReturn(userToExit);
        gameService.userExitGame(userToExit.getUserId());
        
        assertEquals(GameToExit.getRealPlayersNumber(), 3);
        assertEquals(GameToExit.getPlayers(), List.of(10L,12L,13L));
        assertEquals(userToExit.isReady(), false);
    }
    
    @Test
    public void userExitGame_userIsOwner_moreThanOnePlayer() {
        Game GameToExit = new Game();
        GameToExit.setGameId(100L);
        GameToExit.setPlayersNumber(4);
        GameToExit.setRealPlayersNumber(4);
        GameToExit.setOwnerId(10L);
        GameToExit.setPlayers(new ArrayList<>(List.of(10L, 11L, 12L, 13L)));
        
        User userToExit = new User();
        userToExit.setUserId(10L);
        userToExit.setGame(GameToExit);
        userToExit.setReady(true);
        
        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(10L)).thenReturn(userToExit);
        gameService.userExitGame(userToExit.getUserId());
        
        assertEquals(GameToExit.getRealPlayersNumber(), 3);
        assertEquals(GameToExit.getPlayers(), List.of(11L,12L,13L));
        assertEquals(GameToExit.getOwnerId(), 11L);
        assertEquals(userToExit.isReady(), false);
    }

    @Test
    public void userExitGame_onlyOnePlayer() {
        Game GameToExit = new Game();
        GameToExit.setGameId(100L);
        GameToExit.setPlayersNumber(1);
        GameToExit.setRealPlayersNumber(1);
        GameToExit.setOwnerId(10L);
        GameToExit.setPlayers(new ArrayList<>(List.of(10L)));
        
        User userToExit = new User();
        userToExit.setUserId(10L);
        userToExit.setGame(GameToExit);
        userToExit.setReady(true);
        
        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(10L)).thenReturn(userToExit);
        gameService.userExitGame(userToExit.getUserId());
        
        assertEquals(userToExit.getGame(), null);
        assertEquals(userToExit.isReady(), false);
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
    
    @Test
    public void testProcessingAnswer_CorrectCountryAnswer() {
        Long userId = 1L;
        
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGameId(1L);
        gamePostDTO.setSubmitAnswer(Country.Switzerland); // Correct
        gamePostDTO.setHintUsingNumber(2); // 2 hints used
        
        // Mock correct answer
        Map<Long, Country> answers = new HashMap<>();
        answers.put(userId, Country.Switzerland);
        ReflectionTestUtils.setField(gameService, "answers", answers);
        
        // Mock generatedHints
        Map<Country, List<Map<String, Object>>> generatedHints = new HashMap<>();
        List<Map<String, Object>> hintList = List.of(Map.of("hint", "It's in Europe"));
        generatedHints.put(Country.Germany, hintList);
        ReflectionTestUtils.setField(gameService, "generatedHints", generatedHints);
        
        testGame.setPlayers(List.of(userId));
        testGame.setScoreBoard(new HashMap<>(Map.of(userId, 0)));
        when(userRepository.findByUserId(userId)).thenReturn(owner);
        when(gameRepository.findBygameId(1L)).thenReturn(testGame);
        
        GameGetDTO result = gameService.processingAnswer(gamePostDTO, userId);
        
        assertTrue(result.getJudgement());
        assertEquals(hintList, result.getHints());
        assertEquals(80, testGame.getScoreBoard().get(userId)); // 100 - (2 - 1) * 20
        
        verify(gameRepository).save(testGame);
        verify(userRepository).save(owner);
        verify(messagingTemplate).convertAndSend(contains("/scoreBoard"), any(Map.class));
    }
    
    
    @Test
    public void testProcessingAnswer_IncorrectCountryAnswer() {
        Long userId = 1L;
        
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGameId(1L);
        gamePostDTO.setSubmitAnswer(Country.France); // Incorrect
        gamePostDTO.setHintUsingNumber(1); // 1 hint used
        
        // Mock correct answer
        Map<Long, Country> answers = new HashMap<>();
        answers.put(userId, Country.Switzerland);
        ReflectionTestUtils.setField(gameService, "answers", answers);
        
        // Mock generatedHints
        Map<Country, List<Map<String, Object>>> generatedHints = new HashMap<>();
        List<Map<String, Object>> hintList = List.of(Map.of("hint", "It's in Europe"));
        generatedHints.put(Country.Austria, hintList);
        ReflectionTestUtils.setField(gameService, "generatedHints", generatedHints);
        
        testGame.setPlayers(List.of(userId));
        testGame.setScoreBoard(new HashMap<>(Map.of(userId, 0)));
        when(userRepository.findByUserId(userId)).thenReturn(owner);
        when(gameRepository.findBygameId(1L)).thenReturn(testGame);
        
        GameGetDTO result = gameService.processingAnswer(gamePostDTO, userId);
        
        assertFalse(result.getJudgement());
        assertEquals(hintList, result.getHints());
        assertEquals(0, testGame.getScoreBoard().get(userId));
        
        verify(gameRepository).save(testGame);
        verify(messagingTemplate).convertAndSend(contains("/scoreBoard"), any(Map.class));
    }
    
    @Test
    public void testProcessingAnswer_LearningTrackUpdate_CorrectAnswer() {
        Long userId = 1L;
        
        // GamePostDTO with a correct answer
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGameId(1L);
        gamePostDTO.setSubmitAnswer(Country.Switzerland);  // Correct answer
        gamePostDTO.setHintUsingNumber(1);  // 1 hint used
        
        // Mock correct answers
        Map<Long, Country> answers = new HashMap<>();
        answers.put(userId, Country.Switzerland);  // Correct answer
        ReflectionTestUtils.setField(gameService, "answers", answers);
        
        // Mock generatedHints
        Map<Country, List<Map<String, Object>>> generatedHints = new HashMap<>();
        List<Map<String, Object>> hintList = List.of(Map.of("hint", "It's in Europe"));
        generatedHints.put(Country.Switzerland, hintList);  // Correct hint for Switzerland
        ReflectionTestUtils.setField(gameService, "generatedHints", generatedHints);
        
        // Set up the game with one player
        testGame.setPlayers(List.of(userId));
        testGame.setScoreBoard(new HashMap<>(Map.of(userId, 0)));
        testGame.setModeType("combat");

        // Mock User object
        User mockUser = mock(User.class);
        mockUser.setUserId(userId);
        mockUser.setUsername("Test User");
        
        // Mock the user repository call to return the mocked user
        when(userRepository.findByUserId(userId)).thenReturn(mockUser);
        
        // Mock game repository call
        when(gameRepository.findBygameId(1L)).thenReturn(testGame);
        
        GameGetDTO result = gameService.processingAnswer(gamePostDTO, userId);
        
        assertTrue(result.getJudgement());
        
        verify(mockUser).updateLearningTrack(Country.Switzerland);
        verify(userRepository).save(mockUser);
    }
    
    @Test
    public void testProcessingAnswer_LearningTrackUpdate_IncorrectAnswer() {
        Long userId = 1L;
        
        // GamePostDTO with an incorrect answer
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGameId(1L);
        gamePostDTO.setSubmitAnswer(Country.France);  // Incorrect answer
        gamePostDTO.setHintUsingNumber(1);  // 1 hint used
        
        // Mock correct answers
        Map<Long, Country> answers = new HashMap<>();
        answers.put(userId, Country.Switzerland);  // Correct answer is Switzerland
        ReflectionTestUtils.setField(gameService, "answers", answers);
        
        // Mock generatedHints
        Map<Country, List<Map<String, Object>>> generatedHints = new HashMap<>();
        List<Map<String, Object>> hintList = List.of(Map.of("hint", "It's in Europe"));
        generatedHints.put(Country.Austria, hintList);  // Incorrect hint
        ReflectionTestUtils.setField(gameService, "generatedHints", generatedHints);
        
        // Set up the game with one player
        testGame.setPlayers(List.of(userId));
        testGame.setScoreBoard(new HashMap<>(Map.of(userId, 0)));
        testGame.setModeType("combat");

        // Mock User object
        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn(userId);
        when(mockUser.getUsername()).thenReturn("Test User");
        
        // Mock the user repository call
        when(userRepository.findByUserId(userId)).thenReturn(mockUser);
        when(gameRepository.findBygameId(1L)).thenReturn(testGame);
        
        // Call the method
        GameGetDTO result = gameService.processingAnswer(gamePostDTO, userId);
        
        // Validate result
        assertFalse(result.getJudgement());
        assertEquals(hintList, result.getHints());
        
        verify(mockUser, never()).updateLearningTrack(any());
        
        verify(userRepository, never()).save(mockUser);
    }
    
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

    @Test
    public void giveupGame_onlyPlayer_gameDeletedAndUserUpdated() {

        Long userId = 1L;

        Game mockGame = mock(Game.class);
        when(mockGame.getRealPlayersNumber()).thenReturn(1);
        when(mockGame.getGameName()).thenReturn("TestGame");
        when(mockGame.getScore(userId)).thenReturn(50);
        when(mockGame.getCorrectAnswers(userId)).thenReturn(5);
        when(mockGame.getTime()).thenReturn(60);
        when(mockGame.getModeType()).thenReturn("solo");

        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setUsername("player1");
        mockUser.setGame(mockGame);

        when(userRepository.findByUserId(userId)).thenReturn(mockUser);

        gameService.giveupGame(userId);

        verify(mockGame, times(2)).updateScore(eq(userId), eq(-1));
        assertNull(mockUser.getGame());
        assertFalse(mockUser.isReady());
        verify(userRepository).save(mockUser);
        verify(userRepository).flush();
        verify(gameRepository).deleteByGameId(mockGame.getGameId());
    }

    @Test
    void giveupGame_ownerMultiplePlayers_transfersOwnership() {
        // Arrange
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setUsername("player1");
    
        Game mockGame = mock(Game.class);
        mockUser.setGame(mockGame);
        User newOwner = new User();
        newOwner.setUserId(2L);
        newOwner.setUsername("player2");
    
        when(mockGame.getGameId()).thenReturn(1L);
        when(mockGame.getGameName()).thenReturn("TestGame");
        when(mockGame.getScore(userId)).thenReturn(50);
        when(mockGame.getCorrectAnswers(userId)).thenReturn(5);
        when(mockGame.getTotalQuestions(userId)).thenReturn(10);
        when(mockGame.getGameCreationDate()).thenReturn(LocalDateTime.now());
        when(mockGame.getTime()).thenReturn(60);
        when(mockGame.getModeType()).thenReturn("combat");
        when(mockGame.getRealPlayersNumber()).thenReturn(2);
        when(mockGame.getOwnerId()).thenReturn(userId); 
        when(mockGame.getPlayers()).thenReturn(Arrays.asList(userId, 2L));
        when(mockGame.getScoreBoard()).thenReturn(new HashMap<>(Map.of(userId, 50, 2L, 30)));
        when(userRepository.findByUserId(userId)).thenReturn(mockUser);
        when(userRepository.findByUserId(2L)).thenReturn(newOwner);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(gameRepository.save(any(Game.class))).thenReturn(mockGame);

        doAnswer(invocation -> {
            when(mockGame.getOwnerId()).thenReturn(2L); 
            return null;
        }).when(mockGame).setOwnerId(2L);
    
        // Mock getGameLobby to avoid unnecessary WebSocket calls
        doNothing().when(gameService).getGameLobby();
    
        // Act
        gameService.giveupGame(userId);
    
        // Assert
        verify(mockGame).setRealPlayersNumber(1);
        verify(mockGame).setOwnerId(2L); 
        verify(mockGame, times(2)).removePlayer(mockUser); 
        verify(mockGame).updateScore(eq(userId), eq(-1));
        verify(messagingTemplate).convertAndSend(eq("/topic/game/1/owner"), eq(2L)); 
        verify(messagingTemplate).convertAndSend(eq("/topic/user/1/scoreBoard"), any(Map.class));
        assertNull(mockUser.getGame());
        assertFalse(mockUser.isReady());
        verify(userRepository).save(mockUser);
        verify(userRepository).flush();
        verify(gameRepository).save(mockGame);
        verify(gameRepository).flush();
    }
    
    @Test
    public void startSoloGame_validInput_gameStartedSuccessfully() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Solo Game");
        inputGame.setModeType("solo");
        inputGame.setTime(5);
        inputGame.setPlayersNumber(1);
        inputGame.setPassword("pass");
        
        // Mock owner
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findBygameName("Solo Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game savedGame = invocation.getArgument(0);
            savedGame.setGameId(1L);
            return savedGame;
        });
        
        // Act
        gameService.startSoloGame(inputGame);
        
        // Assert
        verify(gameRepository, atLeastOnce()).save(any(Game.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/startsolo/1/gameId"), eq(1L));
        verify(messagingTemplate).convertAndSend(eq("/topic/start/1/hints"), any(GameGetDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/start/1/ready-time"), eq(5));
    }
    
    @Test
    public void startSoloGame_invalidMode_throwsException() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Invalid Mode Game");
        inputGame.setModeType("arcade");
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> gameService.startSoloGame(inputGame));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid mode type"));
    }
    
    @Test
    public void startSoloGame_lockNotAcquired_logsWarningAndReturns() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Locked Game");
        inputGame.setModeType("solo");
        
        ReentrantLock lock = mock(ReentrantLock.class);
        when(lock.tryLock()).thenReturn(false);
        ReflectionTestUtils.setField(gameService, "userLocks", new ConcurrentHashMap<>(Map.of(1L, lock)));
        
        // Act
        gameService.startSoloGame(inputGame);
        
        // Assert
        verify(gameRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }
    
    @Test
    void startSoloGame_threadInterrupted_sendsTimerInterruptedMessage() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Interrupted Solo Game");
        inputGame.setModeType("solo");
        inputGame.setTime(5);
        inputGame.setPlayersNumber(1);
        inputGame.setPassword("pass");
        
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findBygameName("Interrupted Solo Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            game.setGameId(1L);
            return game;
        });
        
        Thread gameThread = new Thread(() -> gameService.startSoloGame(inputGame));
        gameThread.start();
        
        try {
            Thread.sleep(100);
            gameThread.interrupt();
            gameThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // reset interrupt flag
        }
        
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
        eq("/topic/game/1/timer-interrupted"), eq("TIMER_STOPPED")
        );
    } 
    
    @Test
    void startCombatGame_notAllPlayersReady_throwsResponseStatusException() {
        // Arrange
        Long gameId = 3L;
        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);
        doReturn(false).when(gameService).checkAllReady(gameId);
        
        // Act + Assert
        ResponseStatusException thrown = assertThrows(
        ResponseStatusException.class,
        () -> gameService.startGame(gameId)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatus());
        assertEquals("Not all players are ready", thrown.getReason());
    }
    
    @Test
    void startCombatGame_gameNotFound_throwsResponseStatusException() {
        // Arrange
        Long gameId = 3L;
        when(gameRepository.findBygameId(gameId)).thenReturn(null);
        
        // Act + Assert
        ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> gameService.startGame(gameId)
        );
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Game Not Found", ex.getReason());
    }
    
    @Test
    void startCombatGame_validGame_startGameSuccessfully() throws InterruptedException {
        // Arrange
        Long gameId = 3L;
        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);
        // doReturn(false).when(gameService).checkAllReady(gameId);
        
        // Act
        gameService.startGame(gameId);
        
        // Assert
        verify(gameRepository).save(testGameCombat);
        verify(messagingTemplate).convertAndSend(matches("/topic/start/3/hints"), any(GameGetDTO.class));
        verify(messagingTemplate).convertAndSend(matches("/topic/start/3/ready-time"), eq(5));
        
        // Verify the game running flag is set to true
        assertTrue(testGameCombat.getGameRunning());

    }

    @Test
    void startCombatGame_threadInterrupted_sendsTimerInterruptedMessage() {
        // Arrange
        Long gameId = 1L;

        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);
        doReturn(true).when(gameService).checkAllReady(gameId);
        when(gameRepository.save(any(Game.class))).thenReturn(testGameCombat);

        Thread combatGameThread = new Thread(() -> gameService.startGame(gameId));
        combatGameThread.start();

        try {
            Thread.sleep(100); // allow thread to start
            combatGameThread.interrupt(); // simulate interruption
            combatGameThread.join(); // wait for thread to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt status
        }

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
            eq("/topic/game/" + gameId + "/timer-interrupted"),
            eq("TIMER_STOPPED")
        );
    }
}