package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPasswordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserProfileDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;
  
  private User user;
  private UserPostDTO userPostDTO;

  @BeforeEach
  public void setup() {
    user = new User();
    user.setUserId(1L);
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.OFFLINE);

    userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
  }

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setUserId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("Test User");
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }
/* 
  @Test
  public void changePassword_validToken_success() throws Exception {
      // given: valid token and password DTO
      UserPasswordDTO passwordDTO = new UserPasswordDTO();
      passwordDTO.setToken("valid-token");
      passwordDTO.setCurrentPassword("oldPassword");
      passwordDTO.setNewPassword("newPassword123");
  
      User user = new User();
      user.setUserId(1L);
      user.setToken("valid-token");
  
      // when: mock authentication and password change
      given(userService.userAuthenticate(Mockito.any())).willReturn(user);
      doNothing().when(userService).changePassword(user.getUserId(), "oldPassword", "newPassword123");
  
      // then: perform request and expect 204
      MockHttpServletRequestBuilder request = put("/users/pwd")
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJsonString(passwordDTO));
  
      mockMvc.perform(request)
          .andExpect(status().isNoContent());
  }

  @Test
  public void changePassword_invalidToken_throwsException() throws Exception {
    // given: DTO with invalid token
    UserPasswordDTO passwordDTO = new UserPasswordDTO();
    passwordDTO.setToken("invalid-token");
    passwordDTO.setCurrentPassword("oldPassword");
    passwordDTO.setNewPassword("newPassword123");

    // when: authentication fails
    given(userService.userAuthenticate(Mockito.any()))
        .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Authenticated"));

    // then: perform request and expect 404 with reason
    MockHttpServletRequestBuilder request = put("/users/pwd")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(passwordDTO));

    mockMvc.perform(request)
        .andExpect(status().isNotFound())
        .andExpect(status().reason("User Not Authenticated"));
  } */
  
  @Test
  public void userAuthenticate_validToken_success() throws Exception {
    // given
    given(userService.userAuthenticate(Mockito.any())).willReturn(user);

    // when
    MockHttpServletRequestBuilder postRequest = post("/auth")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then    
    mockMvc.perform(postRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists());
  }

  @Test
  public void userAuthenticate_invalidToken_throwsException() throws Exception {
    // given
    given(userService.userAuthenticate(Mockito.any())).willThrow(
      new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Authenticated"));
    
    // when
    MockHttpServletRequestBuilder postRequest = post("/auth")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));
    
    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isNotFound())
        .andExpect(status().reason("User Not Authenticated"));
  }

  @Test
  public void login_validCredentials_success() throws Exception {
    // given
    given(userService.login(Mockito.any())).willReturn(user);
  
    // when
    MockHttpServletRequestBuilder postRequest = post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));
  
    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(user.getUsername()))
        .andExpect(jsonPath("$.token").exists());
  }
  
  @Test
  public void login_invalidCredentials_throwsUnauthorized() throws Exception {
    // given
    given(userService.login(Mockito.any())).willThrow(
        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password")
    );
  
    // when
    MockHttpServletRequestBuilder postRequest = post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));
  
    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isUnauthorized())
        .andExpect(status().reason("Invalid username or password"));
  }

  @Test
  public void logout_validToken_success() throws Exception {
    // given
    User user = new User();
    user.setToken("validToken");

    Mockito.when(userService.userAuthenticate(Mockito.any())).thenReturn(user);
    Mockito.doNothing().when(userService).logout(Mockito.any());

    // when
    MockHttpServletRequestBuilder postRequest = post("/logout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isNoContent());
  }

  @Test
  public void logout_invalidToken_throwsException() throws Exception {
    // given
    Mockito.when(userService.userAuthenticate(Mockito.any()))
           .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // when
    MockHttpServletRequestBuilder postRequest = post("/logout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
          .andExpect(status().isNotFound())
          .andExpect(status().reason("User not found"));
  }

  @Test
  public void getUserProfile_validId_success() throws Exception {
    UserGetDTO userGetDTO = new UserGetDTO();
    userGetDTO.setUsername("testUsername");
    userGetDTO.setAvatar("avatar1.png");
    userGetDTO.setEmail("test@example.com");
    userGetDTO.setBio("This is a test bio.");
    userGetDTO.setLevel(300);

    given(userService.getUser(1L)).willReturn(userGetDTO);

    mockMvc.perform(get("/users/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testUsername"))
        .andExpect(jsonPath("$.avatar").value("avatar1.png"))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.bio").value("This is a test bio."))
        .andExpect(jsonPath("$.level").value(300));
  }

  @Test
  public void getUserProfile_userNotFound_throwsException() throws Exception {
    // given
    Long nonExistentUserId = 999L;
    given(userService.getUser(nonExistentUserId))
        .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // when
    MockHttpServletRequestBuilder getRequest = get("/users/" + nonExistentUserId)
        .contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest)
        .andExpect(status().isNotFound())
        .andExpect(status().reason("User not found"));
  }

  @Test
  public void updateUserProfile_validInput_success() throws Exception {
    // given
    UserProfileDTO updateDTO = new UserProfileDTO();
    updateDTO.setUsername("updatedUsername");
    updateDTO.setAvatar("avatar2.png");
    updateDTO.setEmail("updated@example.com");
    updateDTO.setBio("Updated bio");

    User updatedUser = new User();
    updatedUser.setUserId(1L);
    updatedUser.setUsername(updateDTO.getUsername());
    updatedUser.setAvatar(updateDTO.getAvatar());
    updatedUser.setEmail(updateDTO.getEmail());
    updatedUser.setBio(updateDTO.getBio());

    given(userService.updateUserProfile(eq(1L), any())).willReturn(updatedUser);

    // when
    MockHttpServletRequestBuilder putRequest = put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(updateDTO));

    // then
    mockMvc.perform(putRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(updateDTO.getUsername())))
        .andExpect(jsonPath("$.avatar", is(updateDTO.getAvatar())))
        .andExpect(jsonPath("$.email", is(updateDTO.getEmail())))
        .andExpect(jsonPath("$.bio", is(updateDTO.getBio())));
  }

  @Test
  public void updateUserProfile_emptyUsername_throwsException() throws Exception {
    // given
    UserProfileDTO updateDTO = new UserProfileDTO();
    updateDTO.setUsername("");
    updateDTO.setAvatar("avatar1.png");
    updateDTO.setEmail("test@example.com");
    updateDTO.setBio("test bio");

    given(userService.updateUserProfile(eq(1L), any()))
        .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not be empty"));

    // when
    MockHttpServletRequestBuilder putRequest = put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(updateDTO));

    // then
    mockMvc.perform(putRequest)
        .andExpect(status().isBadRequest())
        .andExpect(status().reason("Username must not be empty"));
  }

  @Test
  public void updateUserProfile_duplicateUsername_throwsException() throws Exception {
    // given
    UserProfileDTO updateDTO = new UserProfileDTO();
    updateDTO.setUsername("duplicateUsername");
    updateDTO.setAvatar("avatar1.png");
    updateDTO.setEmail("test@example.com");
    updateDTO.setBio("test bio");

    given(userService.updateUserProfile(eq(1L), any()))
        .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists"));

    // when
    MockHttpServletRequestBuilder putRequest = put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(updateDTO));

    // then
    mockMvc.perform(putRequest)
        .andExpect(status().isBadRequest())
        .andExpect(status().reason("Username already exists"));
  }

  @Test
  public void updateUserProfile_invalidAvatar_throwsException() throws Exception {
    // given
    UserProfileDTO updateDTO = new UserProfileDTO();
    updateDTO.setUsername("validUsername");
    updateDTO.setAvatar("invalid-avatar.png");
    updateDTO.setEmail("test@example.com");
    updateDTO.setBio("test bio");

    given(userService.updateUserProfile(eq(1L), any()))
        .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar selection"));

    // when
    MockHttpServletRequestBuilder putRequest = put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(updateDTO));

    // then
    mockMvc.perform(putRequest)
        .andExpect(status().isBadRequest())
        .andExpect(status().reason("Invalid avatar selection"));
  }
  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}