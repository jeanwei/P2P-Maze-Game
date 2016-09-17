package com.p2p.maze;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 *
 */
public interface GameInterface extends Remote {

  public void setPrimary(String primary) throws RemoteException;

  public void setBackup(String backup) throws RemoteException;

  /**
   * [Primary] Inform primary server the new player
   * if primary does not contain backup server, this player will be promoted
   *
   * @param player backup server to be
   * @return game state copied to backup server
   */
  public GameState initPlayer(Player player) throws RemoteException;

  /**
   * [Primary] Get latest game state from primary server
   *
   * @return game state (user input: 0)
   */
  public GameState getGameState() throws RemoteException;

  /**
   * [Primary] Player move in the maze
   *
   * @param move movement direction (1: west, 2: south, 3: east, 4: north)
   * @return Game state
   */
  public GameState move(int move) throws RemoteException;

  /**
   * [Primary] Player exit the game
   *
   * @param playerId player (user input: 9)
   */
  public void exit(String playerId) throws RemoteException;

  /**
   * [Primary <-> Backup] Ping each other every 2sec to check alive
   *
   * @return true if alive
   */
  public boolean ping() throws RemoteException;

  /**
   * [Primary -> Backup] Sync game state between primary and backup servers
   *
   * @return true if sync succeeds
   */
  public boolean syncGameState() throws RemoteException;

  /**
   * [Primary -> Backup] Promote new backup server when old one exit
   *
   * @param playerId  new Primary Server ID
   * @param gameState game state
   */
  public void promoteToBackupServer(String playerId, GameState gameState) throws RemoteException;
}
