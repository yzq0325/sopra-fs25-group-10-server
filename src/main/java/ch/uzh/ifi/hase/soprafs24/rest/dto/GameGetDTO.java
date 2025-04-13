package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Map;
import java.time.LocalDateTime;

public class GameGetDTO {

    private String ownerName;

    private Long ownerId;

    private Long gameId;

    private Map<Long, String> scoreBoard;

    private String gameName;

    private String lockType;

    private int playersNumber;

    private int realPlayersNumber;

    private String password;

    private String modeType;

    private LocalDateTime endTime;

    private Integer finalScore;

    private String resultSummary;

    private Integer totalQuestions;

    private Integer correctAnswers;

    private Map<Integer, String> hints;

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

    public Long getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Map<Long, String> getScoreBoard() {
        return scoreBoard;
    }

    public void setScoreBoard(Map<Long, String> scoreBoard) {
        this.scoreBoard = scoreBoard;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public String getModeType() {
        return modeType;
    }
    
    public void setModeType(String modeType) {
        this.modeType = modeType;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getFinalScore() {
        return finalScore;
    }
    
    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }
    
    public String getResultSummary() {
        return resultSummary;
    }
    
    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public Integer getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public void setHints(Map<Integer, String> hints){
        this.hints = hints;
    }
}

