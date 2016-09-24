package com.p2p.maze;

import com.p2p.maze.utils.LogFormatter;

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
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * Game
 * <p>
 * Player in the maze game
 * First player join the game will become primary server
 * Second player join the game will become secondary server
 * In the event of primary or secondary server crash, the system should promote a new pair of primary/secondary servers
 */
public class Game implements GameInterface {

  private static final Logger LOGGER = Logger.getLogger(Game.class.getSimpleName());

  private final Object lock = new Object();

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
  private Registry trackerRegistry;
  private GameInterface serverGameInterface;
  private GameState gameState;
  private boolean timerStarted = false;

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

    LOGGER.info(String.format("initial GameState: %s", gameState));
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

      LOGGER.info(String.format("start connecting to server: %s", server.getPlayerId()));

      while (serverGameInterface == null) {
        try {
          serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
          serverGameInterface = (GameInterface) serverRegistry.lookup(server.getPlayerId());
        } catch (Exception e) {
          LOGGER.severe("initial serverGameInterface error: " + server.getPlayerId());
          e.printStackTrace();
        }
      }

      LOGGER.info(String.format("connected to server: %s successfully", server.getPlayerId()));

      while (true) {
        try {
          if (connectToAddPlayer(server)) {
            break;
          }
        } catch (Exception e) {
          LOGGER.severe("initial connection error");

          TrackerInterface stub = (TrackerInterface) trackerRegistry.lookup("Tracker");
          TrackerState trackerState = stub.getTrackerState();
          gameState = new GameState(trackerState);
          gameState.setBackup(trackerState.getBackup());

          LOGGER.warning("contactTracker reconnected GameState: " + gameState);

          // TODO: this could cause further exception!!
          server = gameState.getPrimary();
          serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
          serverGameInterface = (GameInterface) serverRegistry.lookup(server.getPlayerId());
        }

        LOGGER.info(LocalDateTime.now() + " retry to connect to server after 1s: " + server.getPlayerId());
        Thread.sleep(1000); // sleep for 2000ms and try again
      }
      updatePlayer();

    } else {
      LOGGER.warning("Primary server not found!");
    }

    LOGGER.info("game state after init: " + gameState);
    LOGGER.info("player after init: " + player);
  }

  private boolean connectToAddPlayer(Player server) throws RemoteException, NotBoundException, InterruptedException {
    while (true) {
      gameState = serverGameInterface.initPlayer(player);
      // found its own player id inside the map, then add player success, then loop
      if (gameState.getPlayer(player.getPlayerId()) != null) {
        break;
      }
      LOGGER.warning(String.format("RETRY: connect to server %s after 400ms", server.getPlayerId()));
      Thread.sleep(400); // sleep for 400ms and try again

    } // while loop only stops when successfully add new player inside the map
    return true;
  }

  private void updateGameState(GameState gameState){
    this.gameState = gameState;
    updatePlayer();
    refreshGameStateUI();
  }

  private void refreshGameStateUI(){
//    LOGGER.info("game state after refreshing: " + gameState);
    LOGGER.info("player after refreshing: " + player);
  }

  /**
   * Update player position and score
   */
  private void updatePlayer(){
    String playerId = player.getPlayerId();
    this.player = gameState.getPlayer(playerId);
  }

  private synchronized void run() {

    LOGGER.info(String.format("Player %s is playing", player.getPlayerId()));

    // if player is primary/backup server, it needs to ping every 2 sec
//    startKeepAlive();
    if (isBackup()){
      LOGGER.info("registered as back up");
      LOGGER.info("start backup -> primary timer");
      Timer timer = new Timer();
      timer.schedule(new KeepAliveTask(), 0, 1000);
      timerStarted = true;
    }

    Scanner scanner = new Scanner(System.in);
    String input;
    Character commandChar;
    while (true) {

      try {
        input = scanner.nextLine();
        if (input == null || input.length() == 0){
          continue;
        }
        commandChar = input.charAt(0);

      } catch (NoSuchElementException e) {
        LOGGER.severe("Unable to read command");
        quit();
        return;
      }

      String trackingId = " Command Char : " + commandChar;

      if (isPrimary()){
        try{
          play(commandChar);
        } catch (RemoteException | NotBoundException e) {
          LOGGER.severe("server play error: " + e.toString());
        }
      } else {
        boolean primaryNotFound = false;
        try {
          LOGGER.info(trackingId + " connecting to primary: "+gameState.getPrimary().getPlayerId());
          connectToServerAndPlay(commandChar);
          LOGGER.info(trackingId + " connected to primary: "+gameState.getPrimary().getPlayerId());

        } catch (RemoteException | NotBoundException e) {
          LOGGER.severe("primary connectToServerAndPlay error : " + gameState.getPlayerInfo());
          e.printStackTrace();

          primaryNotFound = true;
        }

        if (primaryNotFound){
          try {
            if (isPrimary() || isBackup()){
              LOGGER.info(player.getPlayerId() + " is backup , playing local ");
              play(commandChar);

            } else {
              Player backupServer = gameState.getBackup();
              serverRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
              serverGameInterface = (GameInterface) serverRegistry.lookup(backupServer.getPlayerId());

              LOGGER.info(trackingId + " connecting to: "+backupServer.getPlayerId());

              connectToServerAndPlay(commandChar);

              LOGGER.info(trackingId + " connected to backup: "+backupServer.getPlayerId());
            }

          } catch (RemoteException | NotBoundException e) {
            LOGGER.severe(String.format("backup connectToServerAndPlay error: %s\n player: %s", e.toString(),
                    gameState.getPlayerInfo()));
          }
        }
      }
    }
  }

  private void quit() {
    System.exit(0);
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
        LOGGER.info("Exit!");
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
        LOGGER.info("Exit!");
        System.exit(0);
        break;
      default:
        break;

    }
  }

  public static void main(String[] args) {
    initLogger();

    if (args.length < 3) {
      LOGGER.warning("Invalid input to start the game");
      return;
    }

    String trackerIpAddress = args[0];
    int portNumber = Integer.parseInt(args[1]);
    String playerId = args[2];

    LOGGER.info("Game start -----------------------");
    LOGGER.info(String.format("Tracker IP: %s, port number: %d, playerId: %s", trackerIpAddress, portNumber, playerId));

    if (playerId == null || playerId.length() != 2) {
      LOGGER.warning("Invalid player id");
      return;
    }

    try {
      LOGGER.info("Game init -----------------------");
      String playerIpAddress = InetAddress.getLocalHost().getHostAddress();
      LOGGER.info("Player IP: "+ playerIpAddress);

      Registry playerRegistry = LocateRegistry.getRegistry(playerIpAddress, portNumber);

      Game game = new Game(playerId, playerIpAddress, portNumber);
      GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);
      playerRegistry.rebind(playerId, iGame);

      Registry trackerRegistry = LocateRegistry.getRegistry(trackerIpAddress, portNumber);
      game.contactTracker(trackerRegistry);

      game.init();

      LOGGER.info("Player ready: " + playerId);

      game.run();

    } catch (RemoteException |  NotBoundException e) {
      LOGGER.severe("Client connected exception: " + e.toString());
      e.printStackTrace();
    } catch (InterruptedException e) {
      LOGGER.severe("Client InterruptedException exception: " + e.toString());
      e.printStackTrace();
    } catch (UnknownHostException e) {
      LOGGER.severe("Client UnknownHostException exception: " + e.toString());
      e.printStackTrace();
    }


    LOGGER.severe(String.format("Player %s exit unexpectedly", playerId));
    LOGGER.severe("Exit!");
    System.exit(0);
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
          LOGGER.info("promoting: " + id + " as new backup when initPlayer");
          LOGGER.info("current player size: " + gameState.getPlayers().keySet().size());

          Player next = gameState.getPlayers().get(id);
          try {
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.promoteToBackupServer(gameState);
            gameState.setBackup(next); // set only after promoteToBackupServer is successful
            LOGGER.info("promoted: " + id + " as new backup");
            found=true;
            break;

          } catch (RemoteException | NotBoundException e) {
            LOGGER.severe("Unable to promote player: " + next.getPlayerId());
          }
        }

        if (!found){
          gameState.setBackup(player);
        }

        LOGGER.info("found a backup: " + gameState.getBackup().getPlayerId());
        LOGGER.info("start primary -> backup timer");
        if (!timerStarted){
          Timer timer = new Timer();
          timer.schedule(new KeepAliveTask(), 0, 1000);
        }

        timerStarted = true;
        LOGGER.info("timer started: " + player.getPlayerId());

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
        LOGGER.warning("Unrecognized command");
    }
    if (updated){
      refreshGameStateUI(); // TODO: UI update in primary server may be too frequent
      notifyBackup();
    }
    return gameState;
  }

  private void notifyBackup() throws RemoteException, NotBoundException {
    if (!isPrimary()) {
      LOGGER.warning("Only primary server needs notify backup server on game change");
      return;
    }

    Player backupServer = gameState.getBackup();
    if (backupServer != null && !backupServer.getPlayerId().equals(player.getPlayerId())) {
      LOGGER.info("Notify backup: "+ backupServer.getPlayerId());

      try{
        Registry backupRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
        GameInterface stub = (GameInterface) backupRegistry.lookup(backupServer.getPlayerId());
        stub.syncGameState(gameState);
      } catch (Exception e){
        LOGGER.severe(String.format("notifyBackup error: %s", e.toString()));
      }
    }
  }

  private void notifyTracker() {
    try {
      TrackerInterface stub = (TrackerInterface) trackerRegistry.lookup("Tracker");
      stub.updateServers(gameState.getPrimary(), gameState.getBackup());
      LOGGER.info(String.format("Notify Tracker | primary: %s, backup: %s", gameState.getPrimary(), gameState.getBackup()));

    } catch (RemoteException | NotBoundException e) {
      LOGGER.severe("Update tracker failed!");
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
    LOGGER.info("promoted to new back up");
    LOGGER.info("start backup -> primary timer");
    Timer timer = new Timer();
    timer.schedule(new KeepAliveTask(), 0, 1000);
    timerStarted = true;
//    startKeepAlive();
  }

  private void startKeepAlive() {
    if (isPrimary() || isBackup()) {
      LOGGER.info("startKeepAlive");
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
          LOGGER.info("Ping primary->backup : " + backup.getPlayerId());

        } catch (RemoteException | NotBoundException e) {
          LOGGER.warning("Ping primary->backup failed! Trying to promote new backup server now.");
          handleBackupServerDown();
        }
      } else if (isBackup()) {
        Player primary = gameState.getPrimary();
        if (primary == null) {
          LOGGER.severe("Error: Primary server missing!");
          return;
        }

        try {
          Registry primaryRegistry = LocateRegistry.getRegistry(primary.getIp(), primary.getPortNumber());
          GameInterface iPrimary = (GameInterface) primaryRegistry.lookup(primary.getPlayerId());
          iPrimary.ping();
          LOGGER.info("Ping backup->primary : " + primary.getPlayerId());

        } catch (RemoteException | NotBoundException e) {
          LOGGER.warning(String.format("Ping backup->primary failed! Trying to promote %s first.", player.getPlayerId()));
          handlePrimaryServerDown();
        }
      }

    }

    private void handleBackupServerDown() {
      // remove current backup from player list
      if (gameState.getBackup() != null){
        LOGGER.warning("backup down : " + gameState.getBackup().getPlayerId());
      }
      gameState.exitPlayer(gameState.getBackup());
      gameState.setBackup(null);

      // promote next player to backup
      synchronized (lock){
        promote();
      }
    }


    private void handlePrimaryServerDown() {
      LOGGER.info("primary down, self " + player.getPlayerId() + "  as primary");
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
        LOGGER.info("promoting: " + id + " as new backup");
        LOGGER.info("current player size: " + gameState.getPlayers().keySet().size());

        Player next = gameState.getPlayers().get(id);
        try {
          if (!found){
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.promoteToBackupServer(gameState);
            gameState.setBackup(next); // set only after promoteToBackupServer is successful
            LOGGER.info("promoted: " + id + " as new backup");
            found = true;
          } else {
            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.setPrimary(gameState.getPrimary());
            LOGGER.info("update primary for : " + id);
          }

        } catch (RemoteException | NotBoundException e) {
          LOGGER.severe("Unable to connect to player: " + next.getPlayerId());
          removeList.add(next);
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
