package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import ch.uzh.ifi.hase.soprafs24.constant.Country;

public class UtilServiceTest {

    private UtilService utilService;

    private GameRepository gameRepository;
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        utilService = new UtilService(gameRepository, userRepository);
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
