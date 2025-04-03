
```
├── Application.java
├── constant
│   ├── GameStatus.java
│   ├── GameType.java
│   ├── RoleType.java
│   ├── UserStatus.java
│   ├── SessionStatus.java
│   ├── TeamType.java
│   └── LeaderboardType.java
│
├── controller
│   ├── GameController.java
│   ├── UserController.java
│   ├── LeaderboardController.java
│   ├── TeamController.java
│   ├── SessionController.java
│   ├── GameHistoryController.java
│   └── ClueController.java
│
├── entity
│   ├── Game.java
│   ├── User.java
│   ├── Team.java
│   ├── GameSession.java
│   ├── CombatGame.java
│   ├── SoloGame.java
│   ├── GameHistory.java
│   ├── GameResult.java
│   ├── Leaderboard.java
│   ├── LearningProgress.java
│   ├── CountryProgress.java
│   ├── ClueGenerator.java
│   └── CountryProvider.java
│
├── exceptions
│   ├── GlobalExceptionAdvice.java
│   ├── UserNotFoundException.java
│   ├── GameNotFoundException.java
│   ├── SessionNotFoundException.java
│   └── InvalidGameActionException.java
│
├── repository
│   ├── GameRepository.java
│   ├── UserRepository.java
│   ├── TeamRepository.java
│   ├── GameSessionRepository.java
│   ├── GameHistoryRepository.java
│   ├── GameResultRepository.java
│   ├── LeaderboardRepository.java
│   ├── LearningProgressRepository.java
│   ├── CountryProgressRepository.java
│   ├── ClueRepository.java
│   └── CountryProviderRepository.java
│
├── rest
│   ├── dto
│   │   ├── GameGetDTO.java
│   │   ├── GamePostDTO.java
│   │   ├── UserGetDTO.java
│   │   ├── UserPasswordDTO.java
│   │   ├── UserPostDTO.java
│   │   ├── TeamGetDTO.java
│   │   ├── GameSessionDTO.java
│   │   ├── GameHistoryDTO.java
│   │   ├── GameResultDTO.java
│   │   ├── LeaderboardDTO.java
│   │   ├── LearningProgressDTO.java
│   │   ├── CountryProgressDTO.java
│   │   ├── ClueDTO.java
│   │   └── CountryProviderDTO.java
│   └── mapper
│       ├── DTOMapper.java
│       ├── GameMapper.java
│       ├── UserMapper.java
│       ├── TeamMapper.java
│       ├── GameSessionMapper.java
│       ├── GameHistoryMapper.java
│       ├── LeaderboardMapper.java
│       ├── LearningProgressMapper.java
│       ├── CountryProgressMapper.java
│       ├── ClueMapper.java
│       └── CountryProviderMapper.java
│
└── service
    ├── GameService.java
    ├── UserService.java
    ├── TeamService.java
    ├── GameSessionService.java
    ├── GameHistoryService.java
    ├── GameResultService.java
    ├── LeaderboardService.java
    ├── LearningProgressService.java
    ├── CountryProgressService.java
    ├── ClueService.java
    └── CountryProviderService.java
```