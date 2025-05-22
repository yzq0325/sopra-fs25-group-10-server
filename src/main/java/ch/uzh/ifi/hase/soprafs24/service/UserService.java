package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final ConcurrentHashMap<Long, Long> userLastHeartBeatMap = new ConcurrentHashMap<>();

  private static final long HEARTBEAT_TIMEOUT = 30000;
    
  private static final long CHECK_INTERVAL = 10000;

  @PostConstruct
  public void init() {
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
      scheduler.scheduleAtFixedRate(this::checkInactiveUsers, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private final UserRepository userRepository;

  @Autowired
    private GameService gameService;


  private static final Set<String> VALID_AVATARS = Set.of(
    "/avatar_1.png",
    "/avatar_2.png",
    "/avatar_3.png",
    "/avatar_4.png",
    "/avatar_5.png",
    "/avatar_6.png"    
  );

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    checkIfUserExists(newUser);
    checkIfUsernameCorrect(newUser.getUsername());
    checkIfPasswordCorrect(newUser.getPassword());

    newUser.setAvatar(VALID_AVATARS.iterator().next());
    newUser.setEmail("");
    newUser.setBio("");
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  public void changePassword(UserPostDTO userPostDTO) {
    User userToBeChanged = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    checkIfUserNotExist(userToBeChanged.getUserId());
    checkIfPasswordCorrect(userToBeChanged.getPassword());
    
    User userInDB = userRepository.findByUserId(userToBeChanged.getUserId());
    userInDB.setPassword(userToBeChanged.getPassword());
    userRepository.save(userInDB);
  }

  public User login(User loginUser) {
    User userInDB = userRepository.findByUsername(loginUser.getUsername());
    if (userInDB == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Your username is not found! Please register one or use correct username!");
    }
    else if(userInDB != null && !userInDB.getPassword().equals(loginUser.getPassword())){
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your password is not correct! Please type again!");
    }
    if(userInDB.getStatus().equals(UserStatus.ONLINE)){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user has already logged in!");
    }

    userInDB.setStatus(UserStatus.ONLINE);
    userInDB.setToken(UUID.randomUUID().toString());
    userRepository.save(userInDB);
    return userInDB;
  }

  public void logout(User user) {
    User userInDB = userRepository.findByToken(user.getToken());
    if (userInDB == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    userInDB.setStatus(UserStatus.OFFLINE);
    userInDB.setToken(UUID.randomUUID().toString());
    userRepository.save(userInDB);
  }

  public User findUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  public User userAuthenticate(User authenticateUser) {
    User userVerified = userRepository.findByToken(authenticateUser.getToken());
    if (userVerified == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Authenticated");
    }
    return userVerified;
  }
  
  public UserGetDTO getUser(Long userId){
    User user = findUserById(userId);
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    userGetDTO.setLevel(((user.getLevel()).multiply(new BigDecimal(100))).intValue());
    return userGetDTO;
  }
  
  public User updateUserProfile(Long userId, User updatedInfo) {
    User userInDB = userRepository.findByUserId(userId);
    checkIfUserNotExist(userId);
    
    if(!updatedInfo.getUsername().equals(userInDB.getUsername())){
      checkIfUsernameExist(updatedInfo.getUsername());
      checkIfUsernameCorrect(updatedInfo.getUsername());
      userInDB.setUsername(updatedInfo.getUsername());
      userInDB.updateGameHistory(updatedInfo.getUsername());
    }
    if(!updatedInfo.getEmail().equals(userInDB.getEmail())){
      checkIfEmailCorrect(updatedInfo.getEmail());
      userInDB.setEmail(updatedInfo.getEmail());
    }
    if(!updatedInfo.getBio().equals(userInDB.getBio())){
      checkIfBioCorrect(updatedInfo.getBio());
      userInDB.setBio(updatedInfo.getBio());
    }
    if(!updatedInfo.getAvatar().equals(userInDB.getAvatar())){
      checkIfAvatarCorrect(updatedInfo);
      userInDB.setAvatar(updatedInfo.getAvatar());
    }

    userRepository.save(userInDB);
    return userInDB;
  }

  public UserGetDTO getHistory(Long userId) {
    User userToGetHistory = userRepository.findByUserId(userId);
    if (userToGetHistory == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Authenticated");
    }
    UserGetDTO userDTOwithHistory = new UserGetDTO();

    userDTOwithHistory.setGameHistory(userToGetHistory.getGameHistory());

    return userDTOwithHistory;
  }

  public UserGetDTO getLearningTracking(Long userId){
    User targetUser = findUserById(userId);
    UserGetDTO userGetDTO = new UserGetDTO();
    userGetDTO.setLearningTracking(targetUser.getLearningTracking());
    return userGetDTO;
  }

  public void updateUserHeartBeatTime(Long userId) {
        userLastHeartBeatMap.put(userId, System.currentTimeMillis());
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */

  private void checkIfUsernameExist(String username){
    User userByUsername = userRepository.findByUsername(username);

    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username exists! Please change one!");
    } 
  }
  private void checkIfUserNotExist(Long userId){
    User userByUsername = userRepository.findByUserId(userId);

    if (userByUsername == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
    } 
  }
  private void checkIfAvatarCorrect(User user){
    if (user.getAvatar() != null && !VALID_AVATARS.contains(user.getAvatar())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar selection");
    }
  }

  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    } 
  }

  private void checkIfPasswordCorrect(String password){
    if (password == ""){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password can not be empty! Please change one!");
      }
    else if(password.contains(" ")){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password can not contain space! Please change one!");
    }
    else if(password.length()>20){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is too long! Password cannot exceed 20 characters, please change one!");
    }
  }

  private void checkIfUsernameCorrect(String username){
    if (username == ""){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The username can not be empty! Please change one!");
      }
    else if(username.contains(" ")){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The username can not contain space! Please change one!");
    }
    else if(username.length()>20){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The username is too long! Username cannot exceed 20 characters, please change one!");
    }
  }

  private void checkIfEmailCorrect(String email) {
    String emailFormat = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$";
    if(!(email == "")){
      if(!email.matches(emailFormat)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The email is not correct! Please change one!");
      }
    }
  }

  private void checkIfBioCorrect(String bio){
    if(!(bio == "")){
      if(bio.length() > 200) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The bio is too long! Bio cannot exceed 200 characters, please change!");
      }
    }
  }

  private void checkInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        
        userLastHeartBeatMap.forEach((userId, lastActiveTime) -> {
            if (currentTime - lastActiveTime > HEARTBEAT_TIMEOUT) {
                User userNotActive = userRepository.findByUserId(userId);
                Game gameToExit = userNotActive.getGame();
                if(gameToExit != null){
                    gameService.giveupGame(userId);
                }
                logout(userNotActive);

                userLastHeartBeatMap.remove(userId);
            }
        });
    }
}

