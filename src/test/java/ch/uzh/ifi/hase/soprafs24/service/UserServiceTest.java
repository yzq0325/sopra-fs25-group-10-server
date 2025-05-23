package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Optional;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Optional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setUserId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    testUser.setAvatar("/avatar_1.png");
    testUser.setBio("");
    testUser.setEmail("");

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);
    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getUserId(), createdUser.getUserId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
    assertNotNull(createdUser.getAvatar());
    assertEquals("", createdUser.getEmail());
    assertEquals("", createdUser.getBio());
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void changePassword_validInputs_success() {
      // given: a user with existing password
      testUser.setPassword("oldPassword");
      Mockito.when(userRepository.findByUserId(any())).thenReturn(testUser);
    
      UserPostDTO  userPostDTO = new UserPostDTO();
      userPostDTO.setPassword("newPassword");
      userPostDTO.setUserId(1L);
      // when: user changes password with correct current password
      userService.changePassword(userPostDTO);
  
      // then: password should be updated and saved
      assertEquals("newPassword", testUser.getPassword());
      Mockito.verify(userRepository).save(testUser);
  }

  @Test
  public void changePassword_incorrectPasswordFormat_throwsException() {
      // given: current password is incorrect
      testUser.setPassword("oldPassword");
      Mockito.when(userRepository.findByUserId(any())).thenReturn(testUser);

      UserPostDTO  userPostDTO = new UserPostDTO();
      userPostDTO.setPassword(" ");
      // when + then: expect 400 BAD_REQUEST
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.changePassword(userPostDTO)
      );
  
      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
      assertTrue(exception.getReason().contains("The password can not contain space! Please change one!"));
  }

  @Test
  public void changePassword_userNotFound_throwsException() {
      // given: repository returns empty
      Mockito.when(userRepository.findByUserId(any())).thenReturn(null);
  
      // when + then: expect 404 NOT_FOUND
      UserPostDTO  userPostDTO = new UserPostDTO();
      userPostDTO.setPassword("newPassword");
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.changePassword(userPostDTO)
      );
  
      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
      assertTrue(exception.getReason().contains("User not found!"));
  }

  @Test
  public void userAuthenticate_invalidToken_throwsException() {
    // given
    Mockito.when(userRepository.findByToken(testUser.getToken())).thenReturn(null);

    // when
    User freshUser = new User();
    freshUser.setUsername("testUsername");
    freshUser.setToken(testUser.getToken());

    // then
    ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> userService.userAuthenticate(freshUser));
    assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    assertEquals("User Not Authenticated", e.getReason());
  }

  @Test
  public void login_validCredentials_success() {
    // given
    User loginUser = new User();
    loginUser.setUsername("testUser");
    loginUser.setPassword("correctPassword");

    User userInDB = new User();
    userInDB.setUsername("testUser");
    userInDB.setPassword("correctPassword");
    userInDB.setStatus(UserStatus.OFFLINE); 

    Mockito.when(userRepository.findByUsername("testUser")).thenReturn(userInDB);

    // when
    User loggedInUser = userService.login(loginUser);

    // then
    assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
    assertNotNull(loggedInUser.getToken());
  }

  @Test
  public void login_invalidCredentials_throwsException() {
    // given
    User loginUser = new User();
    loginUser.setUsername("testUser");
    loginUser.setPassword("wrongPassword");

    User userInDB = new User();
    userInDB.setUsername("testUser");
    userInDB.setPassword("correctPassword");

    Mockito.when(userRepository.findByUsername("testUser")).thenReturn(userInDB);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.login(loginUser);
    });

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    assertEquals("Your password is not correct! Please type again!", exception.getReason());
  }

  @Test
  public void logout_validToken_success() {
    // given
    User user = new User();
    user.setToken("validToken");

    User userInDB = new User();
    userInDB.setToken("validToken");
    userInDB.setStatus(UserStatus.ONLINE);

    Mockito.when(userRepository.findByToken("validToken")).thenReturn(userInDB);

    // when
    userService.logout(user);

    // then
    Mockito.verify(userRepository).save(userInDB);
    assertEquals(UserStatus.OFFLINE, userInDB.getStatus());
  }
  
  @Test
  public void logout_invalidToken_throwsException() {
    // given
    User user = new User();
    user.setToken("invalidToken");

    Mockito.when(userRepository.findByToken("invalidToken")).thenReturn(null);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.logout(user);
    });

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertEquals("User not found", exception.getReason());
  }

  @Test
  public void findUserById_validId_success() {
    // given
    testUser.setUserId(1L);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // when
    User foundUser = userService.findUserById(1L);

    // then
    assertNotNull(foundUser);
    assertEquals(testUser.getUserId(), foundUser.getUserId());
    assertEquals(testUser.getUsername(), foundUser.getUsername());
  }

  @Test
  public void findUserById_invalidId_throwsException() {
    // given
    Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.findUserById(999L);
    });

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertEquals("User not found", exception.getReason());
  }



  @Test
  public void updateUserProfile_validInputs_success() {
    // given
    testUser.setUserId(1L);
    testUser.setUsername("oldUsername");

    User updateInfo = new User();
    updateInfo.setUsername("newUsername");
    updateInfo.setAvatar("/avatar_1.png");
    updateInfo.setEmail("new@email.com");
    updateInfo.setBio("new bio");

    Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

    // when
    userService.updateUserProfile(testUser.getUserId(), updateInfo);

    // then
    assertEquals("newUsername", testUser.getUsername());
    assertEquals("/avatar_1.png", testUser.getAvatar());
    assertEquals("new@email.com", testUser.getEmail());
    assertEquals("new bio", testUser.getBio());
  }

  @Test
  public void updateUserProfile_userNotFound_throwsException() {
    // given
    Mockito.when(userRepository.findByUserId(999L)).thenReturn(null);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(999L, new User())
    );
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertTrue(exception.getReason().contains("User not found!"));
  }

  @Test
  public void updateUserProfile_emptyUsername_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername(""); // blank

    Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertTrue(exception.getReason().contains("The username can not be empty! Please change one!"));
  }

  @Test
  public void updateUserProfile_duplicateUsername_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername("duplicateUsername");

    User existingUser = new User();
    existingUser.setUsername("duplicateUsername");

    Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername("duplicateUsername")).thenReturn(existingUser);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    assertTrue(exception.getReason().contains("Username exists! Please change one!"));
  }

  @Test
  public void updateUserProfile_invalidAvatar_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername("newUsername");
    updateInfo.setAvatar("invalid_avatar.png");
    updateInfo.setBio("");
    updateInfo.setEmail("");

    Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertTrue(exception.getReason().contains("Invalid avatar selection"));
  }

  @Test
  public void updateUserProfile_invalidEmailFormat_throwsException() {
      User updateInfo = new User();
      updateInfo.setUsername("validUsername");
      updateInfo.setAvatar("/avatar_1.png");
      updateInfo.setEmail("invalid-email");
      updateInfo.setBio("");

      Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);
      Mockito.when(userRepository.findByUsername("validUsername")).thenReturn(null);

      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.updateUserProfile(testUser.getUserId(), updateInfo)
      );

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
      assertTrue(exception.getReason().contains("The email is not correct"));
  }

  @Test
  public void updateUserProfile_bioTooLong_throwsException() {
      User updateInfo = new User();
      updateInfo.setUsername("validUsername");
      updateInfo.setAvatar("/avatar_1.png");
      updateInfo.setEmail("valid@email.com");
      updateInfo.setBio("a".repeat(201)); // 201 chars

      Mockito.when(userRepository.findByUserId(testUser.getUserId())).thenReturn(testUser);
      Mockito.when(userRepository.findByUsername("validUsername")).thenReturn(null);

      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.updateUserProfile(testUser.getUserId(), updateInfo)
      );

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
      assertTrue(exception.getReason().contains("The bio is too long"));
  }

  @Test
  public void getUsers_returnsUserList_success() {
      List<User> mockUsers = Collections.singletonList(testUser);
      Mockito.when(userRepository.findAll()).thenReturn(mockUsers);

      List<User> result = userService.getUsers();

      assertEquals(1, result.size());
      assertEquals(testUser.getUsername(), result.get(0).getUsername());
  }

  @Test
  public void getUser_validId_returnsCorrectUserGetDTO() {
      testUser.setLevel(new BigDecimal("3.0"));
      Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      UserGetDTO dto = userService.getUser(1L);

      assertEquals("testUsername", dto.getUsername());
      assertEquals(300, dto.getLevel()); // 3.0 * 100
  }

  @Test
  public void getHistory_validUser_returnsGameHistory() {
      User userWithHistory = new User();
      userWithHistory.setUserId(1L);
      userWithHistory.setUsername("testUser");
      userWithHistory.setAvatar("/avatar_1.png");
      userWithHistory.setBio("");
      userWithHistory.setEmail("");

      userWithHistory.setGameHistory("TestGame", 100, 5, 10, LocalDateTime.now(), 60, "combat", "easy");

      Mockito.when(userRepository.findByUserId(1L)).thenReturn(userWithHistory);

      UserGetDTO result = userService.getHistory(1L);

      assertNotNull(result);
      assertNotNull(result.getGameHistory());
      assertEquals(1, result.getGameHistory().size());
      assertEquals("TestGame", result.getGameHistory().get(0).getGameName());
  }

  @Test
  public void getLearningTracking_validUser_returnsMap() {
      User userWithTracking = new User();
      userWithTracking.setUserId(1L);

      userWithTracking.updateLearningTrack(Country.France);
      userWithTracking.updateLearningTrack(Country.France);
      userWithTracking.updateLearningTrack(Country.Germany);

      Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(userWithTracking));

      UserGetDTO dto = userService.getLearningTracking(1L);

      assertNotNull(dto);
      assertEquals(2, dto.getLearningTracking().get(Country.France));
      assertEquals(1, dto.getLearningTracking().get(Country.Germany));
  }
}
