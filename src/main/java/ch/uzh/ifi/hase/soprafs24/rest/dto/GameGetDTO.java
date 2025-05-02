package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

public class GameGetDTO {

    private String ownerName;

    private Long ownerId;

    private Long userId;

    private String username;

    private Integer totalScore;

    private Long gameId;

    private Map<String, Integer> scoreBoard;

    private String gameName;

    private int time;

    private String lockType;

    private int playersNumber;

    private int realPlayersNumber;

    private String password;

    private String modeType;

    private String gameCode;

    private LocalDateTime endTime;

    private Integer finalScore;

    private String resultSummary;

    private Integer totalQuestions;

    private Integer correctAnswers;

    private List<Map<String, Object>> hints;

    private boolean judgement;

    private boolean gameRunning;

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

    public Map<String, Integer> getScoreBoard() {
        return scoreBoard;
    }

    public void setScoreBoard(Map<String, Integer> scoreBoard) {
        this.scoreBoard = scoreBoard;
    }

    public int getTime() {
        return time;
    }
    
    public void setTime(int time) {
        this.time = time;
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

    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Integer getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
      
    public void setHints(List<Map<String, Object>> hints){
        this.hints = hints;
    }
  
    public List<Map<String, Object>> getHints(){
        return hints;
    }
    public void setJudgement(boolean judgement){
        this.judgement = judgement;
    }
  
    public boolean getJudgement(){
        return judgement;
    }

    public void setGameRunning(boolean gameRunning){
        this.gameRunning = gameRunning;
    }
  
    public boolean getGameRunning(){
        return gameRunning;
    }
}

