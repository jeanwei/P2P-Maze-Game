package com.p2p.maze;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * GameState map - Game State
 *
 */
public class GameState extends TrackerState {
  private final String TREASURE_VALUE = "*";
  private String[][] maze;

  // storing list of player
  private Map<String, Integer> scoreList;
  private Map<String, Position> playerPosition;
  private Map<String, Position> treasurePosition;

  public GameState(TrackerState trackerState){
    this.n = trackerState.getN();
    this.k = trackerState.getK();
    this.primary = trackerState.getPrimary();
    this.backup = trackerState.getBackup();
  }

  public void initGameState(){
    this.maze = new String[n][n];
    this.scoreList = new HashMap<>();
    this.playerPosition = new HashMap<>();
    this.treasurePosition = new HashMap<>();
    Random random = new Random();
    for(int i = 0; i < k; i++){
      if(!this.addTreasure(random.nextInt(n), random.nextInt(n))){
        i--;
      }
    }
  }

  public synchronized void addNewPlayer(Player newPlayer) {
    Random random = new Random();
    while(!this.add(random.nextInt(n), random.nextInt(n), newPlayer)){

    }
  }

  private synchronized boolean addTreasure(int newPositionX, int newPositionY) {
    if(maze[newPositionX][newPositionY] != null){
      return false;
    } else {
      maze[newPositionX][newPositionY] = TREASURE_VALUE;
      treasurePosition.put(newPositionX + "X" + newPositionY, new Position(newPositionX, newPositionY));
    }
    return true;
  }

  public synchronized boolean move(Player player,  int horizontal, int vertical) {
    String playerID = player.getPlayerId();
    Position curPosition = this.playerPosition.get(playerID);
    int newPositionX = curPosition.posX + horizontal;
    int newPositionY = curPosition.posY + vertical;
    int oldPositionX = curPosition.posX;
    int oldPositionY = curPosition.posY;
    if(newPositionX < 0 || newPositionX > n-1
        || newPositionY < 0 || newPositionY > n-1)
      return false;
    if(this.add(newPositionX, newPositionY, player))
      this.remove(oldPositionX, oldPositionY);
    return false;
  }

  private synchronized void collectTreasureAndUpdateScore(String playerId, int positionX, int positionY) {
    Integer score = scoreList.get(playerId);
    score = score == null ? 1 : score++;
    scoreList.put(playerId, score);
    Random random = new Random();
    while(!this.addTreasure(random.nextInt(n), random.nextInt(n))){

    }
    remove(positionX, positionY);
  }

  private synchronized boolean add(int newPositionX, int newPositionY, Player player) {
    String playerID =  player.getPlayerId();
    if(maze[newPositionX][newPositionY] != null){
      if (TREASURE_VALUE.equals(maze[newPositionX][newPositionY])){
        collectTreasureAndUpdateScore(playerID, newPositionX, newPositionY);
      } else {
        return false;
      }
    }

    maze[newPositionX][newPositionY] = playerID;
    if(playerPosition.containsKey( playerID))
    {
      Position position = playerPosition.get( playerID);
      position.posX = newPositionX;
      position.posY = newPositionY;
    }
    else
      playerPosition.put(playerID, new Position(newPositionX, newPositionY));
    return true;
  }

  private void remove(int oldPositionX , int oldPositionY) {
    maze[oldPositionX][oldPositionY] = null;
  }

  public void exitPlayer(String playerID)
  {
    Position pos = this.playerPosition.get(playerID);
    this.remove(pos.posX, pos.posY);
    this.playerPosition.remove(playerID);
  }

  public Map<String, Integer> getScoreList() {
    return scoreList;
  }

  public Map<String, Position> getPlayerPosition() {
    return playerPosition;
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("GameState{" +
        "n=" + n +
        ", k=" + k +
        ", primary='" + primary + '\'' +
        ", backup='" + backup + '\'' +
        ", playerPosition='" + Arrays.asList(playerPosition) + '\'' +
        ", treasurePosition='" + Arrays.asList(treasurePosition) + '\'' +
        '}');

    String newLineStr = System.getProperty("line.separator");
    stringBuffer.append(newLineStr);
    stringBuffer.append("maze=" + newLineStr);
    if (maze == null){
      stringBuffer.append("null");
    } else {
      for(int i = 0; i < n+1; i++){

        for(int j = 0; j<n+1; j++){
          if (j==0 && i ==0){
            stringBuffer.append("    ");
            continue;
          } else if (i == 0){
            stringBuffer.append((j-1) + "   ");
            continue;
          } else if (j == 0){
            stringBuffer.append((i-1) + "   ");
            continue;
          }
          String value = maze[i-1][j-1];
          if (TREASURE_VALUE.equals(value)){
            stringBuffer.append(TREASURE_VALUE + "   ");
          } else if (value != null){
            stringBuffer.append(value + "  ");
          } else {
            stringBuffer.append("    ");
          }
        }
        stringBuffer.append(newLineStr);
      }
    }

    return stringBuffer.toString();
  }
}
