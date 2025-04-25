package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPasswordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserProfileDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }


  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    
  }
    

  // @PutMapping("/users/pwd")
  // @ResponseStatus(HttpStatus.NO_CONTENT)
  // public void changePassword(@RequestBody UserPasswordDTO userPasswordDTO) {
  //     // verification
  //     User userInput = new User();
  //     userInput.setToken(userPasswordDTO.getToken());
  //     User authenticatedUser = userService.userAuthenticate(userInput);
  
  //     // change password
  //     userService.changePassword(authenticatedUser.getUserId(), userPasswordDTO.getCurrentPassword(), userPasswordDTO.getNewPassword());
  // }

  @PostMapping("/auth")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO userAuthenticate(@RequestBody UserPostDTO userPostDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    User userVerified = userService.userAuthenticate(userInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(userVerified);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO login(@RequestBody UserPostDTO userPostDTO) {
    User loginInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User loggedInUser = userService.login(loginInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody UserPostDTO userPostDTO) {
    // verification
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User authenticatedUser = userService.userAuthenticate(userInput);

    userService.logout(authenticatedUser);
  }

  @GetMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getUserProfile(@PathVariable("userId") Long userId) {
    return userService.getUser(userId);
  }

  @PutMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO updateUserProfile(@PathVariable("userId") Long userId,@RequestBody UserPostDTO userPostDTO) {
    User updatedInfo = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User updatedUser = userService.updateUserProfile(userId, updatedInfo);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
  }

  @GetMapping("/history/{userId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getHistory(@PathVariable Long userId){
    return userService.getHistory(userId);
  }

  @GetMapping("/users/{userId}/statistics")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getStatistics(@PathVariable Long userId){
    return userService.getLearningTracking(userId);
  }
}
