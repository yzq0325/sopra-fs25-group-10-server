package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */

@RestController
public class GameController {

  private final GameService gameService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  GameController(GameService gameService) {
      this.gameService = gameService;
  }

  @PostMapping("/games")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public GameGetDTO createGame(@RequestBody GamePostDTO gamePostDTO) {
      Game gameToCreate = DTOMapper.INSTANCE.convertGamePostDTOtoGameEntity(gamePostDTO);

      Game createdGame = gameService.createGame(gameToCreate);

      return DTOMapper.INSTANCE.convertGameEntityToGameGetDTO(createdGame);
  }

  @PutMapping("/lobby")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void getGameLobby() {
    gameService.getGameLobby();
  }

  @PutMapping("/lobbyIn/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void joinGame(@RequestBody GamePostDTO gamePostDTO, @PathVariable Long userId) {
    Game gameToBeJoined = DTOMapper.INSTANCE.convertGamePostDTOtoGameEntity(gamePostDTO);
  
    gameService.userJoinGame(gameToBeJoined, userId);
  }

  @PutMapping("/lobbyOut/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void exitGame(@PathVariable Long userId) {
    gameService.userExitGame(userId);
  }

  @GetMapping("/ready/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getGamePlayers(@PathVariable Long gameId) {
    List<User> players = gameService.getGamePlayers(gameId);

    List<UserGetDTO> allPlayersDTOs = new ArrayList<>();
    for (User player : players) {
      allPlayersDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(player));
    }

    return allPlayersDTOs;
  }

  @PutMapping("/start/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void startGame(@PathVariable Long gameId) {
    gameService.startGame(gameId);
  }

  @PutMapping("/games/{gameId}/end")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void submitScores(@PathVariable Long gameId, @RequestBody GamePostDTO gamePostDTO) {
      gameService.submitScores(
          gameId,
          gamePostDTO.getScoreMap(),
          gamePostDTO.getCorrectAnswersMap(),
          gamePostDTO.getTotalQuestionsMap()
      );
  }

  @GetMapping("/users/{userId}/history")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<GameGetDTO> getUserGameHistory(@PathVariable Long userId) {
    return gameService.getGamesByUser(userId);
  }

  @GetMapping("/leaderboard")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getLeaderboard() {
    return gameService.getLeaderboard();
  }

  @PutMapping("/submit/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GameGetDTO answerProcessing(@PathVariable Long userId, @RequestBody GamePostDTO gamePostDTO) {
    return gameService.processingAnswer(gamePostDTO,userId);
  }

  @PutMapping("/giveup/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void giveupGame(@PathVariable Long userId){
    gameService.giveupGame(userId);
  }

  @PutMapping("/save/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void saveGame(@PathVariable Long gameId){
      gameService.saveGame(gameId);
  }
    
}