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
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

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
    testUser.setId(1L);
    testUser.setName("testName");
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

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void changePassword_validInputs_success() {
    // given: a user with existing password
    testUser.setPassword("oldPassword");
    Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // when: user changes password with correct current password
    userService.changePassword(testUser.getId(), "oldPassword", "newPassword123");

    // then: password should be updated
    assertEquals("newPassword123", testUser.getPassword());
  }

  @Test
  public void changePassword_incorrectCurrentPassword_throwsException() {
    // given
    testUser.setPassword("correctPassword");
    Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // when + then user changes password with incorrect current password
    assertThrows(ResponseStatusException.class, () -> 
        userService.changePassword(testUser.getId(), "wrongPassword", "newPassword")
    );
  }

  @Test
  public void changePassword_emptyNewPassword_throwsException() {
    // given
    testUser.setPassword("correctPassword");
    Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // when + then user changes password to null
    assertThrows(ResponseStatusException.class, () -> 
        userService.changePassword(testUser.getId(), "correctPassword", "")
    );
  }

}
