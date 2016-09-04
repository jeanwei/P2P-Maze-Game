package com.p2p.maze;

import java.rmi.RemoteException;

/**
 * Created by ufinity on 9/4/2016.
 */
public interface GameState extends TrackerInfo {
  public String getPlayerId() throws RemoteException;

  public void setPlayerId(String playerId) throws RemoteException;
}
