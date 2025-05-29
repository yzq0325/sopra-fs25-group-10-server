package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;

@ExtendWith(MockitoExtension.class)
public class UtilServiceTest {
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UtilService utilService;
    
    private Long gameId = 1L;
    private Long userId = 101L;
    
    @BeforeEach
    public void setup() {
        utilService.removeCacheForGame(gameId);  // clean state
        // utilService is injected by @InjectMocks, no need to instantiate manually
    }
    
    @Test
    void testAddHintForGame() {
        utilService.initHintQueue(gameId, List.of(userId));
        utilService.addHintForGame(gameId, "easy");
        
        // Let it complete async logic, you may mock generateClues to isolate test
        assertTrue(utilService.getHintCache().containsKey(gameId));
    }
    
    @Test
    void testFormatTime() {
        assertEquals("00:30", utilService.formatTime(30));
        assertEquals("01:05", utilService.formatTime(65));
        assertEquals("10:00", utilService.formatTime(600));
    }
    
    @Test
    void testGenerateClues() {
        Map<Country, List<Map<String, Object>>> clues = utilService.generateClues(3, "easy", gameId);
        assertNotNull(clues);
        assertFalse(clues.isEmpty());
    }
    
    @Test
    void testGetFirstHint() {
        utilService.initHintQueue(gameId, List.of(userId));
        Map<Country, List<Map<String, Object>>> dummyClue = utilService.generateClues(3, "easy", gameId);
        utilService.getHintCache().get(gameId).add(dummyClue);
        
        Map<Country, List<Map<String, Object>>> result = utilService.getFirstHint(gameId);
        assertEquals(dummyClue, result);
    }
    
    @Test
    void testGetHintCache() {
        utilService.initHintQueue(gameId, List.of(userId));
        assertNotNull(utilService.getHintCache().get(gameId));
    }

    // @Test
    // void testGetHintForUser_withCacheRefillMechanism_worksCorrectly() throws InterruptedException {
    //     Long gameId = 1L;
    //     Long userId = 101L;
    //     int expectedPlayerNumber = 1; // Expected number of players initialized
        
    //     utilService.initHintQueue(gameId, List.of(userId));
    //     UtilService.HintList hintList = utilService.getHintCache().get(gameId);
        
    //     assertNotNull(hintList, "HintList should be initialized");
    //     assertEquals(expectedPlayerNumber, hintList.getPlayerNumber(), "HintList should have the correct player number");
        
    //     Map<String, Object> clue = new HashMap<>();
    //     clue.put("text", "This country is in South America and has a long coastline.");
    //     clue.put("difficulty", 1);
        
    //     Map<Country, List<Map<String, Object>>> hintToAdd = new HashMap<>();
    //     hintToAdd.put(Country.Brazil, List.of(clue));
        
    //     hintList.add(hintToAdd);
        
    //     Map<Country, List<Map<String, Object>>> returnedHint = utilService.getHintForUser(gameId, userId);
        
    //     assertNotNull(returnedHint, "Hint should be returned successfully");
    //     assertTrue(returnedHint.containsKey(Country.Brazil), "Returned hint should contain Brazil");
        
    //     List<Map<String, Object>> clues = returnedHint.get(Country.Brazil);
    //     assertFalse(clues.isEmpty(), "Hints list for Brazil should not be empty");
    //     assertEquals("This country is in South America and has a long coastline.", clues.get(0).get("text"), "Returned hint text does not match");
    //     assertEquals(1, clues.get(0).get("difficulty"), "Returned hint difficulty does not match");
        
    //     IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
    //         utilService.getHintForUser(gameId, userId);
    //     });
    //     assertTrue(thrown.getMessage().contains("No more hints available"), "Exception message should indicate no more hints");
        
    //     assertEquals(2, hintList.userProgress.get(userId).get(), "User progress should be incremented to 2");
    // }

    
    @Test
    void testInitHintQueue() {
        utilService.initHintQueue(gameId, List.of(userId));
        ConcurrentMap<Long, UtilService.HintList> hintCache = utilService.getHintCache();
        assertTrue(hintCache.containsKey(gameId));
        assertEquals(1, hintCache.get(gameId).getPlayerNumber());
    }
    
    @Test
    void testRefillHintQueue() {
        utilService.initHintQueue(gameId, List.of(userId));
        utilService.refillHintQueue(gameId, "easy");
        
        var hintList = utilService.getHintCache().get(gameId);
        assertTrue(hintList.size() > 0);
    }
    
    @Test
    void testRemoveCacheForGame() {
        utilService.initHintQueue(gameId, List.of(userId));
        utilService.removeCacheForGame(gameId);
        assertFalse(utilService.getHintCache().containsKey(gameId));
    }
    
    @Test
    void testRemoveExitPlayer() {
        utilService.initHintQueue(gameId, List.of(userId));
        utilService.removeExitPlayer(gameId, userId);
        assertFalse(utilService.getHintCache().get(gameId).userProgress.containsKey(userId));
    }
    
    @Test
    void testTimingCounter() throws InterruptedException {
        Game mockGame = new Game();
        mockGame.setGameId(gameId);
        mockGame.setPlayers(List.of(userId));
        
        when(gameRepository.findBygameId(gameId)).thenReturn(mockGame);
        
        Thread timingThread = new Thread(() -> utilService.timingCounter(1, gameId));
        timingThread.start();
        timingThread.join();  // Wait for thread to finish
        
        verify(gameRepository, atLeastOnce()).findBygameId(gameId);
    }
    
    //    @Test
    //    void testGenerateClues_validOutput_returns5Clues() {
    //        Map<Country, List<Map<String, Object>>> result = utilService.generateClues(5);
    //
    //        // one country only
    //        assertEquals(1, result.size(), "Result should contain exactly one country");
    //
    //        Country country = result.keySet().iterator().next();
    //        List<Map<String, Object>> clues = result.get(country);
    //
    //        // the number of clues should be 10
    //        assertEquals(5, clues.size(), "Should generate exactly 10 clues");
    //
    //        // all clues should contain text and difficulty
    //        for (int i = 0; i < clues.size(); i++) {
    //            Map<String, Object> clue = clues.get(i);
    //            assertTrue(clue.containsKey("text"), "Clue should contain 'text'");
    //            assertTrue(clue.containsKey("difficulty"), "Clue should contain 'difficulty'");
    //
    //            assertTrue(clue.get("text") instanceof String, "Clue text should be a string");
    //            assertTrue(clue.get("difficulty") instanceof Integer, "Difficulty should be an integer");
    //
    //            // difficulty should be in range of 1 to 10
    //            int difficulty = (int) clue.get("difficulty");
    //            assertTrue(difficulty >= 1 && difficulty <= 5, "Difficulty should be between 1 and 10");
    //        }
    //
    //        System.out.println("Country: " + country);
    //        clues.forEach(c -> System.out.println("[" + c.get("difficulty") + "] " + c.get("text")));
    //    }
    //
    //    // pressure test
    //    @Test
    //    void testGenerateClues_multipleRuns() {
    //        UtilService utilService = new UtilService(gameRepository, userRepository);
    //
    //        int success = 0;
    //        int totalRuns = 20;
    //
    //        for (int i = 0; i < totalRuns; i++) {
    //            try {
    //                Map<Country, List<Map<String, Object>>> result = utilService.generateClues(5);
    //
    //                assertEquals(1, result.size(), "Should contain exactly one country");
    //                List<Map<String, Object>> clues = result.values().iterator().next();
    //                assertEquals(5, clues.size(), "Should contain exactly 10 clues");
    //
    //                for (Map<String, Object> clue : clues) {
    //                    assertTrue(clue.containsKey("text"), "Missing 'text' field");
    //                    assertTrue(clue.containsKey("difficulty"), "Missing 'difficulty' field");
    //                    assertTrue(clue.get("text") instanceof String);
    //                    assertTrue(clue.get("difficulty") instanceof Integer);
    //                }
    //
    //                System.out.println(success + "successfully generated clues");
    //                System.out.println("Country: " + result.keySet().iterator().next());
    //                clues.forEach(c -> System.out.println("[" + c.get("difficulty") + "] " + c.get("text")));
    //                success++;
    //                Thread.sleep(1000);
    //            } catch (Exception e) {
    //                System.out.println("Run " + (i + 1) + " failed: " + e.getMessage());
    //            }
    //        }
    //
    //        System.out.println("Test passed in " + success + "/" + totalRuns + " runs.");
    //        assertTrue(success == totalRuns, "Too many failures: only " + success + "/" + totalRuns + " passed.");
    //    }
    
    
}
