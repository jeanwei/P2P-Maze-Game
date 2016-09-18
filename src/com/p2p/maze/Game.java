package com.p2p.maze;

import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

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
   * Check if current player is a primary server
   *
   * @return true if is primary
   */
  private boolean isPrimary() {
    Player primaryServer = gameState.getPrimary();
    return primaryServer != null && primaryServer.getPlayerId().equals(player.getPlayerId());
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
    gameState = new GameState(trackerState);
    System.out.println("contactTracker initial GameState: " + gameState.toString());
  }

  /**
   * Get initial game state
   * if current player is primary server, create game state
   * otherwise, get latest game state from primary server
   *
   * @throws RemoteException
   * @throws NotBoundException
     */
  private void init() throws RemoteException, NotBoundException {
    Player server = gameState.getPrimary();

    if (isPrimary()) {
      gameState.initGameState();
      gameState.addNewPlayer(player);
      updatePlayer();
    } else if (server != null) {
      serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
      GameInterface stub = (GameInterface) serverRegistry.lookup(server.getPlayerId());
      gameState = stub.initPlayer(player);
      updatePlayer();
    } else {
      System.err.println("Primary server is not found!");
    }

    System.out.println("game state after init: " + gameState.toString());
    System.out.println("player after init: " + player.toString());
  }

  private void updateGameState(GameState gameState){
    this.gameState = gameState;
    updatePlayer();
    refreshGameStateUI();
  }

  private void refreshGameStateUI(){
    System.out.println("game state after refreshing: " + gameState.toString());
    System.out.println("player after refreshing: " + player.toString());
  }

  /**
   * Update player position and score
   */
  private void updatePlayer(){
    String playerId = player.getPlayerId();
    Position position = gameState.getPlayerPosition().get(playerId);
    if (position != null){
      player.setPosition(position);
    }

    Integer score = gameState.getScoreList().get(playerId);
    if (score != null){
      player.setScore(score);
    }
  }

  private void run() {
    System.out.println("Player is playing: " + player.getPlayerId());
    Scanner scanner = new Scanner(System.in);
    while (true) {

      String input = scanner.nextLine();
      if (input == null || input.length() == 0){
        continue;
      }

      System.out.println("input : " + input);
      System.out.println("-----------\n");
      Player primaryPlayer = gameState.getPrimary();
      try {
        serverRegistry = LocateRegistry.getRegistry(primaryPlayer.getIp(), primaryPlayer.getPortNumber());
        GameInterface stub = (GameInterface) serverRegistry.lookup(primaryPlayer.getPlayerId());
        switch (input.charAt(0)){

          case '0':
            updateGameState(stub.executeCommand(player, Command.GAME_STATE));
            break;
          case '1':
            updateGameState(stub.executeCommand(player, Command.MOVE_WEST));
            break;
          case '2':
            updateGameState(stub.executeCommand(player, Command.MOVE_SOUTH));
            break;
          case '3':
            updateGameState(stub.executeCommand(player, Command.MOVE_EAST));
            break;
          case '4':
            updateGameState(stub.executeCommand(player, Command.MOVE_NORTH));
            break;
          case '9':
            stub.executeCommand(player, Command.EXIT);
            System.out.println("Exit!");
            System.exit(0);
            break;

        }
      } catch (RemoteException | NotBoundException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Invalid input to start the game");
      return;
    }

    String trackerIpAddress = args[0];
    int portNumber = Integer.parseInt(args[1]);
    String playerId = args[2];
    System.out.println("Game start -----------------------\n");
    System.out.print(String.format("Tracker IP: %s, port number: %d, playerId: %s", trackerIpAddress, portNumber, playerId));
    if (playerId == null || playerId.length() != 2) {
      System.err.println("Invalid player id");
      return;
    }

    try {
      System.out.println("Game init -----------------------\n");

      Registry trackerRegistry = LocateRegistry.getRegistry(trackerIpAddress, portNumber);

      String playerIpAddress = InetAddress.getLocalHost().getHostAddress();
      System.out.println("Player IP: "+ playerIpAddress);

      Game game = new Game(playerId, playerIpAddress, portNumber);

      game.contactTracker(trackerRegistry);

      game.init();

      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);

      Registry playerRegistry = LocateRegistry.getRegistry(playerIpAddress, portNumber);
      playerRegistry.rebind(playerId, iGame);

      System.out.println("Player ready: " + playerId);

      // infinite loop that waits for user input and make a move
      game.run();

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
  public GameState initPlayer(Player player) throws RemoteException, NotBoundException {
    gameState.addNewPlayer(player);
    refreshGameStateUI();
    Player backupServer = gameState.getBackup();
    if (!player.getPlayerId().equals(backupServer.getPlayerId())){
      notifyBackup();
    }
    return gameState;
  }

  @Override
  public GameState getGameState() throws RemoteException {
    return gameState;
  }

  @Override
  public GameState executeCommand(Player player, Command move) throws RemoteException, NotBoundException {
    boolean updated = false;
    switch (move) {
      case GAME_STATE:
        break;

      case MOVE_WEST:
        updated = gameState.move(player, 0, -1);
        break;

      case MOVE_SOUTH:
        updated = gameState.move(player, 1, 0);
        break;

      case MOVE_EAST:
        updated = gameState.move(player, 0, 1);
        break;

      case MOVE_NORTH:
        updated =  gameState.move(player, -1, 0);
        break;

      case EXIT:
        updated =  gameState.exitPlayer(player);
        break;

      default:
        System.err.println("Unrecognized command");
    }
    if (updated){
      refreshGameStateUI();
      notifyBackup();
    }
    return gameState;
  }

  private void notifyBackup() throws RemoteException, NotBoundException {
    Player backupServer = gameState.getBackup();
    if (backupServer != null && !backupServer.getPlayerId().equals(player.getPlayerId())){
      Registry backupRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
      GameInterface stub = (GameInterface) backupRegistry.lookup(backupServer.getPlayerId());
      stub.syncGameState(gameState);
    }
  }

  @Override
  public boolean exit(Player player) throws RemoteException {
    gameState.exitPlayer(player);
    return true;
  }

  @Override
  public boolean ping() throws RemoteException {
    return false;
  }

  @Override
  public boolean syncGameState(GameState gameState) throws RemoteException {
    updateGameState(gameState);
    return false;
  }

  @Override
  public void promoteToBackupServer(String playerId, GameState gameState) throws RemoteException {

  }
}
