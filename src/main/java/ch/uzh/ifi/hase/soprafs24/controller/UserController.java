package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPasswordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
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
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

 

  @PutMapping("/users/pwd")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@RequestBody UserPasswordDTO userPasswordDTO) {
      // verification
      User authenticatedUser = userService.userAuthenticate(new User() {{
        setToken(userPasswordDTO.getToken());
      }});
      
      userService.changePassword(authenticatedUser.getUserId(), userPasswordDTO.getCurrentPassword(), userPasswordDTO.getNewPassword());
  }

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
    User authenticatedUser = userService.userAuthenticate(new User() {{
      setToken(userPostDTO.getToken());
      }});
      
      userService.logout(authenticatedUser);
  }
}
