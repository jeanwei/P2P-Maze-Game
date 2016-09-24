package com.p2p.maze;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 *
 */
public interface GameInterface extends Remote {
  /**
   * [Primary] Inform primary server the new player
   * if primary does not contain backup server, this player will be promoted
   *
   * @param player backup server to be
   * @return game state copied to backup server
   */
  public GameState initPlayer(Player player) throws RemoteException, NotBoundException;

  /**
   * [Primary] Player move in the maze
   *
   * @param move movement direction (1: west, 2: south, 3: east, 4: north)
   * @return Game state
   */
  public GameState executeCommand(Player player, Game.Command move) throws RemoteException, NotBoundException;

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
  public boolean syncGameState(GameState gameState) throws RemoteException;

  /**
   * [Primary -> new player] Promote new backup server when old one exit
   *
   * @param gameState game state
   */
  public void promoteToBackupServer(GameState gameState) throws RemoteException;

  /**
   * [Primary -> player] Notify player about new primary (broadcast)
   * @param primary new primary
   * @throws RemoteException
     */
  public void setPrimary(Player primary) throws RemoteException;
}
