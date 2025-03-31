package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GamePostDTO {

    private List<String> players;        

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }
}