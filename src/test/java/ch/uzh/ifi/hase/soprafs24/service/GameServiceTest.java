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
import ch.uzh.ifi.hase.soprafs24.service.UtilService.HintList;

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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
    private Map<Long, Country> answers;
    private Map<Country, List<Map<String, Object>>> generatedHints;

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
        // ownerSolo.setReady(true); // Also mark owner (player1) as ready
        
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
        testGameCombat.setRealPlayersNumber(2);
        testGameCombat.setTime(5);
        testGameCombat.setModeType("combat");
        testGameCombat.setPassword("1234");
        testGameCombat.setDifficulty("easy");
        
        // Setup User entity
        owner = spy(new User());
        owner.setUserId(1L);
        owner.setUsername("Test Owner");
        // owner.setReady(true); // Also mark owner (player1) as ready

        player2 = spy(new User());
        player2.setUserId(2L);
        player2.setUsername("PlayerTwo");
        // player2.setReady(true); // Mark as ready
        
        // Add both players to the combat game
        List<Long> combatPlayers = Arrays.asList(owner.getUserId(), player2.getUserId());
        testGameCombat.setPlayers(combatPlayers);

        // Initialize readyMap for testGameCombat <- new
        Map<Long, Boolean> testGameCombatReadyMap = new HashMap<>();
        testGameCombatReadyMap.put(owner.getUserId(), true); // Owner typically ready
        testGameCombatReadyMap.put(player2.getUserId(), false); // Player 2 not ready by default in this setup
        testGameCombat.setReadyMap(testGameCombatReadyMap);

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
        

        //MockHint
        Country mockCountry = Country.Switzerland;
        List<Map<String, Object>> hintList = new ArrayList<>();
        Map<String, Object> hint1 = new HashMap<>();
        hint1.put("hint", "It's in Europe");
        hintList.add(hint1);
        Map<String, Object> hint2 = new HashMap<>();
        hint2.put("hint", "Famous for chocolate");
        hintList.add(hint2);
        Map<Country, List<Map<String, Object>>> mockHintMap = new HashMap<>();
        mockHintMap.put(mockCountry, hintList);
        List<Map<String, Object>> hintListEasy = new ArrayList<>();
        hintListEasy.add(hintDataEasy);
        
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

        // Initialize readyMap for testGame
        Map<Long, Boolean> testGameReadyMap = new HashMap<>();
        testGameReadyMap.put(1L, true); // Owner
        testGameReadyMap.put(2L, true); // Player
        testGame.setReadyMap(testGameReadyMap);
        testGame.setPlayers(new ArrayList<>(Arrays.asList(1L, 2L)));

        answers = new HashMap<>();
        generatedHints = new HashMap<>();
        ReflectionTestUtils.setField(gameService, "answers", answers);
        ReflectionTestUtils.setField(gameService, "generatedHints", generatedHints);
    }

    // @Test
    // public void checkIfOwnerExists_ownerNotFound_throwsException() {
    //     when(userRepository.findByUserId(99L)).thenReturn(null);
        
    //     ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
    //         gameService.checkIfOwnerExists(99L);
    //     });
        
    //     assertTrue(exception.getReason().toLowerCase().contains("owner"));
    // }
    
    // @Test
    // public void checkIfGameHaveSameOwner_ownerHasGame_throwsException() {
    //     when(gameRepository.findByownerId(1L)).thenReturn(new Game());
        
    //     ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
    //         gameService.checkIfGameHaveSameOwner(1L);
    //     });
        
    //     assertTrue(exception.getReason().toLowerCase().contains("already create a game"));
    // }
    
    // @Test
    // public void checkIfGameNameExists_duplicateName_throwsException() {
    //     when(gameRepository.findBygameName("Test Game")).thenReturn(new Game());
        
    //     ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
    //         gameService.checkIfGameNameExists("Test Game");
    //     });
        
    //     assertTrue(exception.getReason().toLowerCase().contains("gamename"));
        
    // }
    
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
        // userToExit.setReady(true);
        
        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(11L)).thenReturn(userToExit);
        gameService.userExitGame(userToExit.getUserId());
        
        assertEquals(GameToExit.getRealPlayersNumber(), 3);
        assertEquals(GameToExit.getPlayers(), List.of(10L,12L,13L));
        // assertEquals(userToExit.isReady(), false);
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
        // userToExit.setReady(true);

        User newOwner = new User();
        newOwner.setUserId(11L);
        // newOwner.setReady(true);
        newOwner.setGame(GameToExit);

        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(10L)).thenReturn(userToExit);
        when(userRepository.findByUserId(11L)).thenReturn(newOwner);
        gameService.userExitGame(userToExit.getUserId());

        assertEquals(GameToExit.getRealPlayersNumber(), 3);
        assertEquals(GameToExit.getPlayers(), List.of(11L, 12L, 13L));
        assertEquals(GameToExit.getOwnerId(), 11L);
        // assertEquals(userToExit.isReady(), false);
        // assertEquals(newOwner.isReady(), false);
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
        // userToExit.setReady(true);
        
        when(gameRepository.findBygameId(100L)).thenReturn(GameToExit);
        when(userRepository.findByUserId(10L)).thenReturn(userToExit);
        gameService.userExitGame(userToExit.getUserId());
        
        assertEquals(userToExit.getGame(), null);
        // assertEquals(userToExit.isReady(), false);
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
        // Arrange
        Long gameId = 100L;
        Long ownerId = 1L; // The owner of the game
        Long userId = 2L; // The non-owner player whose status will be toggled

        Game mockGame = mock(Game.class);

        when(mockGame.getGameId()).thenReturn(gameId);
        when(mockGame.getOwnerId()).thenReturn(ownerId); 
        when(mockGame.getPlayers()).thenReturn(new ArrayList<>(Arrays.asList(ownerId, userId)));

        when(mockGame.getReadyStatus(userId)).thenReturn(false);

        when(mockGame.switchReadyStatus(eq(userId), eq(false))).thenReturn(true); // Return true to simulate status flip
        given(gameRepository.findBygameId(gameId)).willReturn(mockGame);
        // doNothing().when(gameRepository).save(any(Game.class));
        // doNothing().when(gameRepository).flush();
        // doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        gameService.toggleReadyStatus(gameId, userId);


       verify(gameRepository, atLeastOnce()).findBygameId(gameId); // Called twice as per execution flow
        verify(mockGame, atLeastOnce()).getPlayers();
        verify(mockGame, atLeastOnce()).getOwnerId();
        verify(mockGame, atLeastOnce()).getReadyStatus(userId);
        verify(mockGame, atLeastOnce()).switchReadyStatus(eq(userId), eq(false));
        verify(gameRepository, atLeastOnce()).save(mockGame);
        verify(gameRepository, atLeastOnce()).flush();

        verify(gameService, atLeastOnce()).broadcastReadyStatus(gameId);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/ready/" + gameId + "/status"), anyMap());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/ready/" + gameId + "/canStart"), eq(true));
    }

    @Test
    void toggleReadyStatus_userNotInGame_throwsBadRequest() {
        // Arrange
        Long gameId = 100L;
        Long userId = 10L;
        Long playerInGameId = 11L;

        Game mockGame = mock(Game.class);
        when(mockGame.getGameId()).thenReturn(gameId);

        when(mockGame.getPlayers()).thenReturn(new ArrayList<>(Arrays.asList(playerInGameId)));

        given(gameRepository.findBygameId(gameId)).willReturn(mockGame);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.toggleReadyStatus(gameId, userId);
        }, "Should throw ResponseStatusException for user not in game");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "Status should be BAD_REQUEST");
        assertTrue(exception.getReason().contains("Invalid game or user"), "Reason should indicate invalid game or user");

        verify(gameRepository, times(1)).findBygameId(gameId);
        verify(mockGame, never()).getOwnerId();
        verify(mockGame, never()).getReadyStatus(any(Long.class));
        verify(gameRepository, never()).save(any(Game.class));
        verify(gameRepository, never()).flush();
    }

    @Test
    void toggleReadyStatus_ownerCannotReady_throwsBadRequest() {
        // Arrange
        Long gameId = 100L;
        Long ownerId = 1L; // This user will be the owner

        Game mockGame = mock(Game.class);

        when(mockGame.getGameId()).thenReturn(gameId);
        when(mockGame.getOwnerId()).thenReturn(ownerId); // mockGame's owner is userId
        when(mockGame.getPlayers()).thenReturn(new ArrayList<>(Arrays.asList(ownerId, 3L, 4L))); // Owner is in players

        given(gameRepository.findBygameId(gameId)).willReturn(mockGame);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.toggleReadyStatus(gameId, ownerId);
        }, "Should throw ResponseStatusException because the owner cannot toggle ready.");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "Status should be BAD_REQUEST.");
        assertTrue(exception.getReason().contains("Owner cannot toggle ready"), "Reason should indicate owner cannot toggle ready.");

        verify(gameRepository).findBygameId(gameId);

        verify(mockGame).getPlayers();

        verify(mockGame).getOwnerId();

        verify(mockGame, never()).getReadyStatus(any(Long.class));
        verify(mockGame, never()).switchReadyStatus(any(Long.class), any(Boolean.class));
        verify(gameRepository, never()).save(any(Game.class));
        verify(gameRepository, never()).flush();
        verify(gameService, never()).broadcastReadyStatus(any(Long.class));
    }
    
    @Test
    void checkAllReady_allPlayersReady_returnsTrue() {
        Long gameId = 1L;
        Long ownerId = 100L;
        
        User owner = new User();
        owner.setUserId(ownerId);
        
        User player1 = new User();
        player1.setUserId(101L);
        // player1.setReady(true);
        
        User player2 = new User();
        player2.setUserId(102L);
        // player2.setReady(true);
        
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

        User player2 = new User();
        player2.setUserId(102L);

        Game game = new Game();
        game.setGameId(gameId);
        game.setOwnerId(ownerId);

        Map<Long, Boolean> readyStatusMap = new HashMap<>();
        readyStatusMap.put(ownerId, true);
        readyStatusMap.put(player1.getUserId(), true);
        readyStatusMap.put(player2.getUserId(), false); // This player is NOT ready

        ReflectionTestUtils.setField(game, "readyMap", readyStatusMap);

        game.setPlayers(List.of(ownerId, player1.getUserId(), player2.getUserId()));

        given(gameRepository.findBygameId(gameId)).willReturn(game);

        List<User> players = List.of(owner, player1, player2);
        doReturn(players).when(gameService).getGamePlayers(gameId);


        boolean result = gameService.checkAllReady(gameId);

        assertFalse(result, "checkAllReady should return false when some players are not ready.");

        // verify(gameRepository, times(1)).findBygameId(gameId);
        // verify(gameService, times(1)).getGamePlayers(gameId);
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
        // assertFalse(mockUser.isReady());
        verify(userRepository).save(mockUser);
        verify(userRepository).flush();
        verify(gameRepository).deleteByGameId(mockGame.getGameId());
    }

    //TODO: Verify Trasnfer ownership functionality
    @Test
    void giveupGame_ownerMultiplePlayers_transfersOwnership() {
        // Arrange
        Long userId = 1L;
        Long newOwnerId = 2L;
        Long gameId = 3L;

        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);

        // Ensure userRepository.save passes through the real user objects
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        gameService.giveupGame(userId);

        // Assert direct state changes on the real testGameCombat object
        // assertEquals(1, testGameCombat.getRealPlayersNumber());
        // assertTrue(testGameCombat.getPlayers().contains(newOwnerId));
        // assertEquals(newOwnerId, testGameCombat.getOwnerId());
        // assertFalse(testGameCombat.getReadyMap().containsKey(userId));
        // assertTrue(testGameCombat.getReadyMap().containsKey(newOwnerId));
        // assertEquals(1, testGameCombat.getReadyMap().size());
        // assertEquals(-1, testGameCombat.getScore(userId)); 

        // // Verify interactions with mock dependencies (repositories, messaging)
        // verify(owner, times(1)).setGame(null); 
        // verify(userRepository, times(1)).save(owner);
        // verify(userRepository, times(1)).flush();
        // verify(gameRepository, times(1)).save(testGameCombat);
        // verify(gameRepository, times(1)).flush();
        // verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId + "/owner"), eq(newOwnerId));
        // verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId + "/scoreBoard"), any(Map.class));
        // verify(utilService, times(1)).removeExitPlayer(eq(gameId), eq(userId));
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
    public void startExerciseGame_validInput_gameStartedSuccessfully() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Exercise Game");
        inputGame.setModeType("exercise");
        inputGame.setTime(5);
        inputGame.setPlayersNumber(1);
        
        // Mock owner
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findBygameName("Exercise Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game savedGame = invocation.getArgument(0);
            savedGame.setGameId(1L);
            return savedGame;
        });
        
        // Act
        gameService.startExerciseGame(inputGame);
        
        // Assert
        verify(gameRepository, atLeastOnce()).save(any(Game.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/startExercise/1/gameId"), eq(1L));
        verify(messagingTemplate).convertAndSend(eq("/topic/start/1/ready-time"), eq(5));
    }
    
    @Test
    public void startExerciseGame_invalidMode_throwsException() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Invalid Mode Game");
        inputGame.setModeType("123");
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> gameService.startExerciseGame(inputGame));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid mode type"));
    }
    
    @Test
    public void startExerciseGame_lockNotAcquired_logsWarningAndReturns() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Locked Game");
        inputGame.setModeType("exercise");
        
        ReentrantLock lock = mock(ReentrantLock.class);
        when(lock.tryLock()).thenReturn(false);
        ReflectionTestUtils.setField(gameService, "userLocks", new ConcurrentHashMap<>(Map.of(1L, lock)));
        
        // Act
        gameService.startExerciseGame(inputGame);
        
        // Assert
        verify(gameRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }
    
    @Test
    void startExerciseGame_threadInterrupted_sendsTimerInterruptedMessage() {
        // Arrange
        Game inputGame = new Game();
        inputGame.setOwnerId(1L);
        inputGame.setGameName("Interrupted Exercise Game");
        inputGame.setModeType("exercise");
        inputGame.setTime(5);
        inputGame.setPlayersNumber(1);
        
        when(userRepository.findByUserId(1L)).thenReturn(owner);
        when(gameRepository.findBygameName("Interrupted Exercise Game")).thenReturn(null);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            game.setGameId(1L);
            return game;
        });
        
        Thread gameThread = new Thread(() -> gameService.startExerciseGame(inputGame));
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
        Map<Long, Boolean> currentReadyMap = testGameCombat.getReadyMap();
        currentReadyMap.put(player2.getUserId(), true); // Set Player 2 to ready!
        doReturn(true).when(gameService).checkAllReady(gameId);
        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);

        // Act
        gameService.startGame(gameId);

        // Assert
        verify(gameService, atLeastOnce()).checkAllReady(gameId);
        assertTrue(testGameCombat.getGameRunning(), "Game running flag should be true after successful start.");
        verify(gameRepository, atLeastOnce()).save(testGameCombat);
        verify(gameRepository, atLeastOnce()).flush();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(matches("/topic/start/3/hints"), any(GameGetDTO.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(matches("/topic/start/3/ready-time"), eq(5));
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

    @Test
    void getHintsOfOneCountry_shouldReturnHints_withoutRefill() {
        Long gameId = 1L;
        Long userId = 1L;
        String difficulty = "easy";

        HintList hintList = mock(HintList.class);
        ConcurrentMap<Long, HintList> hintCache = new ConcurrentHashMap<>();
        hintCache.put(gameId, hintList);
        when(utilService.getHintCache()).thenReturn(hintCache);

        when(hintList.getMaxProgressAcrossUsers()).thenReturn(3);  // below refill threshold
        when(hintList.size()).thenReturn(10);

        Map<Country, List<Map<String, Object>>> expectedHints = new HashMap<>();
        Map<String, Object> hintDataEasy = new HashMap<>();
        hintDataEasy.put("hint", "It's in Europe");
        List<Map<String, Object>> hintListEasy = new ArrayList<>();
        hintListEasy.add(hintDataEasy);
        expectedHints.put(Country.Switzerland, hintListEasy);

        when(utilService.getHintForUser(gameId, userId)).thenReturn(expectedHints);

        ConcurrentMap<Long, AtomicBoolean> refillInProgress =
            (ConcurrentMap<Long, AtomicBoolean>) ReflectionTestUtils.getField(gameService, "refillInProgress");
        refillInProgress.clear();

        Map<Country, List<Map<String, Object>>> hints = gameService.getHintsOfOneCountry(gameId, userId, difficulty);

        assertEquals(expectedHints, hints);
        verify(utilService, never()).addHintForGame(anyLong(), anyString());
    }

    @Test
    void restartCheckReady_allPlayersReady_noExceptionThrown() {
        // Arrange
        Long gameId = 1L;
        when(gameRepository.findBygameId(gameId)).thenReturn(testGame);

        // Act
        assertDoesNotThrow(() -> gameService.restartCheckReady(gameId));

        // Assert
        verify(gameRepository, times(1)).findBygameId(gameId);
    }

    @Test
    void restartCheckReady_somePlayersNotReady_throwsBadRequest() {
        // Arrange
        Long gameId = 3L; // Use testGameCombat where player2 is not ready
        when(gameRepository.findBygameId(gameId)).thenReturn(testGameCombat);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.restartCheckReady(gameId);
        }, "Should throw ResponseStatusException when some players are not ready");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Someone is not ready!", exception.getReason());
        verify(gameRepository, times(1)).findBygameId(gameId);
    }

    @Test
    void restartCheckReady_gameNotFound_throwsNotFound() {
        // Arrange
        Long gameId = 999L;
        when(gameRepository.findBygameId(gameId)).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.restartCheckReady(gameId);
        }, "Should throw ResponseStatusException when game is not found");

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Game not found", exception.getReason());
        verify(gameRepository, times(1)).findBygameId(gameId);
    }

    @Test
    void restartCheckReady_emptyReadyMap_noExceptionThrown() {
        // Arrange
        Long gameId = 2L;
        testGameSolo.setReadyMap(new HashMap<>()); // Empty readyMap
        when(gameRepository.findBygameId(gameId)).thenReturn(testGameSolo);

        // Act
        assertDoesNotThrow(() -> gameService.restartCheckReady(gameId));

        // Assert
        verify(gameRepository, times(1)).findBygameId(gameId);
    }

    @Test
    void getLeaderboard_multipleUsers_returnsSortedList() {
        // Arrange
        User user1 = new User();
        user1.setUserId(1L);
        user1.setUsername("User1");
        user1.setAvatar("avatar1.png");
        user1.setLevel(new BigDecimal("5.50")); // 550 after *100

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("User2");
        user2.setAvatar("avatar2.png");
        user2.setLevel(new BigDecimal("10.00")); // 1000 after *100

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("User3");
        user3.setAvatar("avatar3.png");
        user3.setLevel(new BigDecimal("3.25")); // 325 after *100

        List<User> users = Arrays.asList(user1, user2, user3);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserGetDTO> leaderboard = gameService.getLeaderboard();

        // Assert
        assertEquals(3, leaderboard.size(), "Leaderboard should contain 3 users");
        // 验证按 level 降序排序：user2 (1000), user1 (550), user3 (325)
        assertEquals(2L, leaderboard.get(0).getUserId());
        assertEquals("User2", leaderboard.get(0).getUsername());
        assertEquals("avatar2.png", leaderboard.get(0).getAvatar());
        assertEquals(1000, leaderboard.get(0).getLevel());

        assertEquals(1L, leaderboard.get(1).getUserId());
        assertEquals("User1", leaderboard.get(1).getUsername());
        assertEquals("avatar1.png", leaderboard.get(1).getAvatar());
        assertEquals(550, leaderboard.get(1).getLevel());

        assertEquals(3L, leaderboard.get(2).getUserId());
        assertEquals("User3", leaderboard.get(2).getUsername());
        assertEquals("avatar3.png", leaderboard.get(2).getAvatar());
        assertEquals(325, leaderboard.get(2).getLevel());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getLeaderboard_emptyUserList_returnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<UserGetDTO> leaderboard = gameService.getLeaderboard();

        // Assert
        assertTrue(leaderboard.isEmpty(), "Leaderboard should be empty");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getLeaderboard_singleUser_returnsSingleDTO() {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setUsername("User1");
        user.setAvatar("avatar1.png");
        user.setLevel(new BigDecimal("7.89")); // 789 after *100

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        // Act
        List<UserGetDTO> leaderboard = gameService.getLeaderboard();

        // Assert
        assertEquals(1, leaderboard.size(), "Leaderboard should contain 1 user");
        UserGetDTO dto = leaderboard.get(0);
        assertEquals(1L, dto.getUserId());
        assertEquals("User1", dto.getUsername());
        assertEquals("avatar1.png", dto.getAvatar());
        assertEquals(789, dto.getLevel());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getLeaderboard_usersWithDecimalAndNegativeLevels_handlesCorrectly() {
        // Arrange
        User user1 = new User();
        user1.setUserId(1L);
        user1.setUsername("User1");
        user1.setAvatar("avatar1.png");
        user1.setLevel(new BigDecimal("0.123")); // 12 after *100 (round down)

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("User2");
        user2.setAvatar("avatar2.png");
        user2.setLevel(new BigDecimal("-1.50")); // -150 after *100

        List<User> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserGetDTO> leaderboard = gameService.getLeaderboard();

        // Assert
        assertEquals(2, leaderboard.size(), "Leaderboard should contain 2 users");
        // 验证排序：user1 (12), user2 (-150)
        assertEquals(1L, leaderboard.get(0).getUserId());
        assertEquals(12, leaderboard.get(0).getLevel());
        assertEquals(2L, leaderboard.get(1).getUserId());
        assertEquals(-150, leaderboard.get(1).getLevel());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getLeaderboard_nullUsernameOrAvatar_handlesCorrectly() {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setUsername(null); // 模拟 null username
        user.setAvatar(null); // 模拟 null avatar
        user.setLevel(new BigDecimal("5.00")); // 500 after *100

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        // Act
        List<UserGetDTO> leaderboard = gameService.getLeaderboard();

        // Assert
        assertEquals(1, leaderboard.size(), "Leaderboard should contain 1 user");
        UserGetDTO dto = leaderboard.get(0);
        assertEquals(1L, dto.getUserId());
        assertNull(dto.getUsername(), "Username should be null");
        assertNull(dto.getAvatar(), "Avatar should be null");
        assertEquals(500, dto.getLevel());
        verify(userRepository, times(1)).findAll();
    }


@Test
    void nextQuestion_ExerciseMode_gameExists_returnsGameGetDTO() {
        // Arrange
        Long gameId = 1L;
        Map<Country, List<Map<String, Object>>> hints = new HashMap<>();
        List<Map<String, Object>> hintList = Collections.singletonList(Map.of("hint", "In Europe"));
        hints.put(Country.Switzerland, hintList);

        when(gameRepository.findBygameId(gameId)).thenReturn(testGame);
        // Mock getHintsOfOneCountry（假设在 GameService 中）
        doReturn(hints).when(gameService).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());

        // Act
        GameGetDTO result = gameService.nextQuestion_ExerciseMode(gameId);

        // Assert
        assertNotNull(result, "GameGetDTO should not be null");
        assertEquals(hintList, result.getHints(), "Hints should match");
        assertEquals(Country.Switzerland.ordinal(), result.getAnswer(), "Answer should match Switzerland's ordinal");
        assertEquals(Country.Switzerland, answers.get(testGame.getOwnerId()), "Answers map should contain ownerId and Switzerland");

        verify(gameRepository, times(1)).findBygameId(gameId);
        verify(gameService, times(1)).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());
    }

    @Test
    void nextQuestion_ExerciseMode_emptyHints_throwsException() {
        // Arrange
        Long gameId = 1L;
        Map<Country, List<Map<String, Object>>> emptyHints = new HashMap<>();

        when(gameRepository.findBygameId(gameId)).thenReturn(testGame);
        doReturn(emptyHints).when(gameService).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            gameService.nextQuestion_ExerciseMode(gameId);
        }, "Should throw NoSuchElementException for empty hints");

        verify(gameRepository, times(1)).findBygameId(gameId);
        verify(gameService, times(1)).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());
        assertTrue(answers.isEmpty(), "Answers map should remain empty");
    }

    @Test
    void nextQuestion_ExerciseMode_singleHint_returnsGameGetDTO() {
        // Arrange
        Long gameId = 1L;
        Map<Country, List<Map<String, Object>>> hints = new HashMap<>();
        List<Map<String, Object>> hintList = Collections.singletonList(Map.of("hint", "Capital is Bern"));
        hints.put(Country.Switzerland, hintList);

        when(gameRepository.findBygameId(gameId)).thenReturn(testGame);
        doReturn(hints).when(gameService).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());

        // Act
        GameGetDTO result = gameService.nextQuestion_ExerciseMode(gameId);

        // Assert
        assertNotNull(result, "GameGetDTO should not be null");
        assertEquals(hintList, result.getHints(), "Hints should match");
        assertEquals(Country.Switzerland.ordinal(), result.getAnswer(), "Answer should match Switzerland's ordinal");
        assertEquals(Country.Switzerland, answers.get(testGame.getOwnerId()), "Answers map should contain ownerId and Switzerland");

        verify(gameRepository, times(1)).findBygameId(gameId);
        verify(gameService, times(1)).getHintsOfOneCountry(gameId, testGame.getOwnerId(), testGame.getDifficulty());

    void testGiveupGame_SoloPlayer_GameDeleted() {
        Long userId = 1L;
        Game game = spy(new Game());
        game.setGameId(100L);
        game.setOwnerId(userId);
        game.setRealPlayersNumber(1);
        game.setPlayers(new ArrayList<>(List.of(userId)));
        game.setGameName("SoloGame");
        game.setGameCreationDate(LocalDateTime.now());
        game.setTime(5);
        game.setModeType("solo");
        game.setDifficulty("easy");

        User player = new User();
        player.setUserId(userId);
        player.setGame(game);

        game.getScoreBoard().put(userId, 40);
        game.getCorrectAnswersMap().put(userId, 4);
        game.getTotalQuestionsMap().put(userId, 10);

        doReturn(40).when(game).getScore(userId);
        doReturn(4).when(game).getCorrectAnswers(userId);
        doReturn(10).when(game).getTotalQuestions(userId);

        when(userRepository.findByUserId(userId)).thenReturn(player);

        gameService.giveupGame(userId);

        verify(game, times(2)).updateScore(eq(userId), eq(-1));
        verify(userRepository).save(player);
        verify(userRepository).flush();
        verify(gameRepository).deleteByGameId(eq(100L));
        verify(utilService).removeCacheForGame(eq(100L));
    }

    @Test
    void testGiveupGame_Multiplayer_OwnerQuits() {
        // Given
        Long ownerId = owner.getUserId();        // 1L
        Long newOwnerId = player2.getUserId();   // 2L

        // Spy users to allow mocking getGame()
        owner = spy(owner);
        player2 = spy(player2);

        // Use modifiable list for players to allow removal
        List<Long> modifiablePlayers = new ArrayList<>(Arrays.asList(ownerId, newOwnerId));
        testGameCombat.setPlayers(modifiablePlayers);

        // Mark both players as ready
        Map<Long, Boolean> readyMap = new HashMap<>();
        readyMap.put(ownerId, true);
        readyMap.put(newOwnerId, true);
        testGameCombat.setReadyMap(readyMap);

        // Attach the game to users
        owner.setGame(testGameCombat);
        player2.setGame(testGameCombat);

        // Score and answer tracking
        testGameCombat.getScoreBoard().put(ownerId, 100);
        testGameCombat.getCorrectAnswersMap().put(ownerId, 8);
        testGameCombat.getTotalQuestionsMap().put(ownerId, 10);

        // Mock repository and getters
        when(userRepository.findByUserId(ownerId)).thenReturn(owner);
        when(userRepository.findByUserId(newOwnerId)).thenReturn(player2);
        when(owner.getGame()).thenReturn(testGameCombat);
        when(player2.getGame()).thenReturn(testGameCombat);
        doNothing().when(gameService).broadcastReadyStatus(testGameCombat.getGameId());
        doNothing().when(gameService).getGameLobby();

        // When
        gameService.giveupGame(ownerId);

        // Then
        verify(gameRepository, never()).deleteByGameId(testGameCombat.getGameId());  // game shouldn't be deleted (still 1 player left)
        verify(owner, times(1)).setGameHistory(
            eq(testGameCombat.getGameName()),
            eq(testGameCombat.getScore(ownerId)),
            eq(testGameCombat.getCorrectAnswers(ownerId)),
            eq(testGameCombat.getTotalQuestions(ownerId)),
            eq(testGameCombat.getGameCreationDate()),
            eq(testGameCombat.getTime()),
            eq(testGameCombat.getModeType()),
            eq(testGameCombat.getDifficulty())
        );
        verify(owner, atLeastOnce()).setGame(null);
        verify(userRepository, times(1)).save(owner);
        verify(userRepository, times(1)).flush();

        // Ensure the new owner is assigned and broadcast occurred
        assertEquals(newOwnerId, testGameCombat.getOwnerId());
        verify(messagingTemplate, times(1)).convertAndSend(contains("/owner"), eq(newOwnerId));
        verify(utilService, times(1)).removeExitPlayer(testGameCombat.getGameId(), ownerId);
    }

    @Test
    void testSaveGame_GameNotFoundOrNotRunning() {
        Long gameId = 4L;

        when(gameRepository.findBygameId(gameId)).thenReturn(null);
        gameService.saveGame(gameId);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(utilService);

        Game game = new Game();
        game.setGameId(gameId);
        game.setGameRunning(false);
        when(gameRepository.findBygameId(gameId)).thenReturn(game);
        gameService.saveGame(gameId);
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(utilService);
    }

    @Test
    void testSaveGame_SoloMode() {
        Long gameId = 2L;

        // Mock the Game object (not real)
        Game game = mock(Game.class);

        // Setup mocks for game
        when(game.getGameId()).thenReturn(gameId);
        when(game.getModeType()).thenReturn("solo");
        when(game.getGameRunning()).thenReturn(true);
        when(game.getOwnerId()).thenReturn(1L);
        when(game.getGameCreationDate()).thenReturn(LocalDateTime.now());
        when(game.getTime()).thenReturn(5);
        when(game.getDifficulty()).thenReturn("medium");
        when(game.getScore(1L)).thenReturn(120);
        when(game.getCorrectAnswers(1L)).thenReturn(7);
        when(game.getTotalQuestions(1L)).thenReturn(10);
        when(game.getGameName()).thenReturn("Solo Test Game");

        when(gameRepository.findBygameId(gameId)).thenReturn(game);

        // Mock owner user
        User owner = mock(User.class);
        when(userRepository.findByUserId(1L)).thenReturn(owner);

        // Call method under test
        gameService.saveGame(gameId);

        // Verify interactions
        verify(owner).setGameHistory(eq("Solo Test Game"), eq(120), eq(7), eq(10), any(), eq(5), eq("solo"), eq("medium"));
        verify(owner).setGame(null);
        verify(userRepository).save(owner);

        verify(gameRepository).deleteByGameId(gameId);
        verify(userRepository).flush();
        verify(utilService).removeCacheForGame(gameId);
    }

    @Test
    void testGetHintsOfOneCountry() {
        Long gameId = 1L;
        Long userId = 100L;
        String difficulty = "easy";

        // Prepare a hint list with one hint map
        List<Map<String, Object>> hintsList = new ArrayList<>();
        Map<String, Object> hintDetails = new HashMap<>();
        hintDetails.put("hint", "It's in Europe");
        hintsList.add(hintDetails);

        // Prepare mock hint map with content
        Map<Country, List<Map<String, Object>>> mockHintMap = new HashMap<>();
        mockHintMap.put(Country.Switzerland, hintsList);

        when(utilService.getHintForUser(gameId, userId)).thenReturn(mockHintMap);

        HintList mockHintList = mock(HintList.class);
        ConcurrentMap<Long, HintList> mockHintCache = new ConcurrentHashMap<>();
        mockHintCache.put(gameId, mockHintList);
        when(utilService.getHintCache()).thenReturn(mockHintCache);

        when(mockHintList.size()).thenReturn(20);
        when(mockHintList.getMaxProgressAcrossUsers()).thenReturn(16);

        // Call the actual method
        Map<Country, List<Map<String, Object>>> result = gameService.getHintsOfOneCountry(gameId, userId, difficulty);

        // Assert content matches expected hints, not the same instance
        assertEquals(1, result.size());
        assertTrue(result.containsKey(Country.Switzerland));
        assertEquals("It's in Europe", result.get(Country.Switzerland).get(0).get("hint"));

    }
}