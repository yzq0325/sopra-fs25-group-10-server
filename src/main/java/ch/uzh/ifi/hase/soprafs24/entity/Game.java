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
  private String owner;

  @ElementCollection
  @CollectionTable(name = "gameplayers", joinColumns = @JoinColumn(name = "gameId"))
  @Column(name = "username")
  private List<String> players = new ArrayList<>();
  
  @ElementCollection
  @CollectionTable(name = "scoreboard", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "username")
  @Column(name = "userscore")
  private Map<String, Integer> scoreBoard = new HashMap<>();
  
  @Column(nullable = false)
  private int hintsNumber;

  @Column(nullable = false)
  private int playersNumber;

  @Column(nullable = false)
  private int realPlayersNumber;

  @Column(nullable = false)
  private int time;
  
  @Column(nullable = true)
  private String gameCreationDate;

  @Column(nullable = false)
  private String modeType;

  @Column(nullable = true)
  private String password;

  @Column(nullable = false)
  private LocalDateTime endTime;
  
  @Column(nullable = true)
  private Integer finalScore;
  
  @ElementCollection
  @CollectionTable(name = "correct_answers", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "username")
  @Column(name = "correct")
  private Map<String, Integer> correctAnswersMap = new HashMap<>();
  
  @ElementCollection
  @CollectionTable(name = "total_questions", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "username")
  @Column(name = "total")
  private Map<String, Integer> totalQuestionsMap = new HashMap<>();
  
  @ElementCollection
  @CollectionTable(name = "result_summaries", joinColumns = @JoinColumn(name = "gameId"))
  @MapKeyColumn(name = "username")
  @Column(name = "summary")
  private Map<String, String> resultSummaryMap = new HashMap<>();

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

  public List<String> getPlayers() {
    return players;
  }

  public void setPlayers(List<String> players) {
    this.players = players;
  }
  
  //add player
  public void addPlayer(User player) {
    players.add(player.getUsername());
    player.setGame(this); 
  }

  //remove player
  public void removePlayer(User player) {
      players.remove(player.getUsername());
      player.setGame(null); 
  }

  //get scoreBoard
  public Map<String, Integer> getScoreBoard() {
      return scoreBoard;
  }

  public void setScoreBoard(Map<String, Integer> scoreBoard) {
      this.scoreBoard = scoreBoard;
  }

  // update scoreBoard
  public void updateScore(String username, int score) {
      scoreBoard.put(username, score);
  }

  // get specific user's score
  public Integer getScore(String username) {
      return scoreBoard.get(username);
  }

  // remove specific user's score
  public Integer removeScore(String username) {
      return scoreBoard.remove(username);
  }

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
  
  public String getGameCreationDate() {
    return gameCreationDate;
  }

  public void setGameCreationDate(String gameCreationDate) {
    this.gameCreationDate = gameCreationDate;
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

  public Map<String, String> getResultSummaryMap() {
    return resultSummaryMap;
  }

  public void setResultSummaryMap(Map<String, String> resultSummaryMap) {
    this.resultSummaryMap = resultSummaryMap;
  }

  public Map<String, Integer> getTotalQuestionsMap() {
    return totalQuestionsMap;
  }

  public void setTotalQuestionsMap(Map<String, Integer> totalQuestionsMap) {
    this.totalQuestionsMap = totalQuestionsMap;
  }

  public Map<String, Integer> getCorrectAnswersMap() {
    return correctAnswersMap;
  }

  public void setCorrectAnswersMap(Map<String, Integer> correctAnswersMap) {
    this.correctAnswersMap = correctAnswersMap;
  }
}