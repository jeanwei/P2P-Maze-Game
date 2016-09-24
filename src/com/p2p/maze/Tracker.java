package com.p2p.maze;

import com.p2p.maze.utils.LogFormatter;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * Tracker
 * <p>
 * Tracker will have a well-known IP address and port number
 * Tracker knows the values of N and K.
 * In addition, tracker will inform new players of current Primary and Secondary servers
 */
public class Tracker implements TrackerInterface {

  private static final Logger LOGGER = Logger.getLogger(Tracker.class.getSimpleName());

  private int portNumber;

  private TrackerState trackerState = new TrackerState();

  public Tracker(int portNumber, int n, int k) {
    this.portNumber = portNumber;
    this.trackerState.setN(n);
    this.trackerState.setK(k);
  }

  @Override
  public synchronized TrackerState register(Player player) throws RemoteException {

    LOGGER.info(String.format("player %s, start: %s", player.getPlayerId(), trackerState));

    Player primary = trackerState.getPrimary();
    if (primary == null) {
      trackerState.setPrimary(player);

      LOGGER.info(String.format("player %s, as primary server", player.getPlayerId()));
      return trackerState;
    }

    LOGGER.info(String.format("player %s, end: %s", player.getPlayerId(), trackerState));

    return trackerState;
  }

  @Override
  public synchronized void updateServers(Player primaryServer, Player backupServer) throws RemoteException {
    trackerState.setPrimary(primaryServer);
    trackerState.setBackup(backupServer);

    LOGGER.info(String.format("updated trackerState: %s", trackerState));
  }

  @Override
  public synchronized TrackerState getTrackerState() throws RemoteException {
    LOGGER.info(String.format("retried trackerState: %s", trackerState));

    return trackerState;
  }

  public static void main(String args[]) {
    initLogger();

    if (args.length < 3) {
      LOGGER.warning("Invalid input to create tracker");
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

      LOGGER.info(String.format("Tracker ready. port: %d, N: %d, K: %d", portNumber, n, k));
    } catch (RemoteException e) {
      LOGGER.severe("Server exception: " + e.toString());
      e.printStackTrace();
    }
  }

  /**
   * Custom log format
   */
  private static void initLogger() {
    LOGGER.setUseParentHandlers(false);
    LogFormatter formatter = new LogFormatter();
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(formatter);
    LOGGER.addHandler(handler);
  }
}
