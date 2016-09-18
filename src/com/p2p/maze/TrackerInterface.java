package com.p2p.maze;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote Interface for Tracker class
 *
 */
public interface TrackerInterface extends Remote {
  /**
   * Inform tracker a new player joins the game
   * @param player new player
   * @return current TrackerState (n, k, primary, backup)
   * @throws RemoteException
     */
  public TrackerState register(Player player) throws RemoteException;

  /**
   * Inform tracker new servers in case a primary/backup server crashes
   * @param primaryServer new primary server
   * @param backupServer new backup server
   * @throws RemoteException
   */
  public void updateServers(Player primaryServer, Player backupServer) throws RemoteException;

  public TrackerState getTrackerState() throws RemoteException;
}
