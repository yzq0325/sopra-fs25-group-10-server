package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Map;

public class GameGetDTO {

    private String owner;

    private Long gameId;

    private Map<String, String> scoreBoard;

    private String gameName;

    private String lockType;

    private int playersNumber;

    private int realPlayersNumber;

    private String password;

    public Long getGameId() {
        return gameId;
    }
    
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getPlayersNumber() {
        return playersNumber;
    }
    
    public void setPlayersNumber(int playersNumber) {
        this.playersNumber = playersNumber;
    }     
    
    public String getLockType() {
        return lockType;
    }
    
    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public int getPlayerNumbers() {
        return playersNumber;
    }
    
    public void setPlayerNumbers(int playersNumber) {
        this.playersNumber = playersNumber;
    }     

      public int getRealPlayersNumber() {
        return realPlayersNumber;
    }
    
    public void setRealPlayersNumber(int realPlayersNumber) {
        this.realPlayersNumber = realPlayersNumber;
    }   

    public Map<String, String> getScoreBoard() {
        return scoreBoard;
    }

    public void setScoreBoard(Map<String, String> scoreBoard) {
        this.scoreBoard = scoreBoard;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}

