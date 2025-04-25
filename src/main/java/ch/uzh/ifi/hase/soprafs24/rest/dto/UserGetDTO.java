package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.math.BigDecimal;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User.GameQuickSave;

public class UserGetDTO {

  private Long userId;
  private String name;
  private String username;
  private UserStatus status;
  private String token;
  private String avatar;
  private String email;
  private String bio;
  private int level;
  private Map<String, GameQuickSave> gameHistory;
  private Map<Country, Integer> learningTracking;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void setGameHistory(Map<String,GameQuickSave> gameHistory){
    this.gameHistory = gameHistory;
  }

  public Map<String,GameQuickSave> getGameHistory(){
    return gameHistory;
  }

  public void setLearningTracking(Map<Country,Integer> learningTracking){
    this.learningTracking = learningTracking;
  }

  public Map<Country,Integer> getLearningTracking(){
    return learningTracking;
  }
}
