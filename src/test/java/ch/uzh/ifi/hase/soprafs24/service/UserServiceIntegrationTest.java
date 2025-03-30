package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
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
    testUser.setName("testName");
    testUser.setPassword("testPassword");
    testUser.setStatus(UserStatus.OFFLINE);
    testUser.setToken(UUID.randomUUID().toString());
    userRepository.save(testUser);
    userRepository.flush();
    
    assertNotNull(userRepository.findByUsername("testUsername"));
  }

  @Test
  public void createUser_validInputs_success() {
    // given
    testUser.setUsername("testValidUsername");
    testUser.setName("testValidName");
    testUser.setPassword("testPassword"); 
    testUser.setToken(UUID.randomUUID().toString());

    // when
    User createdUser = userService.createUser(testUser);

    // then
    assertEquals(testUser.getUserId(), createdUser.getUserId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    // attempt to create second user with same username
    User testUser2 = new User();

    // change the name but forget about the username
    testUser2.setUsername("testUsername");
    testUser2.setName("testName2");
    testUser2.setPassword("testPassword"); 
    testUser2.setToken(UUID.randomUUID().toString());

    // check that an error is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
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
}
