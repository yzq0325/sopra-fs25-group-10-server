package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  @ElementCollection
  @CollectionTable(name = "gameplayers", joinColumns = @JoinColumn(name = "gameid"))
  @Column(name = "username")
  private List<String> players = new ArrayList<>();
  
  @ElementCollection
  @CollectionTable(name = "scoreboard", joinColumns = @JoinColumn(name = "gameid"))
  @MapKeyColumn(name = "username")
  @Column(name = "userscore")
  private Map<String, Integer> scoreBoard = new HashMap<>();
  
  @Column(nullable = false)
  private int hintsNumber;

  @Column(nullable = false)
  private int playersNumber;

  @Column(nullable = false)
  private int time;
  
  @Column(nullable = false)
  private String gameCreationDate;

  //solo or combat mode
  @Column(nullable = false)
  private String modeType;

  //private or public 
  @Column(nullable = false)
  private String lockType;

  @Column(nullable = true)
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

  public String getLockType() {
    return lockType;
  }

  public void setLockType(String lockType) {
    this.lockType = lockType;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}