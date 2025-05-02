package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import java.util.List;
import java.util.Map;

public class GamePostDTO {

    private Long gameId;

    private String gameName;

    private Long ownerId;

    private List<Long> players;
    
    private int playersNumber;

    private int time;
  
    private String modeType;

    private String lockType;

    private String password;

    private String gameCode;

    private int hintUsingNumber;

    private Country submitAnswer;

    private Map<Long, Integer> scoreMap;

    private Map<Long, Integer> correctAnswersMap;

    private Map<Long, Integer> totalQuestionsMap;

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

    public String getGameCode() {
      return gameCode;
    }
  
    public void setGameCode(String gameCode) {
      this.gameCode = gameCode;
    }

    public Long getOwnerId() {
        return ownerId;
      }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public List<Long> getPlayers() {
        return players;
    }

    public void setPlayers(List<Long> players) {
        this.players = players;
    }

    public int getPlayersNumber() {
        return playersNumber;
      }
    
      public void setPlayersNumber(int playersNumber) {
        this.playersNumber = playersNumber;
      }
      
      public int getTime() {
        return time;
      }
    
      public void setTime(int time) {
        this.time = time;
      }

    
      public String getModeType() {
        return modeType;
      }
    
      public void setModeType(String modeType) {
        this.modeType = modeType;
      }
    
      public String getLockType() {
        return lockType;
      }
    
      public void setLockType(String lockType) {
        this.lockType = lockType;
      }
    
      public String getPassword() {
        return password;
      }
    
      public void setHintUsingNumber(int hintUsingNumber) {
        this.hintUsingNumber = hintUsingNumber;
      }

      public int getHintUsingNumber() {
        return hintUsingNumber;
      }

      public void setSubmitAnswer(Country submitAnswer) {
        this.submitAnswer = submitAnswer;
      }

      public Country getSubmitAnswer() {
        return submitAnswer;
      }
    
      public void setPassword(String password) {
        this.password = password;
      }

      public Map<Long, Integer> getScoreMap() {
        return scoreMap;
      }
    
      public void setScoreMap(Map<Long, Integer> scoreMap) {
        this.scoreMap = scoreMap;
      }
    
      public Map<Long, Integer> getCorrectAnswersMap() {
        return correctAnswersMap;
      }
    
      public void setCorrectAnswersMap(Map<Long, Integer> correctAnswersMap) {
        this.correctAnswersMap = correctAnswersMap;
      }
    
      public Map<Long, Integer> getTotalQuestionsMap() {
        return totalQuestionsMap;
      }
    
      public void setTotalQuestionsMap(Map<Long, Integer> totalQuestionsMap) {
        this.totalQuestionsMap = totalQuestionsMap;
      }
}