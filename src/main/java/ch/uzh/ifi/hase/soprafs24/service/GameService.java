package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
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
import ch.uzh.ifi.hase.soprafs24.service.UtilService;
import ch.uzh.ifi.hase.soprafs24.constant.Country;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private Map<Country, List<Map<String, Object>>> generatedHints;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    UtilService utilService = new UtilService();

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
      Map<Long, Integer> correctAnswersMap = new HashMap<>();
      Map<Long, Integer> totalQuestionsMap = new HashMap<>();

      checkIfOwnerExists(gameToCreate.getOwnerId());
      checkIfGameHaveSameOwner(gameToCreate.getOwnerId());
      gameCreated.setOwnerId(players.get(0));

      gameCreated.setScoreBoard(scoreBoard);
      gameCreated.setCorrectAnswersMap(correctAnswersMap);
      gameCreated.setTotalQuestionsMap(totalQuestionsMap);
      gameCreated.setPlayers(players);
      gameCreated.setHintsNumber(5);
      checkIfGameNameExists(gameToCreate.getGameName());
      gameCreated.setGameName(gameToCreate.getGameName());
      gameCreated.setTime(gameToCreate.getTime());
      gameCreated.setPlayersNumber(gameToCreate.getPlayersNumber());
      gameCreated.setRealPlayersNumber(1);
      gameCreated.setGameRunning(false);

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
      Game targetGame = gameRepository.findBygameId(gameToBeJoined.getGameId());
      if(targetGame.getGameRunning().equals(false)){
        if(targetGame.getRealPlayersNumber() != targetGame.getPlayersNumber()){
          if(gameToBeJoined.getPassword().equals(targetGame.getPassword())){
            targetGame.addPlayer(userRepository.findByUserId(userId));
            targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber()+1);
            gameRepository.save(targetGame);
            gameRepository.flush();
  
            User targetUser = userRepository.findByUserId(userId);
            targetUser.setGame(gameToBeJoined);
            userRepository.save(targetUser);
            userRepository.flush();
  
            List<User> players = getGamePlayers(gameToBeJoined.getGameId());
            messagingTemplate.convertAndSend("/topic/ready/" + gameToBeJoined.getGameId() + "/players", players);
            log.info("websocket send!");
          }
          else{
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong Password! You can't join the game! Please try again!"); 
          }
        }
        else{
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't join this game because this game is full!");
        }
      }else{
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This game is running! You can't join the game! Please try again!");
      }
      
    }

    public void userExitGame(Long userId){
      Game targetGame = userRepository.findByUserId(userId).getGame();
      if(userId != targetGame.getOwnerId()){
        log.info("gameId: ", targetGame.getGameId());
        targetGame.removePlayer(userRepository.findByUserId(userId));
        targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber()-1);
        gameRepository.save(targetGame);
        gameRepository.flush();

        User targetUser = userRepository.findByUserId(userId);
        targetUser.setGame(null);
        userRepository.save(targetUser);
        userRepository.flush();

        List<User> players = getGamePlayers(targetGame.getGameId());
        messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", players);
        log.info("websocket send!");
      }
      else if(targetGame.getRealPlayersNumber()==1){
        gameRepository.deleteByGameId(targetGame.getGameId());
        gameRepository.flush();

        User targetUser = userRepository.findByUserId(userId);
        targetUser.setGame(null);
        userRepository.save(targetUser);
        userRepository.flush();

        List<User> players = getGamePlayers(targetGame.getGameId());
        messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", players);
        log.info("websocket send!");

      }
      else{
        targetGame.removePlayer(userRepository.findByUserId(userId));
        targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber()-1);
        targetGame.setOwnerId((targetGame.getPlayers()).get(0));
        gameRepository.save(targetGame);
        gameRepository.flush();

        User targetUser = userRepository.findByUserId(userId);
        targetUser.setGame(null);
        userRepository.save(targetUser);
        userRepository.flush();

        List<User> players = getGamePlayers(targetGame.getGameId());
        messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", players);
        log.info("websocket send!");
      }
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

    public List<User> getGamePlayers(Long gameId){
      Game gameJoined = gameRepository.findBygameId(gameId);
      List<Long> allPlayers = gameJoined.getPlayers();

      List<User> players = new ArrayList<>();
      for (Long userId : allPlayers) {
          players.add(userRepository.findByUserId(userId));
      }
      return players;
//
//      List<UserGetDTO> allPlayersDTOs = new ArrayList<>();
//
//      for (Long userId : allPlayers) {
//        allPlayersDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(userRepository.findByUserId(userId)));
//      }
//      return allPlayersDTOs;

    }


    public void startGame(Long gameId){
      Game gameToStart = gameRepository.findBygameId(gameId);

      //set time
      LocalDateTime now = LocalDateTime.now();    
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm");
      gameToStart.setGameCreationDate(now.format(formatter));

      //set scoreBoard
      gameToStart.updateScore(gameToStart.getOwnerId(), 0);
      for (Long userId : gameToStart.getPlayers()) {
        User player = userRepository.findByUserId(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
        }
        gameToStart.updateScore(userId, 0);
      }

      //set correctAnwersMap
      gameToStart.updateCorrectAnswers(gameToStart.getOwnerId(), 0);
      for (Long userId : gameToStart.getPlayers()) {
        User player = userRepository.findByUserId(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
        }
        gameToStart.updateCorrectAnswers(userId, 0);
      }
      //set totalQuestionsMap
      gameToStart.updateTotalQuestions(gameToStart.getOwnerId(), 0);
      for (Long userId : gameToStart.getPlayers()) {
        User player = userRepository.findByUserId(userId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
        }
        gameToStart.updateTotalQuestions(userId, 0);
      }

      gameToStart.setGameRunning(true);

      gameRepository.save(gameToStart);
      gameRepository.flush();

      GameGetDTO gameHintDTO = new GameGetDTO();
      
      gameHintDTO.setHints(generatedHints.values().iterator().next());
      gameHintDTO.setTime(gameToStart.getTime());

      messagingTemplate.convertAndSend("/topic/start/" + gameId + "/hints", gameHintDTO);
        log.info("websocket send!");

    }

    public GameGetDTO processingAnswer(GamePostDTO gamePostDTO, Long userId){
      
      //judge right or wrong and update hints
      Game targetGame = gameRepository.findBygameId(gamePostDTO.getGameId());
      //sum up total questions
      // Map<Long, Integer> totalQuestionsMap = targetGame.getTotalQuestionsMap();
      // if (totalQuestionsMap.containsKey(userId)) {
      //     int totalQuestions = totalQuestionsMap.get(userId);
      //     totalQuestionsMap.put(userId, totalQuestions + 1);
      //     targetGame.setTotalQuestionsMap(totalQuestionsMap); 
      // } else {
      //   totalQuestionsMap.put(userId, 1);
      //   targetGame.setTotalQuestionsMap(totalQuestionsMap); 
      // }
      targetGame.updateTotalQuestions(userId,targetGame.getTotalQuestions(userId)+1);
      
      if(gamePostDTO.getSubmitAnswer() == generatedHints.keySet().iterator().next()){
        //sum up correct answers
        // Map<Long, Integer> currentCorrectAnswersMap = targetGame.getCorrectAnswersMap();
        // if (currentCorrectAnswersMap.containsKey(userId)) {
        //   int currentCorrect = currentCorrectAnswersMap.get(userId);
        //   currentCorrectAnswersMap.put(userId, currentCorrect + 1); 
        //   targetGame.setCorrectAnswersMap(currentCorrectAnswersMap);
        // } else {
        //   currentCorrectAnswersMap.put(userId, 1);
        //   targetGame.setCorrectAnswersMap(currentCorrectAnswersMap);
        // }
        targetGame.updateCorrectAnswers(userId,targetGame.getCorrectAnswers(userId)+1);
        //sum up total score
        // Map<Long, Integer> scoreBoardMap = targetGame.getScoreBoard();
        //   int currentscore = scoreBoardMap.get(userId);
        //   scoreBoardMap.put(userId, currentscore + (100-(gamePostDTO.getHintUsingNumber()-1)*20)); 
        //   targetGame.setScoreBoard(scoreBoardMap);
          targetGame.updateScore(userId, targetGame.getScore(userId)+(100-(gamePostDTO.getHintUsingNumber()-1)*20));
          gameRepository.save(targetGame);
          gameRepository.flush();
          // 

          GameGetDTO gameHintDTO = new GameGetDTO();
          gameHintDTO.setHints(generatedHints.values().iterator().next());
          gameHintDTO.setJudgement(true);

          Map<String, Integer> scoreBoardFront = new HashMap<>();
          for(Long userid: targetGame.getPlayers()){
            String username = (userRepository.findByUserId(userid)).getUsername();
            int score = targetGame.getScore(userid);
            scoreBoardFront.put(username,score);
          }
          messagingTemplate.convertAndSend("/topic/user/" + userId + "/scoreBoard", scoreBoardFront);
          log.info("websocket send!");

          return gameHintDTO;
      }else{
        // Map<Long, Integer> currentCorrectAnswersMap = targetGame.getCorrectAnswersMap();
        // if (!currentCorrectAnswersMap.containsKey(userId)) {
        //   currentCorrectAnswersMap.put(userId, 0);
        //   targetGame.setCorrectAnswersMap(currentCorrectAnswersMap);
        // } 
        gameRepository.save(targetGame);
        gameRepository.flush();

        // 

        GameGetDTO gameHintDTO = new GameGetDTO();
        gameHintDTO.setHints(generatedHints.values().iterator().next()); 
        gameHintDTO.setJudgement(false);
        Map<String, Integer> scoreBoardFront = new HashMap<>();
          for(Long userid: targetGame.getPlayers()){
            String username = (userRepository.findByUserId(userid)).getUsername();
            int score = (targetGame.getScoreBoard()).get(userid);
            scoreBoardFront.put(username,score);
          }
          messagingTemplate.convertAndSend("/topic/user/" + userId + "/scoreBoard", scoreBoardFront);
          log.info("websocket send!");

          return gameHintDTO;
      }

    }

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

    public List<GameGetDTO> getLeaderboard() {
      List<Game> allGames = gameRepository.findAll();
      Map<Long, Integer> userScoreMap = new HashMap<>();
  
      for (Game game : allGames) {
          if (game.getEndTime() == null || game.getScoreBoard() == null) {
              continue;
          }
          for (Map.Entry<Long, Integer> entry : game.getScoreBoard().entrySet()) {
              Long userId = entry.getKey();
              Integer score = entry.getValue();
              userScoreMap.put(userId, userScoreMap.getOrDefault(userId, 0) + score);
          }
      }
  
      List<GameGetDTO> leaderboard = new ArrayList<>();
      for (Map.Entry<Long, Integer> entry : userScoreMap.entrySet()) {
          Long userId = entry.getKey();
          Integer totalScore = entry.getValue();
  
          User user = userRepository.findById(userId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  
          GameGetDTO dto = new GameGetDTO();
          dto.setUserId(userId);
          dto.setUsername(user.getUsername());
          dto.setTotalScore(totalScore);
          leaderboard.add(dto);
      }

      leaderboard.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));
  
      return leaderboard;
    }
    public void giveupGame(Long userId){
      Game targetGame = (userRepository.findByUserId(userId)).getGame();

      if(targetGame.getOwnerId().equals(userId)){
        targetGame.removePlayer(userRepository.findByUserId(userId));
        targetGame.setOwnerId(targetGame.getPlayers().get(0));
        targetGame.updateScore(userId, 0);
      }else{
      targetGame.removePlayer(userRepository.findByUserId(userId));
      targetGame.updateScore(userId, 0);
      }
      targetGame.updateCorrectAnswers(userId, 0);
      targetGame.updateTotalQuestions(userId, 0);

      gameRepository.save(targetGame);
      gameRepository.flush();

      messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", targetGame.getPlayers());
      log.info("websocket send!");
    }

}