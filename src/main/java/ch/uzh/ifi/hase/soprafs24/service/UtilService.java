package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class UtilService {
    private static final int FILL_SIZE = 4;
    private static final int HINT_NUMBER = 5;
    private static final int CLEANUP_THRESHOLD = 10;
    
    //adding continent order to enable better distribution
    private static final List<String> CONTINENT_ORDER = List.of("Europe", "Asia", "Americas", "Africa", "Oceania");
    private final Map<Long, Integer> gameToContinentIndex = new ConcurrentHashMap<>();
    private final Map<Long, Integer> gameToOceaniaCounter = new ConcurrentHashMap<>();
    
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
        private final int playerNumber;
        private final List<Map<Country, List<Map<String, Object>>>> hintList = Collections.synchronizedList(new ArrayList<>());
        Map<Long /* userId */, AtomicInteger /* progress */> userProgress = new ConcurrentHashMap<>();
        public Object queueMap;
        
        public HintList(int playerNumber) { this.playerNumber = playerNumber; }
        
        public int size() { return hintList.size(); }
        public int getPlayerNumber() { return playerNumber; }
        public void add(Map<Country, List<Map<String, Object>>> hint) { hintList.add(hint); }
        public Map<Country, List<Map<String, Object>>> get(int index) { return hintList.get(index); }
        public int getMinProgressAcrossUsers() { return userProgress.values().stream().mapToInt(AtomicInteger::get).min().orElse(0); }
        public synchronized void removeUsedHints() {
            if (userProgress.size() < playerNumber) { return; }
            
            // the hint all users at least used
            int minIndex = getMinProgressAcrossUsers();
            if (minIndex > 0) {
                for (int i = 0; i < minIndex; i++) {
                    hintList.set(i, null);
                }
            }
        }
        public synchronized void compactHintList() {
            int minIndex = getMinProgressAcrossUsers();
            if (minIndex > CLEANUP_THRESHOLD) {
                List<Map<Country, List<Map<String, Object>>>> newList = new ArrayList<>(hintList.subList(minIndex, hintList.size()));
                hintList.clear();
                hintList.addAll(newList);
                for (AtomicInteger progress : userProgress.values()) {
                    progress.addAndGet(-minIndex);
                }
            }
        }
        public Map<Country, List<Map<String, Object>>> peekFirstHint() {
            synchronized (hintList) {
                if (hintList.isEmpty()) {
                    throw new IllegalStateException("Hint list is empty");
                }
                return hintList.get(0);
            }
        }
    }
    private final ConcurrentMap<Long, HintList> hintCache = new ConcurrentHashMap<>();
    
    public void initHintQueue(Long gameId, List<Long> players) {
        HintList list = new HintList(players.size());
        for (Long userId : players) {
            list.userProgress.put(userId, new AtomicInteger(1));
        }
        hintCache.put(gameId, list);
    }
    
    @Async
    public void refillHintQueue(Long gameId, String difficulty) {
        HintList list = hintCache.get(gameId);
        int attempts = 0;
        while (list.size() < FILL_SIZE && attempts < FILL_SIZE * 2) {
            try {
                Map<Country, List<Map<String, Object>>> newHint = generateClues(HINT_NUMBER, difficulty , gameId);
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
    }
    
    public Map<Country, List<Map<String, Object>>> getFirstHint(Long gameId) {
        HintList list = hintCache.get(gameId);
        if (list == null || list.hintList.isEmpty()) {
            throw new IllegalStateException("Hint list is empty or not initialized");
        }
        
        synchronized (list.hintList) {
            return list.hintList.get(0);
        }
    }
    
    public Map<Country, List<Map<String, Object>>> getHintForUser(Long gameId, Long userId) {
        if (gameId == null || userId == null) { throw new IllegalArgumentException("gameId and userId cannot be null!"); }
        
        HintList list = hintCache.get(gameId);
        if (list == null) { throw new IllegalStateException("Hint queue not initialized for game " + gameId); }
        
        AtomicInteger currentProgress = list.userProgress.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int index = currentProgress.getAndIncrement();
        synchronized (list.hintList) {
            if (index >= list.hintList.size()) { throw new IllegalStateException("No more hints available for game " + gameId); }
            Map<Country, List<Map<String, Object>>> hint = list.hintList.get(index);
            list.removeUsedHints();
            list.compactHintList();
            log.info("cache of game {}: {}", gameId, list.hintList.stream()
            .filter(Objects::nonNull)
            .map(Map::keySet)
            .collect(Collectors.toList()));
            return hint;
        }
    }
    
    @Async
    public void addHintForGame(Long gameId, String difficulty) {
        log.info("Async refill running on thread {} for game {}", Thread.currentThread().getName(), gameId);
        
        Map<Country, List<Map<String, Object>>> newHint = generateClues(HINT_NUMBER, difficulty, gameId);
        if (newHint != null && !newHint.isEmpty()) {
            int playerNumber = hintCache.get(gameId).getPlayerNumber();
            hintCache.computeIfAbsent(gameId, k -> new HintList(playerNumber)).add(newHint);
        }
    }
    
    public void removeExitPlayer(Long gameId, Long userId) {
        HintList list = hintCache.get(gameId);
        list.userProgress.remove(userId);
    }
    
    public void removeCacheForGame(Long gameId) {
        hintCache.remove(gameId);
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
    public Map<Country, List<Map<String, Object>>> generateClues(int clueNumber, String difficulty, Long gameId) {
        // System.out.println("Game Id: " + gameId);

        try {
            // Country[] countries = Country.values();
            Country targetCountry;
            
            //for hint cycle
            int index = gameToContinentIndex.getOrDefault(gameId, 0);
            String continent = CONTINENT_ORDER.get(index);
            
            if (continent.equals("Oceania")) {
                int oceaniaCount = gameToOceaniaCounter.getOrDefault(gameId, 0);
                if (oceaniaCount % 2 == 1) { // skip Oceania this cycle
                    index = (index + 1) % CONTINENT_ORDER.size();
                    continent = CONTINENT_ORDER.get(index);
                }
                gameToOceaniaCounter.put(gameId, oceaniaCount + 1);
            }

            gameToContinentIndex.put(gameId, (index + 1) % CONTINENT_ORDER.size());

            //creates the countries from that continent to take from that
            final String finalContinent = continent;
            List<Country> filteredCountries = Arrays.stream(Country.values())
            .filter(c -> c.getContinent().equals(finalContinent))
            .collect(Collectors.toList());
            
            if(difficulty.equals("easy")) {
                List<String> easyCountryNames = List.of(
                // Europe
                "UnitedKingdom", "Russia", "Germany", "France", "Italy", "Spain", "Netherlands",
                "Austria", "Sweden", "Norway", "Greece", "Finland", "Ukraine", "Hungary",
                "Switzerland", "Poland", "Belgium", "Portugal", "Denmark", "Iceland",
                "Romania", "Czechia",
                // Asia
                "China", "Japan", "SouthKorea", "India", "Thailand", "Singapore",
                "SaudiArabia", "Iran", "Turkey", "Indonesia", "Mongolia", "UnitedArabEmirates",
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
                targetCountry = filteredCountries.get(new Random().nextInt(filteredCountries.size()));
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
    
    private String buildPrompt(Country country, int clueCount) {
        StringBuilder prompt = new StringBuilder();
    
    prompt.append(String.format("""
        You are a playful AI assistant that generates clear, structured, single-sentence geography guessing clues for a game.

        ### Task
        Generate %d clues for the country: **%s**.
        Each clue should help a player guess the country and should become progressively easier.
        ** Any clue should not contain %s**

        - **Clue %d** must:
        - Clearly state the continent that the country is in (e.g., "This country in South America...").
        - Contain **lesser-known or subtle facts** (e.g., rare fauna, climate, unusual terrain, exports, local language quirks, lesser-known cities, geographic trivia).
        - Avoid anything too obvious or globally iconic.
        - **Avoid clues that spell out the country's name** (e.g., "Chinese" alligator, "Peking" chicken) if these actually refer to the country.

        - **Clues 2 to %d**:
        - Gradually reveal more recognizable geographic, cultural, or linguistic information.
        - Still avoid the country's name, capital city, or political and historical references.
        - **Avoid clues that spell out the country's name**.

        - **Clue 1** (the easiest):
        - May mention world-famous cities, landmarks, national dishes, spoken language, or festivals — but must still avoid naming the country directly.
        - Only if this is a **difficult or lesser-known country to guess**, you may include the capital city.
            - Difficult countries include:
            - Countries with small land area or low global visibility (e.g., Eswatini, Djibouti, Bhutan).
            - Countries with few internationally known cities or landmarks.
            - Countries often confused with neighbors or less discussed globally.
        - Do **not** reveal the capital for well-known countries (e.g., France, China, Brazil, Germany, United States).
        - **Avoid clues that spell out the country's name**.

        ### Forbidden Patterns
        Avoid clues like:
        - This country is Japan. ❌
        - Its capital is Tokyo. ❌
        - Mount Fuji is here. ❌ (too iconic)
        - Peking duck is popular here. ❌ (names the country indirectly)

        ### Output Rules
        - Each clue must be a **single or double sentence only**.
        - All clues must be **factually correct and verifiable**. If you are not certain of a fact, do not include it.
        - Do NOT include the country's name or capital city until the easiest clue.
        - Do NOT use quotation marks, markdown, bullets, or any extra characters.
        - Do NOT include explanations or any introductory or concluding remarks.

        ### Output Format
        Return exactly %d clues in the following plain text format:
        """, clueCount, country, country, clueCount, clueCount - 1, clueCount));

        for (int i = 1; i <= clueCount; i++) {
            prompt.append(String.format("%d. Clue: [Hint] - Difficulty: %d%n", i, i));
        }
        
        return prompt.toString();
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