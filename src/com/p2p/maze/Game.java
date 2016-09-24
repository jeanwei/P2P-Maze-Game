package com.p2p.maze;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Game
 * <p>
 * Player in the maze game
 * First player join the game will become primary server
 * Second player join the game will become secondary server
 * In the event of primary or secondary server crash, the system should promote a new pair of primary/secondary servers
 */
public class Game implements GameInterface {
  private Object lock = new Object();

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

  private static Player player;
  private static Registry serverRegistry;
  private static Registry trackerRegistry;
   private static GameInterface serverGameInterface;
  private static GameState gameState;
  private static boolean timerStarted = false;

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

  private boolean isBackup() {
    Player backupServer = gameState.getBackup();
    return backupServer != null && backupServer.getPlayerId().equals(player.getPlayerId());
  }

  /**
   * Contact tracker when player join the game, receive tracker state: n, k, primary, backup
   *
   * @param registry rmiRegistry
   * @throws RemoteException
   * @throws NotBoundException
   */
  private void contactTracker(Registry registry) throws RemoteException, NotBoundException {
    trackerRegistry = registry;

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
  private void init() throws RemoteException, NotBoundException, InterruptedException {
    Player server = gameState.getPrimary();

    if (isPrimary()) {
      gameState.initGameState();
      gameState.addNewPlayer(player);
      updatePlayer();
    } else if (server != null) {

      System.err.println("initial serverGameInterface: " + server.getPlayerId());

        while(serverGameInterface == null){
          try {
            serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
            serverGameInterface = (GameInterface) serverRegistry.lookup(server.getPlayerId());
          } catch (Exception e){
            System.err.println("initial serverGameInterface error: " + server.getPlayerId());
            e.printStackTrace();
          }
        }

      System.err.println("initial connection to server: " + server.getPlayerId());

        while(true)
        {

        try{
          if (connectToAddPlayer(server)){
            break;
          }
        } catch (Exception e){
          System.out.println("initial connection error");
          TrackerInterface stub = (TrackerInterface) trackerRegistry.lookup("Tracker");
          TrackerState trackerState = stub.getTrackerState();
          gameState = new GameState(trackerState);
          gameState.setBackup(trackerState.getBackup());
          System.out.println("contactTracker reconnected GameState: " + gameState.toString());
          server = gameState.getPrimary();
          serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
          serverGameInterface = (GameInterface) serverRegistry.lookup(server.getPlayerId());
        }
        System.err.println(LocalDateTime.now() + " retry to connect to server after 1s: " + server.getPlayerId());
        Thread.sleep(1000); // sleep for 2000ms and try again
        }

      updatePlayer();
    } else {
      System.err.println("Primary server is not found!");
    }

    System.out.println("game state after init: " + gameState.toString());
    System.out.println("player after init: " + player.toString());
  }

  private boolean connectToAddPlayer(Player server) throws RemoteException, NotBoundException, InterruptedException {
    while(true) // only stop when successfully add new player inside the map
    {
      gameState = serverGameInterface.initPlayer(player);
      // found its own player id inside the map, then add player success, then loop
      if(gameState.getPlayer(player.getPlayerId()) != null){
        break;
      }
      System.err.println(LocalDateTime.now() + "retry to connect to server after 400ms:  " + server.getPlayerId());
      Thread.sleep(400); // sleep for 400ms and try again
    }
    return true;
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
    this.player = gameState.getPlayer(playerId);
  }

  private synchronized void run() {

    System.out.println("Player is playing: " + player.getPlayerId());

    // if player is primary/backup server, it needs to ping every 2 sec
//    startKeepAlive();
    if (isBackup()){
      System.out.println("registered as back up");
      System.out.println("start backup -> primary timer");
      Timer timer = new Timer();
      timer.schedule(new KeepAliveTask(), 0, 1000);
      timerStarted = true;
    }

    Scanner scanner = new Scanner(System.in);
    String input;
    Character commandChar = null;
    while (true) {

      try {
        input = scanner.nextLine();
        if (input == null || input.length() == 0){
          continue;
        }
        commandChar = input.charAt(0);
//        System.out.println(LocalDateTime.now() + "Command Char : " + commandChar);

      } catch (NoSuchElementException e) {
        System.err.println("Unable to read command");
        continue;
      }

      String trackingId = LocalDateTime.now() + " Command Char : " + commandChar;

      System.out.println("-----------");

      if (isPrimary()){
        try{
          play(commandChar);
        } catch (RemoteException | NotBoundException e) {
          System.out.println("server play error");
//          e.printStackTrace();
        }
      } else {
        boolean primaryNotFound = false;
        try {
          System.out.println(trackingId + " connecting to primary: "+gameState.getPrimary().getPlayerId());
          connectToServerAndPlay(commandChar);
          System.out.println(trackingId + " connected to primary: "+gameState.getPrimary().getPlayerId());
        } catch (RemoteException | NotBoundException e) {
          e.printStackTrace();
          System.out.println("primary connectToServerAndPlay error : " + gameState.getPlayerInfo());
          primaryNotFound = true;
        }

        if (primaryNotFound){
          try {
            if (isPrimary() || isBackup()){
              System.out.println(player.getPlayerId() + " is backup , playing local ");
              play(commandChar);

            } else {
              Player backupServer = gameState.getBackup();
              serverRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
              serverGameInterface = (GameInterface) serverRegistry.lookup(backupServer.getPlayerId());
              System.out.println(trackingId + " connecting to: "+backupServer.getPlayerId());
              connectToServerAndPlay(commandChar);
              System.out.println(trackingId + " connected to backup: "+backupServer.getPlayerId());
            }

          } catch (RemoteException | NotBoundException e) {
//            e.printStackTrace();
            System.out.println("backup connectToServerAndPlay error" + gameState.getPlayerInfo());
          }
        }
      }
    }
  }

  private void play(Character commandChar) throws RemoteException, NotBoundException{
    switch (commandChar){
      case '0':
        refreshGameStateUI();
        break;
      case '1':
        updateGameState(executeCommand(player, Command.MOVE_WEST));
        break;
      case '2':
        updateGameState(executeCommand(player, Command.MOVE_SOUTH));
        break;
      case '3':
        updateGameState(executeCommand(player, Command.MOVE_EAST));
        break;
      case '4':
        updateGameState(executeCommand(player, Command.MOVE_NORTH));
        break;
      case '9':
        System.out.println("Exit!");
        System.exit(0);
        break;

    }
  }

  private void connectToServerAndPlay(Character commandChar) throws RemoteException, NotBoundException {
    switch (commandChar){
      case '0':
        updateGameState(serverGameInterface.executeCommand(player, Command.GAME_STATE));
        break;
      case '1':
        updateGameState(serverGameInterface.executeCommand(player, Command.MOVE_WEST));
        break;
      case '2':
        updateGameState(serverGameInterface.executeCommand(player, Command.MOVE_SOUTH));
        break;
      case '3':
        updateGameState(serverGameInterface.executeCommand(player, Command.MOVE_EAST));
        break;
      case '4':
        updateGameState(serverGameInterface.executeCommand(player, Command.MOVE_NORTH));
        break;
      case '9':
        serverGameInterface.executeCommand(player, Command.EXIT);
        System.out.println("Exit!");
        System.exit(0);
        break;
      default:
        break;

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
    System.out.println(String.format("Tracker IP: %s, port number: %d, playerId: %s", trackerIpAddress, portNumber, playerId));
    if (playerId == null || playerId.length() != 2) {
      System.err.println("Invalid player id");
      return;
    }

    try {
      System.out.println("Game init -----------------------\n");
      String playerIpAddress = InetAddress.getLocalHost().getHostAddress();
      System.out.println("Player IP: "+ playerIpAddress);
      Registry playerRegistry = LocateRegistry.getRegistry(playerIpAddress, portNumber);

      Game game = new Game(playerId, playerIpAddress, portNumber);
      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);
      playerRegistry.rebind(playerId, iGame);

      Registry trackerRegistry = LocateRegistry.getRegistry(trackerIpAddress, portNumber);
      game.contactTracker(trackerRegistry);

      game.init();

      System.out.println("Player ready: " + playerId);

      game.run();

    } catch (RemoteException |  NotBoundException e) {
      System.err.println("Client connected exception: " + e.toString());
      e.printStackTrace();
    } catch (InterruptedException e) {
      System.err.println("Client InterruptedException exception: " + e.toString());
      e.printStackTrace();
    } catch (UnknownHostException e) {
      System.err.println("Client UnknownHostException exception: " + e.toString());
      e.printStackTrace();
    }


    System.out.println("Player exit unexpected: " + playerId);
    System.out.println("Exit!");
    System.exit(0);
  }

  @Override
  public void setPrimary(Player primary) throws RemoteException {
     gameState.primary = primary;
  }

  @Override
  public void setBackup(String backup) throws RemoteException {

  }

  @Override
  public GameState initPlayer(Player player) throws RemoteException, NotBoundException {
    synchronized (lock){
      gameState.addNewPlayer(player);
      Player backupServer = gameState.getBackup();
      if (backupServer == null){
        boolean found = false;
        for (String id : gameState.getPlayers().keySet()) {

          if (id.equals(gameState.getPrimary().getPlayerId()) || id.equals(player.getPlayerId())) {
            continue;
          }
          System.err.println("promoting: " + id + " as new backup when initPlayer");
          System.err.println("current player size: " + gameState.getPlayers().keySet().size());

          Player next = gameState.getPlayers().get(id);
          try {
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.promoteToBackupServer(gameState);
            gameState.setBackup(next); // set only after promoteToBackupServer is successful
            System.err.println("promoted: " + id + " as new backup");
            found=true;
            break;

          } catch (RemoteException | NotBoundException e) {
            System.err.println("Unable to promote player: " + next.getPlayerId());
          }
        }

        if (!found){
          gameState.setBackup(player);
        }

        System.out.println(LocalDateTime.now() + " found a backup: " + gameState.getBackup().getPlayerId());
        System.out.println("start primary -> backup timer");
        if (!timerStarted){
          Timer timer = new Timer();
          timer.schedule(new KeepAliveTask(), 0, 1000);
        }

        timerStarted = true;
        System.out.println(LocalDateTime.now() + " timer started: " + player.getPlayerId());
      } else if (!player.getPlayerId().equals(backupServer.getPlayerId())){
        notifyBackup();
      }
      refreshGameStateUI();
    }

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
      refreshGameStateUI(); // TODO: UI update in primary server may be too frequent
      notifyBackup();
    }
    return gameState;
  }

  private void notifyBackup() throws RemoteException, NotBoundException {
    if (!isPrimary()) {
      System.err.println("Only primary server needs notify backup server on game change");
      return;
    }
    Player backupServer = gameState.getBackup();
    if (backupServer != null && !backupServer.getPlayerId().equals(player.getPlayerId())) {
      System.err.println("Notify backup: "+ backupServer.getPlayerId());
      try{
        Registry backupRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
        GameInterface stub = (GameInterface) backupRegistry.lookup(backupServer.getPlayerId());
        stub.syncGameState(gameState);
      } catch (Exception e){
//        e.printStackTrace();
        System.out.println("notifyBackup error");
      }

    }
  }

  private void notifyTracker() {
    try {
      TrackerInterface stub = (TrackerInterface) trackerRegistry.lookup("Tracker");
      stub.updateServers(gameState.getPrimary(), gameState.getBackup());
      System.out.println(String.format("Notify Tracker | primary: %s, backup: %s",
              gameState.getPrimary(), gameState.getBackup()));

    } catch (RemoteException | NotBoundException e) {
      System.err.println("Update tracker failed!");
//      e.printStackTrace();
    }
  }

  @Override
  public boolean ping() throws RemoteException {
    return true;
  }

  @Override
  public boolean syncGameState(GameState gameState) throws RemoteException {
    updateGameState(gameState);
    return false;
  }

  @Override
  public void promoteToBackupServer(GameState gameState) throws RemoteException {
    this.gameState = gameState;
    this.gameState.setBackup(player);
    System.out.println("promoted to new back up");
    System.out.println("start backup -> primary timer");
    Timer timer = new Timer();
    timer.schedule(new KeepAliveTask(), 0, 1000);
    timerStarted = true;
//    startKeepAlive();
  }

  private void startKeepAlive() {
    if (isPrimary() || isBackup()) {
      System.out.println("startKeepAlive");
      Timer timer = new Timer();
      timer.schedule(new KeepAliveTask(), 0, 1000);

    }
  }

  private class KeepAliveTask extends TimerTask {
    public void run() {
      if (isPrimary()) {
        Player backup = gameState.getBackup();
        if (backup == null) {
          return;
        }
        try {
          Registry backupRegistry = LocateRegistry.getRegistry(backup.getIp(), backup.getPortNumber());
          GameInterface iBackup = (GameInterface) backupRegistry.lookup(backup.getPlayerId());
          iBackup.ping();
          System.err.println("Ping primary->backup : " + backup.getPlayerId());

        } catch (RemoteException | NotBoundException e) {
          System.err.println("Ping primary->backup failed!");
//          e.printStackTrace();
          handleBackupServerDown();
        }
      } else if (isBackup()) {
        Player primary = gameState.getPrimary();
        if (primary == null) {
          System.err.println("Error: Primary server missing!");
          return;
        }

        try {
          Registry primaryRegistry = LocateRegistry.getRegistry(primary.getIp(), primary.getPortNumber());
          GameInterface iPrimary = (GameInterface) primaryRegistry.lookup(primary.getPlayerId());
          iPrimary.ping();
          System.err.println("Ping backup->primary : " + primary.getPlayerId());

        } catch (RemoteException | NotBoundException e) {
          System.err.println("Ping backup->primary failed");
//          e.printStackTrace();
          handlePrimaryServerDown();
        }
      }

    }

    private void handleBackupServerDown() {
      // remove current backup from player list
      if (gameState.getBackup() != null){
        System.err.println("backup down : " + gameState.getBackup().getPlayerId());
      }
      gameState.exitPlayer(gameState.getBackup());
      gameState.setBackup(null);

      // promote next player to backup
      synchronized (lock){
        promote();
      }

    }


    private void handlePrimaryServerDown() {
      System.err.println("primary down, self " + player.getPlayerId() + "  as primary");
      // remove backup from player list
      gameState.exitPlayer(gameState.getPrimary());
      gameState.setBackup(null);

      // promote self to primary
      gameState.setPrimary(player);

      // find next player as backup
      promote();
    }

    private void promote() {
      List<Player> removeList = new ArrayList<>();

      boolean found = false;
      for (String id : gameState.getPlayers().keySet()) {

        if (id.equals(gameState.getPrimary().getPlayerId())) {
          continue;
        }
        System.err.println("promoting: " + id + " as new backup");
        System.err.println("current player size: " + gameState.getPlayers().keySet().size());

        Player next = gameState.getPlayers().get(id);
        try {
          if (!found){
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.promoteToBackupServer(gameState);
            gameState.setBackup(next); // set only after promoteToBackupServer is successful
            System.err.println("promoted: " + id + " as new backup");
            found = true;
          } else {
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.setPrimary(gameState.getPrimary());
            System.err.println("update primary for : " + id);
          }

        } catch (RemoteException | NotBoundException e) {
          removeList.add(next);
          System.err.println("Unable to connect to player: " + next.getPlayerId());
        }
      }

      for (Player player : removeList) {
        gameState.exitPlayer(player);
      }

      notifyTracker();

      // TODO: notify all the players
    }
  }
}
