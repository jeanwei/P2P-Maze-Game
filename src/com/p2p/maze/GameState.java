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
  private Map<String, Player> playerMap;

  public GameState(TrackerState trackerState){
    this.n = trackerState.getN();
    this.k = trackerState.getK();
    this.primary = trackerState.getPrimary();
    this.backup = trackerState.getBackup();
  }

  public void initGameState(){
    this.maze = new String[n][n];
    this.playerMap = new HashMap<String, Player>();
    Random random = new Random();
    for(int i = 0; i < k; i++){
      if(!this.addTreasure(random.nextInt(n), random.nextInt(n))){
        i--;
      }
    }
  }

  public synchronized void addNewPlayer(Player newPlayer) {
    Random random = new Random();
    this.playerMap.put(newPlayer.getPlayerId(), newPlayer);
    while(!this.add(random.nextInt(n), random.nextInt(n), newPlayer)){

    }
  }

  private synchronized boolean addTreasure(int newPositionX, int newPositionY) {
    if(maze[newPositionX][newPositionY] != null){
      return false;
    } else {
      maze[newPositionX][newPositionY] = TREASURE_VALUE;
    }
    return true;
  }

  public synchronized boolean move(Player player,  int horizontal, int vertical) {
    String playerID = player.getPlayerId();
    player = this.playerMap.get(playerID);
    int oldPositionX = player.getPosition().posX;
    int oldPositionY = player.getPosition().posY;
    int newPositionX = player.getPosition().posX + horizontal;
    int newPositionY = player.getPosition().posY + vertical;
    if(newPositionX < 0 || newPositionX > n-1
        || newPositionY < 0 || newPositionY > n-1)
      return false;
    if(this.add(newPositionX, newPositionY, player)){
      this.remove(oldPositionX, oldPositionY);
      return true;
    }
    return false;
  }

  private synchronized void collectTreasureAndUpdateScore(Player player, int positionX, int positionY) {
    Integer score = player.getScore();
    player.setScore(score++);
    Random random = new Random();
    while(!this.addTreasure(random.nextInt(n), random.nextInt(n))){

    }
    this.remove(positionX, positionY);
  }

  private synchronized boolean add(int newPositionX, int newPositionY, Player player) {
    String playerID =  player.getPlayerId();
    if(maze[newPositionX][newPositionY] != null){
      if (TREASURE_VALUE.equals(maze[newPositionX][newPositionY])){
        collectTreasureAndUpdateScore(player, newPositionX, newPositionY);
      } else {
        return false;
      }
    }

    maze[newPositionX][newPositionY] = playerID;
    playerMap.get(playerID).getPosition().posX = newPositionX;
    playerMap.get(playerID).getPosition().posY = newPositionY;
    return true;
  }

  private void remove(int oldPositionX , int oldPositionY) {
    maze[oldPositionX][oldPositionY] = null;
  }

  public synchronized boolean exitPlayer(Player player){
    String playerId = player.getPlayerId();
    player = this.playerMap.get(playerId);
    this.remove(player.getPosition().posX, player.getPosition().posY);
    this.playerMap.remove(playerId);
    return true;
  }

  public Player getPlayer(String playerId) {
    return this.playerMap.get(playerId);
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("GameState{" +
        "n=" + n +
        ", k=" + k +
        ", primary='" + primary + '\'' +
        ", backup='" + backup + '\'' +
        ", playerPosition='" + Arrays.asList(playerMap) + '\'' +
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
