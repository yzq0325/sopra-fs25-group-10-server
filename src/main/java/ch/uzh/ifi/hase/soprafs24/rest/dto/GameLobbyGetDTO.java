package ch.uzh.ifi.hase.soprafs24.rest.dto;


public class GameLobbyGetDTO {

    private String gameName;

    private String lockType;

    private int playersNumber;

    private int realPlayersNumber;

    public String getGameName() {
        return gameName;
      }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getPlayersNumber() {
        return playersNumber;
      }
    
    public void setPlayersNumber(int playersNumber) {
        this.playersNumber = playersNumber;
      }     
    
    public String getLockType() {
        return lockType;
    }
    
    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public int getPlayerNumbers() {
        return playersNumber;
      }
    
    public void setPlayerNumbers(int playersNumber) {
        this.playersNumber = playersNumber;
      }     

      public int getRealPlayersNumber() {
        return realPlayersNumber;
      }
    
    public void setRealPlayersNumber(int realPlayersNumber) {
        this.realPlayersNumber = realPlayersNumber;
      }   
    
}

