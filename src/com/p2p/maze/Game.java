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

  public enum Command {
    GAME_STATE(0),
    MOVE_WEST(1),
    MOVE_SOUTH(2),
    MOVE_EAST(3),
    MOVE_NORTH(4),
    EXIT(9);

    private final int id;
    Command(int id) { this.id = id; }
    public int getValue() { return id; }
  }

  private Player player;
  private Registry serverRegistry;
  private GameState gameState;

  public Game(String playerId, String localServerIp, int portNumber) {
    this.player = new Player(playerId, localServerIp, portNumber);
  }

  /**
   * Contact tracker when player join the game, receive tracker state: n, k, primary, backup
   *
   * @param registry rmiRegistry
   * @throws RemoteException
   * @throws NotBoundException
   */
  private void contactTracker(Registry registry) throws RemoteException, NotBoundException {
    TrackerInterface stub = (TrackerInterface) registry.lookup("Tracker");
    TrackerState trackerState = stub.register(player);
    System.out.println("Tracker response: " + trackerState.toString());
    gameState = new GameState(trackerState.getN(), trackerState.getK());
    gameState.setPrimary(trackerState.getPrimary());
    gameState.setBackup(trackerState.getBackup());
    System.out.println("Tracker response: " + gameState.toString());

  }

  private void init() throws RemoteException, NotBoundException {

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

      Registry trackerRegistry = LocateRegistry.getRegistry(trackerIpAddress, portNumber);

      String playerIpAddress = InetAddress.getLocalHost().getHostAddress();
      System.out.println("IP of my system is := "+ playerIpAddress);

      Game game = new Game(playerId, playerIpAddress, portNumber);

      game.contactTracker(trackerRegistry);

      game.init();

      game.run();

      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);

      Registry playerRegistry = LocateRegistry.getRegistry(playerIpAddress, portNumber);
      playerRegistry.rebind(playerId, iGame);

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
  public GameState initPlayer(Player player) throws RemoteException {
    gameState.addNewPlayer(player);
    return gameState;
  }

  @Override
  public GameState getGameState() throws RemoteException {
    return gameState;
  }

  @Override
  public GameState move(Command move) throws RemoteException {
    switch (move) {
      case MOVE_WEST:
        break;

      case MOVE_SOUTH:
        break;

      case MOVE_EAST:
        break;

      case MOVE_NORTH:
        break;

      default:
        System.err.println("Unrecognized move command");
    }
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
