package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
@Transactional
public class UtilService {
    private static final String GEMINI_API_KEY = "AIzaSyDOukvhZmaQlP38T1bdTGGnc5X-TYRr_Gc";

    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    //<country, clue, difficulty(int)>
    public Map<Country, List<Map<String, Object>>> generateClues(int clueNumber) {
        try {
            Country[] countries = Country.values();
            Country targetCountry = countries[new Random().nextInt(countries.length)];
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
            } else {
                throw new RuntimeException("LLM API failed: " + response.body());
            }
        } catch (Exception e) {
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