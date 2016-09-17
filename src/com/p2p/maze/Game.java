package com.p2p.maze;

import java.net.InetAddress;
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

  private Player player;
  private Registry serverRegistry;

  public String playerId = null;
  public int portNumber;

  private GameState gameState;

  private Game() {

  }

  public Game(String playerId, String localServerIp, int portNumber) {
    this.player = new Player(playerId, localServerIp, portNumber);
  }

  public Game(int portNumber, int n, int k, String playerId) {
    this.portNumber = portNumber;
    this.gameState.n = n;
    this.gameState.k = k;
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


  private void contactTracker(Registry registry) throws RemoteException, NotBoundException {
    TrackerInterface stub = (TrackerInterface) registry.lookup("Tracker");
    TrackerState trackerState = stub.register(player);
    System.out.println("Tracker response: " + trackerState.toString());
    gameState = new GameState(trackerState.getN(), trackerState.getK());
    gameState.setPrimary(trackerState.getPrimary());
    gameState.setBackup(trackerState.getBackup());
    System.out.println("Tracker response: " + gameState.toString());

  }

  private void initPosition() throws RemoteException, NotBoundException {

    Player primaryPlayer = gameState.getPrimary();

    primaryPlayer = primaryPlayer != null ? primaryPlayer : gameState.getBackup();

    if (primaryPlayer != null && !primaryPlayer.getPlayerId().equals(player.getPlayerId())){
      serverRegistry = LocateRegistry.getRegistry(primaryPlayer.getIp(), primaryPlayer.getPortNumber());
      GameInterface stub = (GameInterface) serverRegistry.lookup(primaryPlayer.getPlayerId());
      stub.initPlayer(player);
    }  else {
      initPlayer(player);
    }
  }

  private void run() {

  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Invalid input to start the game");
      return;
    }

    System.err.println("s1");
    String trackerIpAddress = args[0];
    int portNumber = Integer.parseInt(args[1]);
    String playerId = args[2];
    if (playerId == null || playerId.length() != 2) {
      System.err.println("Invalid player id");
      return;
    }
    try {
      System.err.println("s2");
      InetAddress IP=InetAddress.getLocalHost();
      System.out.println("IP of my system is := "+IP.getHostAddress());
      String localServerIp = IP.getHostAddress();
//      localServerIp = "localhost";

      Game game = new Game(playerId, localServerIp, portNumber);

      // get Tracker registry
      Registry registry = LocateRegistry.getRegistry(trackerIpAddress, portNumber);

      // contact tracker and get tracker state: n, k, primary, backup
      game.contactTracker(registry);

      // generate random position and call move (repeat till a move is valid)
      game.initPosition();

      // infinite loop that waits for user input and make a move
      game.run();

      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);

      // Bind the remote object's stub in the registry

      Registry playerRegistry = LocateRegistry.getRegistry(localServerIp, portNumber);
      playerRegistry.rebind(playerId, iGame);

      System.err.println("Player ready: " + playerId);

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
  public GameState initPlayer(Player player) throws RemoteException {
    gameState.addNewPlayer(player);
    return gameState;
  }

  @Override
  public GameState getGameState() throws RemoteException {
    return gameState;
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
