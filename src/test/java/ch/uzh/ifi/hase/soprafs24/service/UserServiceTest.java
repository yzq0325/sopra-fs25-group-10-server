package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Optional;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

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
      Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
  
      // when: user changes password with correct current password
      userService.changePassword(testUser.getUserId(), "oldPassword", "newPassword123");
  
      // then: password should be updated and saved
      assertEquals("newPassword123", testUser.getPassword());
      Mockito.verify(userRepository).save(testUser);
  }

  @Test
  public void changePassword_incorrectCurrentPassword_throwsException() {
      // given: current password is incorrect
      testUser.setPassword("correctPassword");
      Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
  
      // when + then: expect 400 BAD_REQUEST
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.changePassword(testUser.getUserId(), "wrongPassword", "newPassword")
      );
  
      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
      assertTrue(exception.getReason().contains("Incorrect current password"));
  }

  @Test
  public void changePassword_emptyNewPassword_throwsException() {
      // given: new password is empty
      testUser.setPassword("correctPassword");
      Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
  
      // when + then: expect 400 BAD_REQUEST
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.changePassword(testUser.getUserId(), "correctPassword", "")
      );
  
      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
      assertTrue(exception.getReason().contains("New password must not be empty"));
  }

  @Test
  public void changePassword_userNotFound_throwsException() {
      // given: repository returns empty
      Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());
  
      // when + then: expect 404 NOT_FOUND
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
          userService.changePassword(999L, "any", "new")
      );
  
      assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
      assertTrue(exception.getReason().contains("User not found"));
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
    updateInfo.setPassword("password");

    Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

    // when
    userService.updateUserProfile(testUser.getUserId(), updateInfo);

    // then
    assertEquals("newUsername", testUser.getUsername());
    assertEquals("/avatar_1.png", testUser.getAvatar());
    assertEquals("new@email.com", testUser.getEmail());
    assertEquals("new bio", testUser.getBio());
    assertEquals("password", testUser.getPassword());
  }

  @Test
  public void updateUserProfile_userNotFound_throwsException() {
    // given
    Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(999L, new User())
    );
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertTrue(exception.getReason().contains("User not found"));
  }

  @Test
  public void updateUserProfile_emptyUsername_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername("   "); // blank

    Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertTrue(exception.getReason().contains("Username must not be empty"));
  }

  @Test
  public void updateUserProfile_duplicateUsername_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername("duplicateUsername");

    User existingUser = new User();
    existingUser.setUsername("duplicateUsername");

    Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    Mockito.when(userRepository.findByUsername("duplicateUsername")).thenReturn(existingUser);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertTrue(exception.getReason().contains("Username already exists"));
  }

  @Test
  public void updateUserProfile_invalidAvatar_throwsException() {
    // given
    User updateInfo = new User();
    updateInfo.setUsername("newUsername");
    updateInfo.setAvatar("invalid_avatar.png");

    Mockito.when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

    // when + then
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), updateInfo)
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertTrue(exception.getReason().contains("Invalid avatar selection"));
  }
}
