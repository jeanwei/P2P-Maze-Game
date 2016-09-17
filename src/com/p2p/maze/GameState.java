package com.p2p.maze;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * GameState map - Game State
 *
 */
public class GameState extends TrackerState{
  private String[][] maze;

  // storing list of player
  private Map<String, Integer> scoreList;
  private Map<String, Position> playerPosition;

  public GameState(int N, int K)
  {
    this.n = N;
    this.k = K;
    this.maze = new String[N][N];
    this.scoreList = new HashMap<String, Integer>();
    this.playerPosition = new HashMap<String, Position>();
  }

  public void addNewPlayer(Player newPlayer){
    Random random = new Random();
    while(!this.add(random.nextInt(n), random.nextInt(n), newPlayer)){
    }
  }

  public boolean move(Player player,  int horizontal, int vertical)
  {
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

  private boolean add(int newPositionX, int newPositionY, Player player)
  {
    String playerID =  player.getPlayerId();
    if(maze[newPositionX][newPositionY] != null)
      return false;
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

  private void remove(int oldPositionX , int oldPositionY)
  {
    maze[oldPositionX][oldPositionY] = null;
  }

  public void exitPlayer(String playerID)
  {
    Position pos = this.playerPosition.get(playerID);
    this.remove(pos.posX, pos.posY);
    this.playerPosition.remove(playerID);
  }

  @Override
  public String toString() {
    return "GameState{" +
        "n=" + n +
        ", k=" + k +
        ", primary='" + primary + '\'' +
        ", backup='" + backup + '\'' +
        ", maze='" + maze + '\'' +
        '}';
  }

}
