package com.p2p.maze;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Game
 * <p>
 * Player in the maze game
 * First player join the game will become primary server
 * Second player join the game will become secondary server
 * In the event of primary or secondary server crash, the system should promote a new pair of primary/secondary servers
 */
public class Game implements GameInterface {

  public static final int CMD_GAME_STATE = 0;
  public static final int CMD_MOVE_WEST = 1;
  public static final int CMD_MOVE_SOUTH = 2;
  public static final int CMD_MOVE_EAST = 3;
  public static final int CMD_MOVE_NORTH = 4;
  public static final int CMD_EXIT = 9;

  private String playerId = null;
  private int portNumber;
  private GameState gameState = new GameState();

  public Game(int portNumber, String playerId) {
    this.portNumber = portNumber;
    this.playerId = playerId;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }


  /**
   * Contact tracker when player join the game, receive tracker state: n, k, primary, backup
   *
   * @param registry
   * @throws RemoteException
   * @throws NotBoundException
   */
  private void contactTracker(Registry registry) throws RemoteException, NotBoundException {
    TrackerInterface stub = (TrackerInterface) registry.lookup("Tracker");
    TrackerState trackerState = stub.register(playerId);
    System.out.println("Tracker response: " + trackerState.toString());
    gameState.setN(trackerState.getN());
    gameState.setK(trackerState.getK());
    gameState.setPrimary(trackerState.getPrimary());
    gameState.setBackup(trackerState.getBackup());
    System.out.println("Tracker response: " + gameState.toString());

  }

  private void init() {

  }

  private void run() {

  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Invalid input to start the game");
      return;
    }

    System.err.println("s1");
    int portNumber = Integer.parseInt(args[0]);
    String playerId = args[1];
    if (playerId == null || playerId.length() != 2) {
      System.err.println("Invalid player id");
      return;
    }

    try {
      System.err.println("s2");
      String serverIP = "localhost";
      Registry registry = LocateRegistry.getRegistry(serverIP, portNumber);

      Game game = new Game(portNumber, playerId);

      game.contactTracker(registry);

      game.init();

      // infinite loop that waits for user input and make a move
      game.run();

      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);

      // Bind the remote object's stub in the registry
      registry.bind(playerId, iGame);

      System.out.println("Player ready: " + playerId);

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      e.printStackTrace();
    }
  }

  @Override
  public void setPrimary(String primary) throws RemoteException {

  }

  @Override
  public void setBackup(String backup) throws RemoteException {

  }

  @Override
  public GameState initPlayer(String playerId) throws RemoteException {
    return null;
  }

  @Override
  public GameState getGameState() throws RemoteException {
    return null;
  }

  @Override
  public GameState move(int move) throws RemoteException {
    return null;
  }

  @Override
  public void exit(String playerId) throws RemoteException {

  }

  @Override
  public boolean ping() throws RemoteException {
    return false;
  }

  @Override
  public boolean syncGameState() throws RemoteException {
    return false;
  }

  @Override
  public void promoteToBackupServer(String playerId, GameState gameState) throws RemoteException {

  }
}
