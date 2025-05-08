package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cglib.core.internal.LoadingCache;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class UtilService {
    private static final int FILL_SIZE = 10;
    private static final int HINT_NUMBER = 5;

//    private static final String GEMINI_API_KEY = "AIzaSyDOukvhZmaQlP38T1bdTGGnc5X-TYRr_Gc";
    private static final String GEMINI_API_KEY = "AIzaSyD73dy5dzR3ZgNLB_tfma-tAtPabQjvjhE";
    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(UtilService.class);

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public UtilService(
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("userRepository") UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public static class HintList {
        private final List<Map<Country, List<Map<String, Object>>>> hintList = Collections.synchronizedList(new ArrayList<>());
        Map<Long /* userId */, AtomicInteger /* progress */> userProgress = new ConcurrentHashMap<>();
        public int size() { return hintList.size(); }
        public void add(Map<Country, List<Map<String, Object>>> hint) { hintList.add(hint); }
        public Map<Country, List<Map<String, Object>>> get(int index) { return hintList.get(index); }
        public int getMinProgressAcrossUsers() { return userProgress.values().stream().mapToInt(AtomicInteger::get).min().orElse(0); }
        public synchronized void removeUsedHints() {
            // the hint all users at least used
            int minIndex = getMinProgressAcrossUsers();
            if (minIndex > 0) {
                for (int i = 0; i < minIndex; i++) {
                    hintList.set(i, null);
                }
            }
        }
    }
    private final ConcurrentMap<Long, HintList> hintCache = new ConcurrentHashMap<>();

    @Async
    public void initHintQueue(Long gameId, String difficulty) {
        HintList list = new HintList();

        int attempts = 0;
        while (list.size() < FILL_SIZE && attempts < FILL_SIZE * 2) {
            try {
                Map<Country, List<Map<String, Object>>> newHint = generateClues(HINT_NUMBER, difficulty);
                if (newHint != null && !newHint.isEmpty()) {
                    list.add(newHint);
                }
//                Thread.sleep(1500);
            }
            catch (Exception e) {
                log.error("Failed to generate hint (attempt {}): {}", attempts + 1, e.getMessage());
            }
            attempts++;
        }

        log.info("Cache preloaded with {} hints for game {}", list.size(), gameId);
        hintCache.put(gameId, list);
    }

    public Map<Country, List<Map<String, Object>>> getHintForUser(Long gameId, Long userId) {
        HintList list = hintCache.get(gameId);
        if (list == null) { throw new IllegalStateException("Hint queue not initialized for game " + gameId); }
        AtomicInteger currentProgress = list.userProgress.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int index = currentProgress.getAndIncrement();
        synchronized (list.hintList) {
            if (index >= list.hintList.size()) { throw new IllegalStateException("No more hints available for game " + gameId); }
            Map<Country, List<Map<String, Object>>> hint = list.hintList.get(index);
            list.removeUsedHints();
            return hint;
        }
    }

    @Async
    public void addHintForGame(Long gameId, String difficulty) {
        log.info("Async refill running on thread {} for game {}", Thread.currentThread().getName(), gameId);

        Map<Country, List<Map<String, Object>>> newHint = generateClues(HINT_NUMBER, difficulty);
        if (newHint != null && !newHint.isEmpty()) {
            hintCache.computeIfAbsent(gameId, k -> new HintList()).add(newHint);
        }
    }

    public ConcurrentMap<Long, HintList> getHintCache() {
        return hintCache;
    }

    public String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void timingCounter(int seconds, Long gameId) {
        try {
            if (gameRepository.findBygameId(gameId) == null) { return; }
            while (seconds >= 0) {
                if (gameRepository.findBygameId(gameId).getPlayers().isEmpty()) { return; }
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/formatted-time", formatTime(seconds));
                log.info("websocket send rest time: {}", seconds);
                seconds = seconds - 1;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
        messagingTemplate.convertAndSend("/topic/end/"+gameId, "Game End!");
    }

    //<country, clue, difficulty(int)>
    public Map<Country, List<Map<String, Object>>> generateClues(int clueNumber, String difficulty) {
        try {
            Country[] countries = Country.values();
            Country targetCountry;
            if(difficulty.equals("easy")) {
                List<String> easyCountryNames = List.of(
                    // Europe
                    "UnitedKingdom", "Russia", "Germany", "France", "Italy", "Spain", "Netherlands",
                    "Austria", "Sweden", "Norway", "Greece", "Finland", "Ukraine", "Hungary",
                    "Switzerland", "Poland", "Belgium", "Portugal", "Denmark", "Iceland",
                    "Romania", "Czechia",
                    // Asia
                    "China", "Japan", "SouthKorea", "India", "Thailand", "Singapore",
                    "Saudi Arabia", "Iran", "Turkey", "Indonesia", "Mongolia", "UnitedArabEmirates",
                    // Americas
                    "Canada", "UnitedStates", "Mexico", "Brazil", "Chile", "Argentina", "Colombia",
                    // Africa
                    "Egypt", "SouthAfrica", "Ethiopia", "DemocraticRepublicOfTheCongo",
                    "Morocco", "Algeria", "Madagascar",
                    // Oceania
                    "Australia", "NewZealand"
                );
                
                String randomCountryName = easyCountryNames.get(new Random().nextInt(easyCountryNames.size()));
                targetCountry = Country.valueOf(randomCountryName);
            }else{
                targetCountry = countries[new Random().nextInt(countries.length)];
            }
            
            String prompt = buildPrompt(targetCountry, clueNumber);
            String payload = buildPayloadJson(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractClues(response.body(), targetCountry);
            }
            else {
                throw new RuntimeException("LLM API failed: " + response.body());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating clues", e);
        }
    }

    private String buildPrompt(Country country, int clueNumber) {
        return """
                You are an AI that generates **single-sentence** geography quiz clues for a game.
                
                ### **Task**
                Given the country: %s, generate **%d different geography clues** that **do not directly mention the country's name and any information provided below**. IMPORTANT: AVOID GENERATING ANYTHING RELATED TO POLITICS AND HISTORY. 
                Each clue should have a difficulty score from **1 to %d**, increasing progressively.
                
                ### **Strict Output Format**
                Return **exactly** %d clues in the following format, NEVER include any other words or characters!!:
                1. Clue: [Provide a single-sentence clue] - Difficulty: 1
                ...
                %d. Clue: [Provide a single-sentence clue] - Difficulty: %d
                Do not miss any line.
                """.formatted(
                country, clueNumber, clueNumber, clueNumber, clueNumber, clueNumber
        );
    }

    private String buildPayloadJson(String promptText) throws Exception {
        Map<String, Object> textPart = Map.of("text", promptText);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> payload = Map.of("contents", List.of(content));

        return objectMapper.writeValueAsString(payload);
    }

    private Map<Country, List<Map<String, Object>>> extractClues(String responseBody, Country country) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode parts = root
                .path("candidates").get(0)
                .path("content")
                .path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Invalid LLM response format");
        }

        String outputText = parts.get(0).path("text").asText();
        String[] lines = outputText.split("\n");

        List<Map<String, Object>> clueList = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d+\\. Clue: .* - Difficulty: \\d+$")) {
                String clueText = line.replaceAll("^\\d+\\. Clue: (.*) - Difficulty: \\d+$", "$1").trim();
                int difficulty = Integer.parseInt(
                        line.replaceAll("^\\d+\\. Clue: .* - Difficulty: (\\d+)$", "$1").trim()
                );

                Map<String, Object> clueMap = new HashMap<>();
                clueMap.put("text", clueText);
                clueMap.put("difficulty", difficulty);

                clueList.add(clueMap);
            }
        }

        Map<Country, List<Map<String, Object>>> result = new HashMap<>();
        result.put(country, clueList);
        return result;
    }


}