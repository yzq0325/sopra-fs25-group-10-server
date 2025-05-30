package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.Country;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;
  private User testUser;

  @BeforeEach
  public void setup() {
    userRepository.deleteAll();

    testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    testUser.setAvatar("/avatar_1.png");
    testUser.setEmail("");
    testUser.setBio("");
    testUser.setStatus(UserStatus.OFFLINE);
    testUser.setToken(UUID.randomUUID().toString());
    userRepository.save(testUser);
    userRepository.flush();
    
    assertNotNull(userRepository.findByUsername("testUsername"));
  }

  @Test
  public void createUser_validInputs_success() {
    User newUser = new User();
    newUser.setUsername("testValidUsername");
    newUser.setPassword("testPassword");
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.OFFLINE);

    User createdUser = userService.createUser(newUser);

    // then
    assertNotNull(createdUser.getUserId());
    assertEquals("testValidUsername", createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
    assertNotNull(createdUser.getAvatar());
    assertEquals("", createdUser.getEmail());
    assertEquals("", createdUser.getBio());
    assertNotNull(createdUser.getLevel());
    assertEquals(new BigDecimal("0.0"), createdUser.getLevel());
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    // attempt to create second user with same username
    User testUser2 = new User();

    // change the name but forget about the username
    testUser2.setUsername("testUsername");
    testUser2.setPassword("testPassword"); 
    testUser2.setToken(UUID.randomUUID().toString());

    // check that an error is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
  }

  @Test
  public void login_validCredentials_success() {
    User loginUser = new User();
    loginUser.setUsername("testUsername");
    loginUser.setPassword("testPassword");
  
    User loggedInUser = userService.login(loginUser);
  
    assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
    assertNotNull(loggedInUser.getToken());
    assertNotEquals("", loggedInUser.getToken());
  }


  @Test
  public void login_userNotFound_throwsUnauthorized() {
    User loginUser = new User();
    loginUser.setUsername("nonexistent");
    loginUser.setPassword("anyPassword");
  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.login(loginUser);
    });
  
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertEquals("Your username is not found! Please register one or use correct username!", exception.getReason());
  }
  
  @Test
  public void login_wrongPassword_throwsUnauthorized() {
    User loginUser = new User();
    loginUser.setUsername("testUsername");
    loginUser.setPassword("wrongPassword");
  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.login(loginUser);
    });
  
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    assertEquals("Your password is not correct! Please type again!", exception.getReason());
  }
  
  @Test
  public void login_userAlreadyOnline_throwsBadRequest() {
    testUser.setStatus(UserStatus.ONLINE);
    userRepository.saveAndFlush(testUser);
  
    User loginUser = new User();
    loginUser.setUsername("testUsername");
    loginUser.setPassword("testPassword");
  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.login(loginUser);
    });
  
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals("This user has already logged in!", exception.getReason());
  }

  @Test
  public void logout_success() {
    testUser.setStatus(UserStatus.ONLINE);
    testUser.setToken(UUID.randomUUID().toString());
    userRepository.save(testUser);

    userService.logout(testUser);
  
    User loggedOutUser = userRepository.findById(testUser.getUserId()).get();
    assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
    // assertEquals("", loggedOutUser.getToken());
  }

  @Test
  public void logout_invalidToken_throwsNotFound() {
    User fakeUser = new User();
    fakeUser.setToken("invalid-token");
  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.logout(fakeUser);
    });
  
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertEquals("User not found", exception.getReason());
  }

  @Test
  public void changePassword_validInput_success() {
    testUser.setPassword("oldPassword");
    testUser = userRepository.saveAndFlush(testUser);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUserId(testUser.getUserId());
    userPostDTO.setPassword("newPassword123");

    userService.changePassword(userPostDTO);

    User updatedUser = userRepository.findById(testUser.getUserId()).orElse(null);
    assertNotNull(updatedUser);
    assertEquals("newPassword123", updatedUser.getPassword());
  }
  
  @Test
  public void changePassword_newPasswordEmpty_throwsBadRequest() {
    testUser.setPassword("correctOld");
    userRepository.saveAndFlush(testUser);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUserId(testUser.getUserId());
    userPostDTO.setPassword("");

  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.changePassword(userPostDTO);
    });
  
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals("The password can not be empty! Please change one!", exception.getReason());
  }
  
  @Test
  public void changePassword_userNotFound_throwsNotFound() {
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("UserNotExist");
    userPostDTO.setPassword("newPassword123");
  
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        userService.changePassword(userPostDTO);
    });
  
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertEquals("User not found!", exception.getReason());
  }
  
  @Test
  public void findUserById_validId_success() {
    User savedUser = new User();
    savedUser.setUsername("abc");
    savedUser.setPassword("abc");
    savedUser.setToken(UUID.randomUUID().toString());
    savedUser.setStatus(UserStatus.OFFLINE);
    savedUser.setAvatar("/avatar_1.png");
    savedUser.setEmail("");
    savedUser.setBio("");

    savedUser = userRepository.saveAndFlush(savedUser);

    User foundUser = userService.findUserById(savedUser.getUserId());

    assertNotNull(foundUser);
    assertEquals("abc", foundUser.getUsername());
  }

  @Test
  public void userAuthenticate_validToken_success() {
    User user = userService.userAuthenticate(testUser);

    assertNotNull(user);
  }

  @Test
  public void userAuthenticate_invalidToken_throwsException() {
    User testUser2 = new User();
    testUser2.setToken(UUID.randomUUID().toString());

    ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> userService.userAuthenticate(testUser2));
    assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    assertEquals("User Not Authenticated", e.getReason());
  }

  @Test
  public void updateUserProfile_validData_success() {
      User updateInfo = new User();
      updateInfo.setUsername("newUsername");
      updateInfo.setAvatar("/avatar_2.png");
      updateInfo.setEmail("new@example.com");
      updateInfo.setBio("New bio");
  
      User updatedUser = userService.updateUserProfile(testUser.getUserId(), updateInfo);
  
      assertEquals("newUsername", updatedUser.getUsername());
      assertEquals("/avatar_2.png", updatedUser.getAvatar());
      assertEquals("new@example.com", updatedUser.getEmail());
      assertEquals("New bio", updatedUser.getBio());
  }

  @Test
  public void updateUserProfile_invalidAvatar_throwsException() {
    User update = new User();
    update.setUsername("newName");
    update.setAvatar("/invalid.png");
    update.setEmail("");
    update.setBio("");
  
    assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), update));
  }

  @Test
  public void updateUserProfile_usernameExists_throwsException() {
    User existing = new User();
    existing.setUsername("duplicate");
    existing.setPassword("pass");
    existing.setToken(UUID.randomUUID().toString());
    existing.setStatus(UserStatus.OFFLINE);
    existing.setAvatar("/avatar_1.png");
    existing.setEmail("");
    existing.setBio("");
    userRepository.saveAndFlush(existing);
  
    User update = new User();
    update.setUsername("duplicate");
    update.setAvatar("/avatar_1.png");
    update.setEmail("");
    update.setBio("");
  
    assertThrows(ResponseStatusException.class, () ->
        userService.updateUserProfile(testUser.getUserId(), update));
  }

  @Test
  public void getGameHistory_success() {
    testUser.setGameHistory("game1", 100, 8, 10, LocalDateTime.now(), 5, "solo", "easy");
    userRepository.save(testUser);

    UserGetDTO result = userService.getHistory(testUser.getUserId());

    assertNotNull(result.getGameHistory());
  }

  @Test
  public void getUser_validId_success() {
    User saved = userRepository.saveAndFlush(testUser);
    UserGetDTO result = userService.getUser(saved.getUserId());
  
    assertNotNull(result);
    assertEquals(0, result.getLevel());
  }

  @Test
  public void getLearningTracking_success() {
    testUser.updateLearningTrack(Country.Australia);
    userRepository.saveAndFlush(testUser);
  
    UserGetDTO dto = userService.getLearningTracking(testUser.getUserId());
  
    assertNotNull(dto);
    assertTrue(dto.getLearningTracking().containsKey(Country.Australia));
    assertEquals(1, dto.getLearningTracking().get(Country.Australia));
  }
}
