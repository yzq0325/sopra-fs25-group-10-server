package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ch.uzh.ifi.hase.soprafs24.service.UtilService.HintList;

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

    private Map<Long, Country> answers = new HashMap<>();

    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicBoolean> refillInProgress = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UtilService utilService;

    public GameService(
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("userRepository") UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public Game createGame(Game gameToCreate) {
        if (userRepository.findByUserId(gameToCreate.getOwnerId()).getGame() != null) { return null; }

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
        gameCreated.setGameCode(String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000));
        gameCreated.setTime(gameToCreate.getTime());
        gameCreated.setPlayersNumber(gameToCreate.getPlayersNumber());
        gameCreated.setRealPlayersNumber(1);
        gameCreated.setDifficulty(gameToCreate.getDifficulty());
        gameCreated.setGameRunning(false);

        String mode = gameToCreate.getModeType();
        if (mode == null || (!mode.equals("solo") && !mode.equals("combat") && !mode.equals("exercise"))) {
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

        getGameLobby();
        log.debug("Created new Game: {}", gameToCreate);
        return gameCreated;
    }

    public void getGameLobby() {
        List<Game> allGames = gameRepository.findAll();
        List<GameGetDTO> gameLobbyGetDTOs = new ArrayList<>();
        for (Game game : allGames) {
            if(game.getModeType().equals("combat")){
                gameLobbyGetDTOs.add(DTOMapper.INSTANCE.convertGameEntityToGameGetDTO(game));
            }
        }
        messagingTemplate.convertAndSend("/topic/lobby", gameLobbyGetDTOs);
        log.info("websocket send: lobby!");
    }

    public void userJoinGame(Game gameToBeJoined, Long userId) {
        Game targetGame = gameRepository.findBygameId(gameToBeJoined.getGameId());
        if(targetGame.getPlayers().contains(userId)){return;}
        if (targetGame.getGameRunning().equals(false)) {
            if (targetGame.getRealPlayersNumber() != targetGame.getPlayersNumber()) {
                if (gameToBeJoined.getPassword().equals(targetGame.getPassword())) {
                    targetGame.addPlayer(userRepository.findByUserId(userId));
                    targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber() + 1);
                    gameRepository.save(targetGame);
                    gameRepository.flush();

                    User targetUser = userRepository.findByUserId(userId);
                    targetUser.setGame(gameToBeJoined);
                    userRepository.save(targetUser);
                    userRepository.flush();

                    List<User> players = getGamePlayers(gameToBeJoined.getGameId());
                    messagingTemplate.convertAndSend("/topic/ready/" + gameToBeJoined.getGameId() + "/players", players);
                    log.info("websocket send: players!");
                    getGameLobby();
                }
                else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong Password! You can't join the game! Please try again!");
                }
            }
            else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't join this game because this game is full!");
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This game is running! You can't join the game! Please try again!");
        }
    }

    public GameGetDTO joinGamebyCode(GamePostDTO gamePostDTO){
        Game gametoJoin = gameRepository.findBygameCode(gamePostDTO.getGameCode());
        if (gametoJoin == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game with your gameCode doesn't exist! Please try another gameCode!");
        }
        else{
            return DTOMapper.INSTANCE.convertGameEntityToGameGetDTO(gametoJoin);
        }
    }

    public void userExitGame(Long userId) {
        Game targetGame = userRepository.findByUserId(userId).getGame();
        if (userId != targetGame.getOwnerId()) {
            targetGame.removePlayer(userRepository.findByUserId(userId));
            targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber() - 1);
            gameRepository.save(targetGame);
            gameRepository.flush();

            User targetUser = userRepository.findByUserId(userId);
            targetUser.setGame(null);
            targetUser.setReady(false);
            userRepository.save(targetUser);
            userRepository.flush();

            List<User> players = getGamePlayers(targetGame.getGameId());
            messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", players);
            log.info("websocket send: players!");

            getGameLobby();
        }
        else if (targetGame.getRealPlayersNumber() == 1) {
            gameRepository.deleteByGameId(targetGame.getGameId());

            User targetUser = userRepository.findByUserId(userId);
            targetUser.setGame(null);
            targetUser.setReady(false);
            userRepository.save(targetUser);
            userRepository.flush();

            getGameLobby();
        }
        else {
            targetGame.removePlayer(userRepository.findByUserId(userId));
            targetGame.setRealPlayersNumber(targetGame.getRealPlayersNumber() - 1);
            targetGame.setOwnerId((targetGame.getPlayers()).get(0));
            gameRepository.save(targetGame);
            gameRepository.flush();

            User targetUser = userRepository.findByUserId(userId);
            targetUser.setGame(null);
            targetUser.setReady(false);
            userRepository.save(targetUser);
            userRepository.flush();

            List<User> players = getGamePlayers(targetGame.getGameId());
            messagingTemplate.convertAndSend("/topic/ready/" + targetGame.getGameId() + "/players", players);
            log.info("websocket send: players!");

            getGameLobby();
        }
        utilService.removeCacheForGame(targetGame.getGameId());
    }

    public void checkIfOwnerExists(Long userId) {
        User userwithUserId = userRepository.findByUserId(userId);
        if (userwithUserId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner doesn't exists! Please try an existed ownername");
        }
    }

    public void checkIfGameHaveSameOwner(Long userId) {
        Game gameWithSameOwner = gameRepository.findByownerId(userId);
        if (gameWithSameOwner != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This owner have already create a game! Please use another one!");
        }
    }

    public void checkIfGameNameExists(String gameName) {
        Game gameWithSameName = gameRepository.findBygameName(gameName);
        if (gameWithSameName != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GameName exists! Please try a new one!");
        }
    }

    public void chatChecksForGame(Long gameId, String playerName) {
        System.out.println(gameId);
        System.out.println(playerName);
        Game game = gameRepository.findBygameId(gameId);
        System.out.println(game);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game Not Found!");
        }
    
        User user = userRepository.findByUsername(playerName);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found!");
        }
    
        if (!game.getPlayers().contains(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant in this game.");
        }

        if(game.getEndTime() != null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game endeded, Chat is not available anymore.");
        }
    }

    public List<User> getGamePlayers(Long gameId) {
        Game gameJoined = gameRepository.findBygameId(gameId);
        List<Long> allPlayers = gameJoined.getPlayers();

        List<User> players = new ArrayList<>();
        for (Long userId : allPlayers) {
            players.add(userRepository.findByUserId(userId));
        }
        messagingTemplate.convertAndSend("/topic/"+gameId+"/gametime", utilService.formatTime(gameJoined.getTime()*60));
        messagingTemplate.convertAndSend("/topic/"+gameId+"/gameCode", gameJoined.getGameCode());
        return players;

    }

    public List<UserGetDTO> getAllPlayers(Long gameId){
        List<User> players = getGamePlayers(gameId);

        List<UserGetDTO> allPlayersDTOs = new ArrayList<>();
        for (User player : players) {
        allPlayersDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(player));
        }

        return allPlayersDTOs;
    }

    public boolean checkAllReady(Long gameId) {
        List<User> players = getGamePlayers(gameId);
        Game game = gameRepository.findBygameId(gameId);
        Long ownerId = game.getOwnerId();
    
        return players.stream()
            .filter(p -> !p.getUserId().equals(ownerId))
            .allMatch(User::isReady);
    }

    public void startSoloGame(Game gameToStart){
        Long gameUserId = gameToStart.getOwnerId();
        ReentrantLock lock = userLocks.computeIfAbsent(gameUserId, k -> new ReentrantLock());
        boolean acquired = lock.tryLock();
        if (!acquired) {
            log.warn("Another createGame is already running for user {}", gameUserId);
            return;
        }

        try {
            Game gameCreated = new Game();

            List<Long> players = new ArrayList<>();
            players.add(gameToStart.getOwnerId());
            Map<Long, Integer> scoreBoard = new HashMap<>();
            Map<Long, Integer> correctAnswersMap = new HashMap<>();
            Map<Long, Integer> totalQuestionsMap = new HashMap<>();

            checkIfOwnerExists(gameToStart.getOwnerId());
            checkIfGameHaveSameOwner(gameToStart.getOwnerId());
            gameCreated.setOwnerId(players.get(0));

            gameCreated.setScoreBoard(scoreBoard);
            gameCreated.setCorrectAnswersMap(correctAnswersMap);
            gameCreated.setTotalQuestionsMap(totalQuestionsMap);
            gameCreated.setPlayers(players);
            gameCreated.setHintsNumber(5);
            checkIfGameNameExists(gameToStart.getGameName());
            gameCreated.setGameName(gameToStart.getGameName());
            gameCreated.setGameCode(String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000));
            gameCreated.setTime(gameToStart.getTime());
            gameCreated.setPlayersNumber(gameToStart.getPlayersNumber());
            gameCreated.setRealPlayersNumber(1);
            gameCreated.setDifficulty(gameToStart.getDifficulty());
            gameCreated.setGameRunning(false);
            LocalDateTime now = LocalDateTime.now();
            gameCreated.setGameCreationDate(now);
            
            String mode = gameToStart.getModeType();
            if (mode == null || (!mode.equals("solo") && !mode.equals("combat") && !mode.equals("exercise"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode type: must be 'solo' or 'combat'");
            }
            gameCreated.setModeType(mode);

            gameCreated.setPassword(gameToStart.getPassword());
            gameCreated = gameRepository.save(gameCreated);
            gameRepository.flush();

            User owner = userRepository.findByUserId(gameToStart.getOwnerId());
            owner.setGame(gameCreated);
            userRepository.save(owner);
            userRepository.flush();

            messagingTemplate.convertAndSend("/topic/startsolo/" + gameCreated.getOwnerId() + "/gameId", gameCreated.getGameId());
            log.info("websocket send: gameId!");

            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                messagingTemplate.convertAndSend("/topic/game/" + gameCreated.getGameId() + "/timer-interrupted", "TIMER_STOPPED");
            }

            gameCreated.updateScore(gameCreated.getOwnerId(), 0);
            for (Long userId : gameCreated.getPlayers()) {
                User player = userRepository.findByUserId(userId);
                if (player == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
                }
                gameCreated.updateScore(userId, 0);
            }

            //set correctAnswersMap
            gameCreated.updateCorrectAnswers(gameCreated.getOwnerId(), 0);
            for (Long userId : gameCreated.getPlayers()) {
                User player = userRepository.findByUserId(userId);
                if (player == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
                }
                gameCreated.updateCorrectAnswers(userId, 0);
            }

            //set totalQuestionsMap
            gameCreated.updateTotalQuestions(gameCreated.getOwnerId(), 0);
            for (Long userId : gameCreated.getPlayers()) {
                User player = userRepository.findByUserId(userId);
                if (player == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
                }
                gameCreated.updateTotalQuestions(userId, 0);
            }

            gameCreated.setGameRunning(true);

            gameCreated = gameRepository.save(gameCreated);
            gameRepository.flush();

            utilService.initHintQueue(gameCreated.getGameId(), gameCreated.getPlayers());
            utilService.refillHintQueue(gameCreated.getGameId(), gameCreated.getDifficulty());

            messagingTemplate.convertAndSend("/topic/start/" + gameCreated.getGameId() + "/ready-time", 5);
            try {
                Thread.sleep(6000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                messagingTemplate.convertAndSend("/topic/game/" + gameCreated.getGameId() + "/timer-interrupted", "TIMER_STOPPED");
            }

            // push hints
            GameGetDTO gameHintDTO = new GameGetDTO();
            generatedHints = getHintsOfOneCountry(gameCreated.getGameId(), gameCreated.getOwnerId(), gameCreated.getDifficulty());
            gameHintDTO.setHints(generatedHints.values().iterator().next());
            Country country = generatedHints.keySet().iterator().next();
            gameHintDTO.setAnswer(country.ordinal());

            // set sheet
            for (Long userId : players) {
                answers.put(userId, country);
            }

            //set scoreboard
            Map<String, Integer> scoreBoardFront = new HashMap<>();
            for (Long userid : gameCreated.getPlayers()) {
                String username = (userRepository.findByUserId(userid)).getUsername();
                int score = gameCreated.getScore(userid);
                scoreBoardFront.put(username, score);
            }
            gameHintDTO.setScoreBoard(scoreBoardFront);
            gameHintDTO.setTime(gameCreated.getTime());
            messagingTemplate.convertAndSend("/topic/start/" + gameCreated.getGameId() + "/hints", gameHintDTO);
            log.info("websocket send: hints!");

            // countdown
            // utilService.countdown(gameId, gameToStart.getTime());
            Game finalGameToStart = gameCreated;
            if(finalGameToStart.getTime()==-1){return;}
            Thread timingThread = new Thread(() -> utilService.timingCounter((finalGameToStart.getTime()) * 60, finalGameToStart.getGameId()));
            timingThread.start();
        } finally {
            lock.unlock();
            userLocks.remove(gameUserId);
        }
    }

    public void startExerciseGame(Game gameToStart){
        Long gameUserId = gameToStart.getOwnerId();
        ReentrantLock lock = userLocks.computeIfAbsent(gameUserId, k -> new ReentrantLock());
        boolean acquired = lock.tryLock();
        if (!acquired) {
            log.warn("Another createGame is already running for user {}", gameUserId);
            return;
        }

        try {
            Game gameCreated = new Game();

            List<Long> players = new ArrayList<>();
            players.add(gameToStart.getOwnerId());

            checkIfOwnerExists(gameToStart.getOwnerId());
            checkIfGameHaveSameOwner(gameToStart.getOwnerId());
            gameCreated.setOwnerId(players.get(0));

            gameCreated.setPlayers(players);
            gameCreated.setHintsNumber(5);
            checkIfGameNameExists(gameToStart.getGameName());
            gameCreated.setGameName(gameToStart.getGameName());
            gameCreated.setGameCode(String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000));
            gameCreated.setTime(gameToStart.getTime());
            gameCreated.setPlayersNumber(gameToStart.getPlayersNumber());
            gameCreated.setRealPlayersNumber(1);
            gameCreated.setDifficulty(gameToStart.getDifficulty());
            gameCreated.setGameRunning(false);
            LocalDateTime now = LocalDateTime.now();
            gameCreated.setGameCreationDate(now);

            String mode = gameToStart.getModeType();
            if (mode == null || (!mode.equals("solo") && !mode.equals("combat") && !mode.equals("exercise"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode type: must be 'solo' or 'combat'");
            }
            gameCreated.setModeType(mode);

            gameCreated.setPassword(gameToStart.getPassword());
            gameCreated = gameRepository.save(gameCreated);
            gameRepository.flush();

            User owner = userRepository.findByUserId(gameToStart.getOwnerId());
            owner.setGame(gameCreated);
            userRepository.save(owner);
            userRepository.flush();

            messagingTemplate.convertAndSend("/topic/startExercise/" + gameCreated.getOwnerId() + "/gameId", gameCreated.getGameId());
            log.info("websocket send: gameId!");

            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                messagingTemplate.convertAndSend("/topic/game/" + gameCreated.getGameId() + "/timer-interrupted", "TIMER_STOPPED");
            }
            
            gameCreated.setGameRunning(true);

            gameCreated = gameRepository.save(gameCreated);
            gameRepository.flush();

            utilService.initHintQueue(gameCreated.getGameId(), gameCreated.getPlayers());
            utilService.refillHintQueue(gameCreated.getGameId(), gameCreated.getDifficulty());

            // countdown
            messagingTemplate.convertAndSend("/topic/start/" + gameCreated.getGameId() + "/ready-time", 5);
            try {
                Thread.sleep(6000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                messagingTemplate.convertAndSend("/topic/game/" + gameCreated.getGameId() + "/timer-interrupted", "TIMER_STOPPED");
            }

            // push hints
            GameGetDTO gameHintDTO = new GameGetDTO();
            generatedHints = getHintsOfOneCountry(gameCreated.getGameId(), gameCreated.getOwnerId(), gameCreated.getDifficulty());
            gameHintDTO.setHints(generatedHints.values().iterator().next());
            Country country = generatedHints.keySet().iterator().next();
            gameHintDTO.setAnswer(country.ordinal());

            // set sheet
            for (Long userId : players) {
                answers.put(userId, country);
            }

            gameHintDTO.setTime(gameCreated.getTime());
            gameHintDTO.setModeType(gameCreated.getModeType());
            messagingTemplate.convertAndSend("/topic/start/" + gameCreated.getGameId() + "/hints", gameHintDTO);
            log.info("websocket send: hints!");
        } finally {
            lock.unlock();
            userLocks.remove(gameUserId);
        }
    }

    public GameGetDTO nextQuestion_ExerciseMode(Long gameId) {
        Game targetGame = gameRepository.findBygameId(gameId);
        GameGetDTO gameHintDTO = new GameGetDTO();
        generatedHints = getHintsOfOneCountry(gameId, targetGame.getOwnerId(), targetGame.getDifficulty());
        gameHintDTO.setHints(generatedHints.values().iterator().next());
        answers.put(targetGame.getOwnerId(), generatedHints.keySet().iterator().next());
        gameHintDTO.setAnswer(generatedHints.keySet().iterator().next().ordinal());

        return gameHintDTO;
    }

    public void toggleReadyStatus(Long gameId, Long userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null || user.getGame() == null || !user.getGame().getGameId().equals(gameId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user or game");
        }
    
        Game game = user.getGame();
        if (user.getUserId().equals(game.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot toggle ready");
        }
    
        user.setReady(!user.isReady());
        userRepository.save(user);
        userRepository.flush();
    
        List<User> players = getGamePlayers(gameId);
    
        Map<Long, Boolean> readinessMap = players.stream()
                .collect(Collectors.toMap(User::getUserId, User::isReady));
        messagingTemplate.convertAndSend("/topic/ready/" + gameId + "/status", readinessMap);
    
        boolean allReady = players.stream()
                .filter(p -> !p.getUserId().equals(game.getOwnerId()))
                .allMatch(User::isReady);
        messagingTemplate.convertAndSend("/topic/ready/" + gameId + "/canStart", allReady);
    }

    public Map<Country, List<Map<String, Object>>> getHintsOfOneCountry(Long gameId, Long userId, String difficulty) {
        if(difficulty == null || difficulty.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,  " Difficulty is not set!");
        }
        log.info("size of hintCache with game {}: {}", gameId, utilService.getHintCache().get(gameId).size());
        Map<Country, List<Map<String, Object>>> hint = utilService.getHintForUser(gameId, userId);

        HintList list = utilService.getHintCache().get(gameId);
        synchronized (list) {
            int minProgress = list.getMinProgressAcrossUsers();
            int remainingHints = list.size() - minProgress;

            if (remainingHints < 10) {
                AtomicBoolean flag = refillInProgress.computeIfAbsent(gameId, id -> new AtomicBoolean(false));
                if (flag.compareAndSet(false, true)) {
                    for (int i = 0; i < 10 - remainingHints; ++i) {
                        utilService.addHintForGame(gameId, difficulty);
                    }
                    flag.set(false);
                }
            }
        }
        return hint;
    }

    public void startGame(Long gameId) {
        Game gameToStart = gameRepository.findBygameId(gameId);

        if (gameToStart == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game Not Found");
        }

        if (!checkAllReady(gameId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all players are ready");
        }

        List<Long> allPlayers = gameToStart.getPlayers();

        //set time
        LocalDateTime now = LocalDateTime.now();
        gameToStart.setGameCreationDate(now);

        //set scoreBoard
        gameToStart.updateScore(gameToStart.getOwnerId(), 0);
        for (Long userId : gameToStart.getPlayers()) {
            User player = userRepository.findByUserId(userId);
            if (player == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, (userRepository.findByUserId(userId)).getUsername() + " is not found");
            }
            gameToStart.updateScore(userId, 0);
        }

        //set correctAnswersMap
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

        gameToStart = gameRepository.save(gameToStart);
        gameRepository.flush();

        utilService.initHintQueue(gameToStart.getGameId(), gameToStart.getPlayers());
        utilService.refillHintQueue(gameToStart.getGameId(), gameToStart.getDifficulty());

        // countdown
        messagingTemplate.convertAndSend("/topic/start/" + gameId + "/ready-time", 5);
        try {
            Thread.sleep(6000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/timer-interrupted", "TIMER_STOPPED");
        }

        // push hints
        GameGetDTO gameHintDTO = new GameGetDTO();
        generatedHints = utilService.getFirstHint(gameId);
        gameHintDTO.setHints(generatedHints.values().iterator().next());
        Country country = generatedHints.keySet().iterator().next();
        gameHintDTO.setAnswer(country.ordinal());

        // set sheet
        for (Long userId : allPlayers) {
            answers.put(userId, country);
        }
        
        //set scoreboard
        Map<String, Integer> scoreBoardFront = new HashMap<>();
        for (Long userid : gameToStart.getPlayers()) {
            String username = (userRepository.findByUserId(userid)).getUsername();
            int score = gameToStart.getScore(userid);
            scoreBoardFront.put(username, score);
        }
        gameHintDTO.setScoreBoard(scoreBoardFront);
        messagingTemplate.convertAndSend("/topic/start/" + gameId + "/hints", gameHintDTO);
        log.info("websocket send: hints!");

        Game finalGameToStart = gameToStart;
        Thread timingThread = new Thread(() -> utilService.timingCounter((finalGameToStart.getTime()) * 60, gameId));
        timingThread.start();

        // reset ready status
        for (Long userId : gameToStart.getPlayers()) {
            User player = userRepository.findByUserId(userId);
            player.setReady(false);
            userRepository.save(player);
        }
        userRepository.flush();
    }

    public GameGetDTO processingAnswer(GamePostDTO gamePostDTO, Long userId) {

        //judge right or wrong and update hints
        Game targetGame = gameRepository.findBygameId(gamePostDTO.getGameId());

        if(targetGame.getModeType().equals("exercise")){
            if (gamePostDTO.getSubmitAnswer() == answers.get(userId)) {
                gameRepository.save(targetGame);
                gameRepository.flush();

                GameGetDTO gameHintDTO = new GameGetDTO();
                gameHintDTO.setJudgement(true);

                return gameHintDTO;
            }
            else{
                gameRepository.save(targetGame);
                gameRepository.flush();

                GameGetDTO gameHintDTO = new GameGetDTO();
                gameHintDTO.setJudgement(false);

                return gameHintDTO;
            }
        }
        else{
            targetGame.updateTotalQuestions(userId, targetGame.getTotalQuestions(userId) + 1);

            if (gamePostDTO.getSubmitAnswer() == answers.get(userId)) {
                targetGame.updateCorrectAnswers(userId, targetGame.getCorrectAnswers(userId) + 1);
                targetGame.updateScore(userId, targetGame.getScore(userId) + (100 - (gamePostDTO.getHintUsingNumber() - 1) * 20));
                gameRepository.save(targetGame);
                gameRepository.flush();

                User targetUser = userRepository.findByUserId(userId);
                if(targetGame.getModeType().equals("combat")){targetUser.updateLearningTrack(answers.get(userId));}
                userRepository.save(targetUser);
                userRepository.flush();

                GameGetDTO gameHintDTO = new GameGetDTO();
                generatedHints = getHintsOfOneCountry(targetGame.getGameId(), userId, targetGame.getDifficulty());
                gameHintDTO.setHints(generatedHints.values().iterator().next());
                answers.put(userId, generatedHints.keySet().iterator().next());
                gameHintDTO.setJudgement(true);
                gameHintDTO.setAnswer(answers.get(userId).ordinal());

                Map<String, Integer> scoreBoardFront = new HashMap<>();
                for (Long userid : targetGame.getPlayers()) {
                    String username = (userRepository.findByUserId(userid)).getUsername();
                    int score = targetGame.getScore(userid);
                    scoreBoardFront.put(username, score);
                }
                scoreBoardFront.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));
                messagingTemplate.convertAndSend("/topic/user/"+targetGame.getGameId()+"/scoreBoard", scoreBoardFront);
                log.info("websocket send!");

                return gameHintDTO;
            }
            else {
                gameRepository.save(targetGame);
                gameRepository.flush();

                GameGetDTO gameHintDTO = new GameGetDTO();
                generatedHints = getHintsOfOneCountry(targetGame.getGameId(), userId, targetGame.getDifficulty());
                gameHintDTO.setHints(generatedHints.values().iterator().next());
                answers.put(userId, generatedHints.keySet().iterator().next());
                gameHintDTO.setAnswer(answers.get(userId).ordinal());
                gameHintDTO.setJudgement(false);
                Map<String, Integer> scoreBoardFront = new HashMap<>();
                for (Long userid : targetGame.getPlayers()) {
                    String username = (userRepository.findByUserId(userid)).getUsername();
                    int score = (targetGame.getScoreBoard()).get(userid);
                    scoreBoardFront.put(username, score);
                }
                scoreBoardFront.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));
                messagingTemplate.convertAndSend("/topic/user/"+targetGame.getGameId()+"/scoreBoard", scoreBoardFront);
                log.info("websocket send!");

                return gameHintDTO;
            }
        }
    }
    
    public void saveGame(Long gameId) {
        Game gameToSave = gameRepository.findBygameId(gameId);
        if (gameToSave == null ) {
            return;
        }
        if(gameToSave.getModeType().equals("combat")){
            for (Long userId : gameToSave.getScoreBoard().keySet()) {
                User player = userRepository.findByUserId(userId);
                player.setGameHistory(gameToSave.getGameName(), gameToSave.getScore(userId), gameToSave.getCorrectAnswers(userId), 
                gameToSave.getTotalQuestions(userId), gameToSave.getGameCreationDate(), gameToSave.getTime(), gameToSave.getModeType());
                player.setLevel(new BigDecimal(gameToSave.getScore(userId)).divide(new BigDecimal(100), 1, RoundingMode.HALF_UP).add(player.getLevel()));
                userRepository.save(player);
                userRepository.flush();
            }
            gameToSave.setGameRunning(false);
            LocalDateTime now = LocalDateTime.now();
            gameToSave.setGameCreationDate(now);
            gameRepository.save(gameToSave);
            gameRepository.flush();
           
        }
        else if(gameToSave.getModeType().equals("solo")){
            User player = userRepository.findByUserId(gameToSave.getOwnerId());
            player.setGameHistory(gameToSave.getGameName(), gameToSave.getScore(gameToSave.getOwnerId()), gameToSave.getCorrectAnswers(gameToSave.getOwnerId()), 
            gameToSave.getTotalQuestions(gameToSave.getOwnerId()), gameToSave.getGameCreationDate(), gameToSave.getTime(),gameToSave.getModeType());
            player.setGame(null);
            userRepository.save(player);
            userRepository.flush();
            gameRepository.deleteByGameId(gameId);
        }
        else{
            User player = userRepository.findByUserId(gameToSave.getOwnerId());
            player.setGame(null);
            userRepository.save(player);
            userRepository.flush();
            gameRepository.deleteByGameId(gameId);
        }
        utilService.removeCacheForGame(gameId);
    }

    public void giveupGame(Long userId) {
        if (userRepository.findByUserId(userId).getGame() == null) { return ; }
        Game gameToEnd = (userRepository.findByUserId(userId)).getGame();

        if (gameToEnd.getRealPlayersNumber() == 1) {
            gameToEnd.updateScore(userId, -1);
            User playerToEnd = userRepository.findByUserId(userId);
            gameToEnd.updateScore(userId, -1);
            playerToEnd.setGameHistory(gameToEnd.getGameName(), gameToEnd.getScore(userId ), gameToEnd.getCorrectAnswers(userId), 
            gameToEnd.getTotalQuestions(userId), gameToEnd.getGameCreationDate(), gameToEnd.getTime(),gameToEnd.getModeType());
            playerToEnd.setGame(null);
            playerToEnd.setReady(false);
            userRepository.save(playerToEnd);
            userRepository.flush();
            gameRepository.deleteByGameId(gameToEnd.getGameId());
            utilService.removeCacheForGame(gameToEnd.getGameId());
        }
        else {
            if (gameToEnd.getOwnerId().equals(userId)) {
                gameToEnd.setRealPlayersNumber(gameToEnd.getRealPlayersNumber() - 1);
                User playerToEnd = userRepository.findByUserId(userId);
                gameToEnd.setOwnerId(gameToEnd.getPlayers().get(1));
                gameToEnd.removePlayer(playerToEnd);
                messagingTemplate.convertAndSend("/topic/game/"+gameToEnd.getGameId()+"/owner", gameToEnd.getOwnerId());
                gameToEnd.updateScore(userId, -1);
                playerToEnd.setGameHistory(gameToEnd.getGameName(), gameToEnd.getScore(userId ), gameToEnd.getCorrectAnswers(userId ), 
                gameToEnd.getTotalQuestions(userId ), gameToEnd.getGameCreationDate(),gameToEnd.getTime(), gameToEnd.getModeType());
                playerToEnd.setGame(null);
                playerToEnd.setReady(false);
                userRepository.save(playerToEnd);
                userRepository.flush();
                getGameLobby();
            }
            else {
                gameToEnd.setRealPlayersNumber(gameToEnd.getRealPlayersNumber() - 1);
                User playerToEnd = userRepository.findByUserId(userId);
                gameToEnd.removePlayer(playerToEnd);
                gameToEnd.updateScore(userId, -1);
                playerToEnd.setGameHistory(gameToEnd.getGameName(), gameToEnd.getScore(userId ), gameToEnd.getCorrectAnswers(userId ), 
                gameToEnd.getTotalQuestions(userId ), gameToEnd.getGameCreationDate(),gameToEnd.getTime(),gameToEnd.getModeType());
                playerToEnd.setGame(null);
                playerToEnd.setReady(false);
                userRepository.save(playerToEnd);
                userRepository.flush();
                getGameLobby();
            }
            Map<String, Integer> scoreBoardFront = new HashMap<>();
            for (Long userid : gameToEnd.getScoreBoard().keySet()) {
                String username = (userRepository.findByUserId(userid)).getUsername();
                int score = (gameToEnd.getScoreBoard()).get(userid);
                scoreBoardFront.put(username, score);
            }
            scoreBoardFront.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
            messagingTemplate.convertAndSend("/topic/user/"+gameToEnd.getGameId()+"/scoreBoard", scoreBoardFront);
            log.info("websocket send: scoreBoard!");

            gameToEnd.removePlayer(userRepository.findByUserId(userId));
            gameRepository.save(gameToEnd);
            gameRepository.flush();
            utilService.removeExitPlayer(gameToEnd.getGameId(), userId);
        }
    }

    public List<UserGetDTO> getLeaderboard() {

        List<User> allUsers = userRepository.findAll();
        List<UserGetDTO> leaderBoard = new ArrayList<>(); 

        for (User user : allUsers) {
            UserGetDTO userGetDTO = new UserGetDTO();
            userGetDTO.setUserId(user.getUserId()); 
            userGetDTO.setLevel((user.getLevel().multiply(new BigDecimal(100))).intValue());
            userGetDTO.setUsername(user.getUsername());
            userGetDTO.setAvatar(user.getAvatar());
            leaderBoard.add(userGetDTO);
        }
        leaderBoard.sort(Comparator.comparing(UserGetDTO::getLevel).reversed());
        return leaderBoard;
    }
}