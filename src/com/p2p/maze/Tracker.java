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
    private TrackerState trackerState = new TrackerState();
    public String portNumber;

    public Tracker(String portNumber, int n, int k) {
        this.portNumber = portNumber;
        this.trackerState.n = n;
        this.trackerState.k = k;
    }



  public String getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public TrackerState getTrackerState() {
      return trackerState;
    }

    public void setTrackerState(TrackerState trackerState) {
      this.trackerState = trackerState;
    }

  public TrackerState register(String playerId) throws RemoteException {
        // if no player, set to primary

    System.err.println("register playerId:" + playerId);
    System.err.println(trackerState.toString());
    String primary = trackerState.getPrimary();

    if (primary == null || primary.isEmpty()){
      trackerState.setPrimary(playerId);
      return trackerState;
    }

        // if has primary only, set to backup
    String backup = trackerState.getBackup();
    if (backup == null || backup.isEmpty()){
      trackerState.setBackup(backup);
    }
        // return n, k, primary and backup
    System.err.println("after register playerId:" + playerId);
    System.err.println(trackerState.toString());

        return trackerState;
    }

  @Override
  public void setPrimaryServer(String primaryServer) throws RemoteException {
    trackerState.setPrimary(primaryServer);
  }

  @Override
  public void setBackupServer(String backupServer) throws RemoteException {
    trackerState.setBackup(backupServer);

  }

  public static void main(String args[]) {

        if (args.length < 3) {
            System.err.println("Invalid input to create tracker");
            return;
        }

        try {
            String portNumber = args[0];
            int n = Integer.parseInt(args[1]);
            int k = Integer.parseInt(args[2]);

            Tracker tracker = new Tracker(portNumber, n, k);
            TrackerInterface stub = (TrackerInterface) UnicastRemoteObject.exportObject(tracker, 0);

            // Bind the remote object's stub in the registry
          String serverIP = "localhost";
          Registry registry = LocateRegistry.getRegistry(serverIP, Integer.parseInt(portNumber));
            registry.bind("Tracker", stub);


            System.err.println("Tracker Server ready");
            System.err.println("portNumber: " + portNumber);
            System.err.println("n: " + n);
            System.err.println("k: " + k);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
