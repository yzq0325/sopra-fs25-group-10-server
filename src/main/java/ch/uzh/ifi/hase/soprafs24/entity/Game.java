package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "GAME")
public class Game implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long gameId;

  @Column(nullable = false, unique = true)
  private String gameName;

  @Column(nullable = false, unique = true)
  private String gameCode;

  @Column(nullable = false, unique = true)
  private Long ownerId;

  @ElementCollection
  @CollectionTable(name = "gameplayers", joinColumns = @JoinColumn(name = "gameId"))
  @Column(name = "userId")
  private List<Long> players = new ArrayList<>();
  
  @ElementCollection
  @CollectionTable(name = "scoreboard", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "userId")
  @Column(name = "userscore")
  private Map<Long, Integer> scoreBoard = new HashMap<>();
  
  @Column(nullable = false)
  private int hintsNumber;

  @Column(nullable = false)
  private int playersNumber;

  @Column(nullable = false)
  private int realPlayersNumber;

  @Column(nullable = false)
  private int time;
  
  @Column(nullable = true)
  private LocalDateTime gameCreationDate;

  @Column(nullable = false)
  private boolean gameRunning;

  @Column(nullable = false)
  private String modeType;

  @Column(nullable = true)
  private String password;

  @Column(nullable = true)
  private LocalDateTime endTime;
  
  @Column(nullable = true)
  private Integer finalScore;

  @Column(nullable = false)
  private String difficulty;
  
  @ElementCollection
  @CollectionTable(name = "correct_answers", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "userId")
  @Column(name = "correct")
  private Map<Long, Integer> correctAnswersMap = new HashMap<>();
  
  @ElementCollection
  @CollectionTable(name = "total_questions", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "userId")
  @Column(name = "total")
  private Map<Long, Integer> totalQuestionsMap = new HashMap<>();
  
  @ElementCollection
  @CollectionTable(name = "result_summaries", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "userId")
  @Column(name = "summary")
  private Map<Long, String> resultSummaryMap = new HashMap<>();

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
  
  //add player
  public void addPlayer(User player) {
    players.add(player.getUserId());
    player.setGame(this); 
  }

  //remove player
  public void removePlayer(User player) {
      players.remove(player.getUserId());
      player.setGame(null); 
  }

  //get scoreBoard
  public Map<Long, Integer> getScoreBoard() {
      return scoreBoard;
  }

  public void setScoreBoard(Map<Long, Integer> scoreBoard) {
      this.scoreBoard = scoreBoard;
  }

  // update scoreBoard
  public void updateScore(Long userId, int score) {
      scoreBoard.put(userId, score);
  }

  // get specific user's score
  public Integer getScore(Long userId) {
      return scoreBoard.get(userId);
  }

  // remove specific user's score
  // public Integer removeScore(Long userId) {
  //     return scoreBoard.remove(userId);
  // }

  public int getHintsNumber() {
    return hintsNumber;
  }

  public void setHintsNumber(int hintsNumber) {
    this.hintsNumber = hintsNumber;
  }

  public int getPlayersNumber() {
    return playersNumber;
  }

  public void setPlayersNumber(int playersNumber) {
    this.playersNumber = playersNumber;
  }

  public int getRealPlayersNumber() {
    return realPlayersNumber;
  }

  public void setRealPlayersNumber(int realPlayersNumber) {
    this.realPlayersNumber = realPlayersNumber;
  }
  
  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }
  
  public LocalDateTime getGameCreationDate() {
    return gameCreationDate;
  }

  public void setGameCreationDate(LocalDateTime gameCreationDate) {
    this.gameCreationDate = gameCreationDate;
  }

  public Boolean getGameRunning() {
    return gameRunning;
  }

  public void setGameRunning(boolean gameRunning) {
    this.gameRunning = gameRunning;
  }

  public String getModeType() {
     return modeType;
  }

  public void setModeType(String modeType) {
    this.modeType = modeType;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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

  public Map<Long, String> getResultSummaryMap() {
    return resultSummaryMap;
  }

  public void setResultSummaryMap(Map<Long, String> resultSummaryMap) {
    this.resultSummaryMap = resultSummaryMap;
  }

  public Map<Long, Integer> getTotalQuestionsMap() {
    return totalQuestionsMap;
  }

  public void setTotalQuestionsMap(Map<Long, Integer> totalQuestionsMap) {
    this.totalQuestionsMap = totalQuestionsMap;
  }

  public void updateTotalQuestions(Long userId, int number) {
    totalQuestionsMap.put(userId, number);
  }

  public Integer getTotalQuestions(Long userId) {
      return totalQuestionsMap.get(userId);
  }

  public Map<Long, Integer> getCorrectAnswersMap() {
    return correctAnswersMap;
  }

  public void setCorrectAnswersMap(Map<Long, Integer> correctAnswersMap) {
    this.correctAnswersMap = correctAnswersMap;
  }

  public void updateCorrectAnswers(Long userId, int number) {
    correctAnswersMap.put(userId, number);
  }

  public Integer getCorrectAnswers(Long userId) {
      return correctAnswersMap.get(userId);
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

}