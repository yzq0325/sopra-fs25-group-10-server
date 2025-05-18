package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
(componentModel = "spring", uses = {UserRepository.class})
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);
  
  //User mappings
  @Mapping(source = "username", target = "username")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "bio", target = "bio")
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "userId", target = "userId")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "token", target = "token")
  UserGetDTO convertEntityToUserGetDTO(User user);

  // Game mappings
  @Mapping(source = "gameId", target = "gameId")
  @Mapping(source = "ownerId", target = "ownerId")
  @Mapping(source = "players", target = "players")
  @Mapping(source = "gameName", target = "gameName") 
  @Mapping(source = "playersNumber", target = "playersNumber")
  @Mapping(source = "time", target = "time") 
  @Mapping(source = "modeType", target = "modeType") 
  @Mapping(source = "password", target = "password")
  @Mapping(source = "gameCode", target = "gameCode")  
  Game convertGamePostDTOtoGameEntity(GamePostDTO gamePostDTO);

  @Mapping(source = "ownerId", target = "ownerId")
  @Mapping(source = "scoreBoard", target = "scoreBoard")
  @Mapping(source = "gameName", target = "gameName") 
  @Mapping(source = "playersNumber", target = "playersNumber")
  @Mapping(source = "realPlayersNumber", target = "realPlayersNumber")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "gameId", target = "gameId")
  @Mapping(source = "gameCode", target = "gameCode")  
  @Mapping(source = "endTime", target = "endTime")
  @Mapping(source = "finalScore", target = "finalScore")
  @Mapping(source = "difficulty", target = "difficulty")
  GameGetDTO convertGameEntityToGameGetDTO(Game game);

  // Profile mappings
  // @Mapping(source = "username", target = "username")
  // @Mapping(source = "avatar", target = "avatar")
  // @Mapping(source = "email", target = "email")
  // @Mapping(source = "bio", target = "bio")
  // User convertUserProfileDTOtoEntity(UserProfileDTO userProfileDTO);

}
