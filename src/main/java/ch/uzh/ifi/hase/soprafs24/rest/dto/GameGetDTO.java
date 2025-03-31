package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Map;

public class GameGetDTO {

    private Map<String, String> scoreBoard;

    public Map<String, String> getScoreBoard() {
        return scoreBoard;
    }

    public void setScoreBoard(Map<String, String> scoreBoard) {
        this.scoreBoard = scoreBoard;
    }
}

