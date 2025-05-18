package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.constant.Country;

public class UtilService_HintListTest {

    private UtilService.HintList hintList;
    private Long user1 = 101L;
    private Long user2 = 102L;


    @BeforeEach
    void setup() {
        hintList = new UtilService.HintList(2);
        hintList.userProgress.put(user1, new AtomicInteger(0));
        hintList.userProgress.put(user2, new AtomicInteger(0));
    }

    private Map<Country, List<Map<String, Object>>> createDummyHint(String text) {
        Map<String, Object> clue = new HashMap<>();
        clue.put("text", text);
        clue.put("difficulty", 1);
        Map<Country, List<Map<String, Object>>> hint = new HashMap<>();
        hint.put(Country.Brazil, List.of(clue));
        return hint;
    }

    @Test
    void testAdd() {
        assertEquals(0, hintList.size());
        Map<Country, List<Map<String, Object>>> hint = createDummyHint("Test Hint");
        hintList.add(hint);
        assertEquals(1, hintList.size());
        assertSame(hint, hintList.get(0));
    }

    @Test
    void testCompactHintList() {
        hintList.add(createDummyHint("Hint 1"));
        hintList.add(createDummyHint("Hint 2"));
        hintList.add(createDummyHint("Hint 3"));
        hintList.add(createDummyHint("Hint 4"));

        final int CLEANUP_THRESHOLD = 2;

        hintList.userProgress.get(user1).set(CLEANUP_THRESHOLD + 1);
        hintList.userProgress.get(user2).set(CLEANUP_THRESHOLD + 2);

        hintList.compactHintList();

        assertEquals(4, hintList.size());

        assertEquals(3, hintList.userProgress.get(user1).get());
        assertEquals(4, hintList.userProgress.get(user2).get());

        assertEquals("Hint 1", hintList.get(0).get(Country.Brazil).get(0).get("text"));
    }

    @Test
    void testGet() {
        Map<Country, List<Map<String, Object>>> hint = createDummyHint("Get Hint");
        hintList.add(hint);
        assertSame(hint, hintList.get(0));
    }

    @Test
    void testGetMinProgressAcrossUsers() {
        assertEquals(0, hintList.getMinProgressAcrossUsers());
        hintList.userProgress.get(user1).set(5);
        hintList.userProgress.get(user2).set(3);
        assertEquals(3, hintList.getMinProgressAcrossUsers());
        hintList.userProgress.get(user1).set(1);
        assertEquals(1, hintList.getMinProgressAcrossUsers());
    }

    @Test
    void testGetPlayerNumber() {
        assertEquals(2, hintList.getPlayerNumber());
    }

    @Test
    void testPeekFirstHint() {
        Map<Country, List<Map<String, Object>>> hint = createDummyHint("First Hint");
        hintList.add(hint);
        assertSame(hint, hintList.peekFirstHint());
    }

    @Test
    void testPeekFirstHint_throwsIfEmpty() {
        Exception ex = assertThrows(IllegalStateException.class, () -> {
            hintList.peekFirstHint();
        });
        assertEquals("Hint list is empty", ex.getMessage());
    }

    @Test
    void testRemoveUsedHints() {
        hintList.add(createDummyHint("Hint 1"));
        hintList.add(createDummyHint("Hint 2"));
        hintList.add(createDummyHint("Hint 3"));

        hintList.userProgress.remove(user2);
        hintList.userProgress.get(user1).set(2);
        hintList.removeUsedHints();

        assertNotNull(hintList.get(0));
        assertNotNull(hintList.get(1));
        assertNotNull(hintList.get(2));

        hintList.userProgress.put(user2, new AtomicInteger(2));
        hintList.removeUsedHints();

        assertNull(hintList.get(0));
        assertNull(hintList.get(1));
        assertNotNull(hintList.get(2));
    }

    @Test
    void testSize() {
        assertEquals(0, hintList.size());
        hintList.add(createDummyHint("Hint"));
        assertEquals(1, hintList.size());
    }
}
