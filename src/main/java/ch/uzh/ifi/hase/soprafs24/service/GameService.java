package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class GameService {

    
    private final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    @Autowired

    public GameService(
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("userRepository") UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public List<Game> getAllGames() {
      return this.gameRepository.findAll();
    }

    public Game createGame(Game gameToCreate) {
      Game gameCreated = new Game();

      List<Long> players = new ArrayList<>();
      players.add(gameToCreate.getOwnerId());
      Map<Long, Integer> scoreBoard = new HashMap<>();

      checkIfOwnerExists(gameToCreate.getOwnerId());
      checkIfGameHaveSameOwner(gameToCreate.getOwnerId());
      gameCreated.setOwnerId(players.get(0));

      gameCreated.setScoreBoard(scoreBoard);
      gameCreated.setPlayers(players);
      gameCreated.setHintsNumber(5);
      checkIfGameNameExists(gameToCreate.getGameName());
      gameCreated.setGameName(gameToCreate.getGameName());
      gameCreated.setTime(gameToCreate.getTime());
      gameCreated.setPlayersNumber(gameToCreate.getPlayersNumber());
      gameCreated.setRealPlayersNumber(1);

      String mode = gameToCreate.getModeType();
      if (mode == null || (!mode.equals("solo") && !mode.equals("combat"))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode type: must be 'solo' or 'combat'");
      }
      gameCreated.setModeType(mode);

      gameCreated.setPassword(gameToCreate.getPassword());
      gameCreated = gameRepository.save(gameCreated);
      gameRepository.flush();

      User owner = userRepository.findByUserId(gameToCreate.getOwnerId());
      owner.setGame(gameCreated);
      userRepository.save(owner);
      userRepository.flush();

      
      log.debug("Created new Game: {}", gameToCreate);
      return gameCreated;
    }

    public Game getGameByGameId(Long gameId){
      return gameRepository.findBygameId(gameId);
    }

    public void userJoinGame(Game gameToBeJoined, Long userId){
      if((gameRepository.findBygameId(gameToBeJoined.getGameId())).getRealPlayersNumber() == 5){
        if(gameToBeJoined.getPassword().equals((gameRepository.findBygameId(gameToBeJoined.getGameId())).getPassword())){
          (gameRepository.findBygameId(gameToBeJoined.getGameId())).addPlayer(userRepository.findByUserId(userId));
          (gameRepository.findBygameId(gameToBeJoined.getGameId())).setRealPlayersNumber((gameRepository.findBygameId(gameToBeJoined.getGameId())).getRealPlayersNumber()+1);
        }
        else{
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong Password! You can't join the game! Please try again!"); 
        }
      }
      else{
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't join this game because this game is full!");
      }
    }

    public void userExitGame(Game gameToBeExited, Long userId){
      (gameRepository.findBygameId(gameToBeExited.getGameId())).removePlayer(userRepository.findByUserId(userId));
      (gameRepository.findBygameId(gameToBeExited.getGameId())).setRealPlayersNumber((gameRepository.findBygameId(gameToBeExited.getGameId())).getRealPlayersNumber()-1);
      (userRepository.findByUserId(userId)).setGame(null);
    }

    public void checkIfOwnerExists(Long userId){
      User userwithUserId = userRepository.findByUserId(userId);
      if(userwithUserId == null){
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner doesn't exists! Please try an existed ownername");
      }
    }
    public void checkIfGameHaveSameOwner(Long userId){
      Game gameWithSameOwner = gameRepository.findByownerId(userId);
      if(gameWithSameOwner != null){
        throw new ResponseStatusException(HttpStatus.CONFLICT, "This owner have already create a game! Please use another one!");
      }
    }
    public void checkIfGameNameExists(String gameName){
      Game gameWithSameName = gameRepository.findBygameName(gameName);
      if(gameWithSameName != null){
        throw new ResponseStatusException(HttpStatus.CONFLICT, "GameName exists! Please try a new one!");
      }
    }

    public List<UserGetDTO> getGamePlayers(Long gameId){
      Game gameJoined = gameRepository.findBygameId(gameId);
      List<Long> allPlayers = gameJoined.getPlayers();

      List<UserGetDTO> allPlayersDTOs = new ArrayList<>();

      for (Long userId : allPlayers) {
        allPlayersDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(userRepository.findByUserId(userId)));
      }
      return allPlayersDTOs;

    }


    public void startGame(Long gameId){
      Game gameToStart = gameRepository.findBygameId(gameId);

      //set time
      LocalDateTime now = LocalDateTime.now();    
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm");
      gameToStart.setGameCreationDate(now.format(formatter));

      //set scoreBoard

      (gameToStart.getScoreBoard()).put(gameToStart.getOwnerId(), 0);
      for (Long userId : gameToStart.getPlayers()) {
        User player = userRepository.findByUserId(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
        }
        (gameToStart.getScoreBoard()).put(userId, 0); 
      }

      
    }

    // public void (){

    // }
  
    public void submitScores(Long gameId,Map<Long, Integer> scoreMap, Map<Long, Integer> correctAnswersMap, Map<Long, Integer> totalQuestionsMap) {
      Game game = gameRepository.findBygameId(gameId);

      if (game == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
      }
  
      // update all users
      for (Long userId : scoreMap.keySet()) {
        Integer score = scoreMap.get(userId);
        Integer correct = correctAnswersMap.getOrDefault(userId, 0);
        Integer total = totalQuestionsMap.getOrDefault(userId, 0);
        String summary = correct + " of " + total + " correct";

        game.updateScore(userId, score);
        game.getCorrectAnswersMap().put(userId, correct);
        game.getTotalQuestionsMap().put(userId, total);
        game.getResultSummaryMap().put(userId, summary);

      }

      // end
      if (game.getScoreBoard().size() >= game.getRealPlayersNumber()) {
        game.setEndTime(LocalDateTime.now());
        log.info("Game " + gameId + " ended automatically. All players submitted scores.");
      }

      gameRepository.save(game);
    }

    public List<GameGetDTO> getGamesByUser(Long userId) {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      String username = user.getUsername();
  
      return gameRepository.findByPlayersContaining(username).stream()
          .filter(game -> game.getEndTime() != null)
          .map(game -> {
              GameGetDTO dto = DTOMapper.INSTANCE.convertGameEntityToGameGetDTO(game);
              dto.setCorrectAnswers(game.getCorrectAnswersMap().get(userId));
              dto.setTotalQuestions(game.getTotalQuestionsMap().get(userId));
              dto.setResultSummary(game.getResultSummaryMap().get(userId));
              return dto;
          })
          .collect(Collectors.toList());
    }
}