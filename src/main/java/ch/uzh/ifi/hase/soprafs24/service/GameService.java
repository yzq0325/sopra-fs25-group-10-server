package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.time.LocalDate;
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

    public Game createGame(GamePostDTO gamePostDTO) {

      Game newGame = new Game();
        
      LocalDate currentDate = LocalDate.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      newGame.setGameCreationDate(currentDate.format(formatter));
        
      Map<String, Integer> scoreBoard = new HashMap<>();
      for (String username : gamePostDTO.getPlayers()) {
        User player = userRepository.findByUsername(username);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, username + " is not found");
        }
        newGame.addPlayer(player);  
        scoreBoard.put(username, 0); 
    }
    
      newGame.setHintsNumber(5);
      newGame = gameRepository.save(newGame);
      gameRepository.flush();
      
      log.debug("Created new Game: {}", newGame);
      return newGame;
    }
}