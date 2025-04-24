package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Table(name = "USER")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Embeddable
    public static class GameQuickSave {

        public GameQuickSave() {
        }

        @Column(name = "score", nullable = false)
        private int score;

        @Column(name = "correctanswers", nullable = false)
        private int correctAnswers;

        @Column(name = "totalQuestions", nullable = false)
        private int totalQuestions;

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getCorrectAnswers() {
            return correctAnswers;
        }

        public void setCorrectAnswers(int correctAnswers) {
            this.correctAnswers = correctAnswers;
        }

        public int getTotalQuestions() {
            return totalQuestions;
        }

        public void setTotalQuestions(int totalQuestions) {
            this.totalQuestions = totalQuestions;
        }
    }

    @Id
    @GeneratedValue
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private String password;

    @Column
    private String avatar;

    @Column
    private String email;

    @Column
    private String bio;

    @ManyToOne
    @JoinColumn(name = "gameId", nullable = true)
    private Game game;

    @Column(precision = 8, scale = 1) 
    private BigDecimal level = new BigDecimal("0.0");

    @ElementCollection
    @CollectionTable(name = "userGameHistory", joinColumns = @JoinColumn(name = "userId"))
    @MapKeyColumn(name = "gameName")
    private Map<String, GameQuickSave> gameHistory = new HashMap<>();

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public BigDecimal getLevel() {
        return level;
    }

    public void setLevel(BigDecimal level) {
        this.level = level;
    }

    public void setGameHistory(String gameName, int score, int correct, int total) {
        GameQuickSave gameQuickSave = new GameQuickSave();
        gameQuickSave.setScore(score);
        gameQuickSave.setCorrectAnswers(correct);
        gameQuickSave.setTotalQuestions(total);
        gameHistory.put(gameName, gameQuickSave);
    }

    public Map<String, GameQuickSave> getGameHistory(){
        return gameHistory;
    }

    public int getGameScore(String gameName) {
        return gameHistory.get(gameName).getScore();
    }

    public int getGameCorrectAnswer(String gameName) {
        return gameHistory.get(gameName).getCorrectAnswers();
    }

    public int getGameTotalQuestions(String gameName) {
        return gameHistory.get(gameName).getTotalQuestions();
    }
}
