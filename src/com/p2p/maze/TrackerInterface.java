package com.p2p.maze;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 *
 */
public interface TrackerInterface extends Remote {
//    public int getN() throws RemoteException;
//
//    public void setN(int n) throws RemoteException;
//
//    public int getK() throws RemoteException;
//
//    public void setK(int k) throws RemoteException;
//
//    public String getPortNumber() throws RemoteException;
//
//    public void setPortNumber(String portNumber) throws RemoteException;

  public TrackerState register(Player player) throws RemoteException;

  public void setPrimaryServer(Player primaryServer) throws RemoteException;

  public void setBackupServer(Player backupServer) throws RemoteException;
}
