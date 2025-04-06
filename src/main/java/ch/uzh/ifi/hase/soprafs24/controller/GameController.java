package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
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

  @PostMapping("/lobby")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<GameGetDTO> getGameLobby() {
      List<Game> allGames = gameService.getAllGames();

      List<GameGetDTO> gameLobbyGetDTOs = new ArrayList<>();
      for (Game game : allGames) {
        gameLobbyGetDTOs.add(DTOMapper.INSTANCE.convertGameEntityToGameGetDTO(game));
    }
    
    return gameLobbyGetDTOs;
  }
}