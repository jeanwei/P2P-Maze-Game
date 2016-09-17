package com.p2p.maze;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Tracker
 * <p>
 * Tracker will have a well-known IP address and port number
 * Tracker knows the values of N and K.
 * In addition, tracker will inform new players of current Primary and Secondary servers
 */
public class Tracker implements TrackerInterface {

  private int portNumber;

  private TrackerState trackerState = new TrackerState();

  public Tracker(int portNumber, int n, int k) {
    this.portNumber = portNumber;
    this.trackerState.n = n;
    this.trackerState.k = k;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public TrackerState getTrackerState() {
    return trackerState;
  }

  public TrackerState register(Player player) throws RemoteException {

    System.err.println("register playerId:" + player);
    System.err.println(trackerState.toString());

    Player primary = trackerState.getPrimary();
    if (primary == null) {
      trackerState.setPrimary(player);
      return trackerState;
    }

    Player backup = trackerState.getBackup();
    if (backup == null) {
      trackerState.setBackup(player);
    }

    System.err.println("after register playerId:" + player);
    System.err.println(trackerState.toString());

    return trackerState;
  }

  @Override
  public void setPrimaryServer(Player primaryServer) throws RemoteException {
    trackerState.setPrimary(primaryServer);
  }

  @Override
  public void setBackupServer(Player backupServer) throws RemoteException {
    trackerState.setBackup(backupServer);

  }

  public static void main(String args[]) {

    if (args.length < 3) {
      System.err.println("Invalid input to create tracker");
      return;
    }

    try {
      int portNumber = Integer.parseInt(args[0]);
      int n = Integer.parseInt(args[1]);
      int k = Integer.parseInt(args[2]);

      Tracker tracker = new Tracker(portNumber, n, k);
      TrackerInterface stub = (TrackerInterface) UnicastRemoteObject.exportObject(tracker, 0);

      // Bind the remote object's stub in the registry
      String serverIP = "localhost";
      Registry registry = LocateRegistry.getRegistry(serverIP, portNumber);
      registry.rebind("Tracker", stub);

      System.err.println("Tracker Server ready");
      System.err.println("portNumber: " + portNumber);
      System.err.println("n: " + n);
      System.err.println("k: " + k);
    } catch (RemoteException e) {
      System.err.println("Server exception: " + e.toString());
      e.printStackTrace();
    }
  }
}
