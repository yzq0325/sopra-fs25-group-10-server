package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
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
      return gameService.getAllPlayers(gameId);
  }

  @PutMapping("/start/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void startGame(@PathVariable Long gameId) {
    gameService.startGame(gameId);
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

  @PostMapping("/startsolo")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void startsoloGame(@RequestBody GamePostDTO gamePostDTO){
    Game gameToStart = DTOMapper.INSTANCE.convertGamePostDTOtoGameEntity(gamePostDTO);
    gameService.startSoloGame(gameToStart);
  }

  @PostMapping("/codejoin")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GameGetDTO joinGamebyCode(@RequestBody GamePostDTO gamePostDTO){
    return gameService.joinGamebyCode(gamePostDTO);
  }

  @MessageMapping("/game/{gameId}/ready")
  public void handlePlayerReady(@DestinationVariable Long gameId, @Payload Map<String, Object> payload) {
      Long userId = Long.valueOf(payload.get("userId").toString());
      gameService.toggleReadyStatus(gameId, userId);
  }

  @PostMapping("/startexercise")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void startexerciseGame(@RequestBody GamePostDTO gamePostDTO){
    Game gameToStart = DTOMapper.INSTANCE.convertGamePostDTOtoGameEntity(gamePostDTO);
    gameService.startExerciseGame(gameToStart);
  }

  @PostMapping("/next/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GameGetDTO nextQuestion_ExerciseMode(@PathVariable Long gameId){
    return gameService.nextQuestion_ExerciseMode(gameId);
  }

  @PutMapping("/finishexercise/{gameId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void finishexercisegame (@PathVariable Long gameId){
    gameService.saveGame(gameId);
  }

}