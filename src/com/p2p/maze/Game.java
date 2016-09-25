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

  private MazeGUI gui;

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
      gameState.addPlayer(player);
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
          LOGGER.warning("currentG GameState: " + gameState);
          Player backup = gameState.getBackup();

          Player newPrimary = null;

          if (backup != null){
            Registry backupServerRegistry  = LocateRegistry.getRegistry(backup.getIp(), backup.getPortNumber());
            GameInterface backupServerGameInterface = (GameInterface) backupServerRegistry.lookup(backup.getPlayerId());
            GameState backupGameSate= backupServerGameInterface.getGameState();
            newPrimary = backupGameSate.getPrimary();
            gameState.setPrimary(newPrimary);
            gameState.setBackup(backupGameSate.getBackup());
          }

          LOGGER.warning("contactTracker reconnected GameState: " + gameState);

          if (newPrimary != null){
            server = newPrimary;
            serverRegistry = LocateRegistry.getRegistry(server.getIp(), server.getPortNumber());
            serverGameInterface = (GameInterface) serverRegistry.lookup(server.getPlayerId());
          }

        }

        LOGGER.info(LocalDateTime.now() + " retry to connect to server after 1s: " + server.getPlayerId());
        Thread.sleep(1000); // sleep for 2000ms and try again
      }
      updatePlayer();

    } else {
      LOGGER.warning("Primary server not found!");
    }
    this.gui = new MazeGUI(this.player, this.gameState);
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

    } // while loop only stops when successfully add new player
    return true;
  }

  private void updateGameState(GameState gameState){
    this.gameState = gameState;
    updatePlayer();
    refreshGameStateUI();
  }

  private void refreshGameStateUI(){
    LOGGER.info("player after refreshing: " + player);
    if(this.gui != null){
      this.gui.updateGameState(this.gameState);
    }
  }

  /**
   * Update player position and score
   */
  private void updatePlayer(){
    String playerId = player.getPlayerId();
    this.player = gameState.getPlayer(playerId);
  }

  private synchronized void run() {

    LOGGER.info(String.format("Player %s is running --------------------------------------------", player.getPlayerId()));

    if (isBackup()) {
      LOGGER.info(String.format("start backup -> primary timer after %s has registered as backup", player.getPlayerId()));
      startKeepAliveTimer();
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
        // TODO: enable continue before submission!
        // continue;
      }

      if (isPrimary()) {
        synchronized (lock){
          try{
            play(commandChar);
          } catch (RemoteException | NotBoundException e) {
            LOGGER.severe("server play error: " + e.toString());
          }
        }

      } else {
        boolean primaryNotFound = false;
        try {
          LOGGER.info(String.format("Command Char %s connect to primary: %s | start", commandChar,
                  gameState.getPrimary().getPlayerId()));

          connectToServerAndPlay(commandChar);

          LOGGER.info(String.format("Command Char %s connect to primary: %s | end", commandChar,
                  gameState.getPrimary().getPlayerId()));

        } catch (RemoteException | NotBoundException e) {
          LOGGER.severe("primary connectToServerAndPlay error : " + gameState.getPlayerInfo());
          primaryNotFound = true;
        }

        if (primaryNotFound) {
          try {
            if (isPrimary() || isBackup()){
              LOGGER.info(player.getPlayerId() + " is backup , playing local ");
              play(commandChar);

            } else {
              Player backupServer = gameState.getBackup();
              serverRegistry = LocateRegistry.getRegistry(backupServer.getIp(), backupServer.getPortNumber());
              serverGameInterface = (GameInterface) serverRegistry.lookup(backupServer.getPlayerId());

              LOGGER.info(String.format("Command char %s connect to: %s | start", commandChar, backupServer.getPlayerId()));

              connectToServerAndPlay(commandChar);

              LOGGER.info(String.format("Command char %s connect to: %s | end", commandChar, backupServer.getPlayerId()));
            }

          } catch (RemoteException | NotBoundException e) {
            LOGGER.severe(String.format("backup connectToServerAndPlay error: %s\n player: %s", e.toString(),
                    gameState.getPlayerInfo()));
          }
        }
      }
    }
  }

  private static void quit() {
    LOGGER.info("Exit!");
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
        quit();
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
        quit();
        break;
      default:
        break;

    }
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
    quit();
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
  public GameState initPlayer(Player player) throws RemoteException, NotBoundException {
    synchronized (lock) {
      gameState.addPlayer(player);
      Player backupServer = gameState.getBackup();
      if (backupServer == null) {
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
            LOGGER.severe("Unable to internalPromote player: " + next.getPlayerId());
          }
        }

        if (!found) { // set new player
          gameState.setBackup(player);
        }
        notifyTracker();
        LOGGER.info("found a backup: " + gameState.getBackup().getPlayerId());

        if (!timerStarted) {
          // when primary find a new backup, it should then start timer
          startKeepAliveTimer();
          LOGGER.info(String.format("Primary %s timer started", player.getPlayerId()));
        }

      } else if (!player.getPlayerId().equals(backupServer.getPlayerId())) {
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
        updated = gameState.move(player, -1, 0);
        break;

      case MOVE_SOUTH:
        updated = gameState.move(player, 0, 1);
        break;

      case MOVE_EAST:
        updated = gameState.move(player, 1, 0);
        break;

      case MOVE_NORTH:
        updated = gameState.move(player, 0, -1);
        break;

      case EXIT:
        updated = gameState.exitPlayer(player);
        break;

      default:
        LOGGER.warning("Unrecognized command");
    }
    if (updated){
      refreshGameStateUI();
      notifyBackup();
    }
    return gameState;
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
  public GameState getGameState() throws RemoteException {
    return gameState;
  }

  @Override
  public void promoteToBackupServer(GameState gameState) throws RemoteException {
    this.gameState = gameState;
    this.gameState.setBackup(player);
    LOGGER.info("promoted to new back up");
    LOGGER.info("start backup -> primary timer");
    startKeepAliveTimer();
  }

  @Override
  public void updateServers(Player primary, Player backup) throws RemoteException {
    gameState.setPrimary(primary);
    gameState.setBackup(backup);
  }

  private void startKeepAliveTimer() {
    LOGGER.info("startKeepAliveTimer");
    Timer timer = new Timer();
    timer.schedule(new KeepAliveTask(), 0, 1000);
    timerStarted = true;
  }

  private class KeepAliveTask extends TimerTask {
    public synchronized void run() {
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
          LOGGER.warning("Ping primary->backup failed! Trying to internalPromote new backup server now.");
          handleBackupServerDown(backup);
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
          LOGGER.warning(String.format("Ping backup->primary failed! Trying to internalPromote %s first.", player.getPlayerId()));
          handlePrimaryServerDown();
        }
      }
    }

    private void handleBackupServerDown(Player oldBackup) {

      synchronized (lock){
        Player backup = gameState.getBackup();
        if (backup != null && backup.getPlayerId().equals(oldBackup.getPlayerId())){          
          LOGGER.warning(String.format("%s remove current backup from player list: %s", player.getPlayerId(),
              gameState.getBackup().getPlayerId()));
      
          gameState.exitPlayer(oldBackup);
          gameState.setBackup(null);

          // promote next player to backup
          internalPromote();
        }
      }
    }

    private void handlePrimaryServerDown() {
      LOGGER.info(String.format("%s remove current primary from player list: %s", player.getPlayerId(),
              gameState.getPrimary().getPlayerId()));
              
      synchronized (lock){
        // remove backup from player list
        if (!isPrimary()){
          gameState.exitPlayer(gameState.getPrimary());

          LOGGER.info("promote self to primary and set backup to null");     
          gameState.setPrimary(player);
          gameState.setBackup(null);
          internalPromote();
        } else {
          LOGGER.info("promoted as " + player.getPlayerId() + "  as primary");
        }

      }
    }

    // promote next available player to backup server and broadcast new servers to other players
    private void internalPromote() {
      List<Player> removeList = new ArrayList<>();

      LOGGER.info("player size before promotion: " + gameState.getPlayers().keySet().size());

      boolean found = false;
      for (String id : gameState.getPlayers().keySet()) {
        if (id.equals(gameState.getPrimary().getPlayerId())) {
          continue;
        }

        Player next = gameState.getPlayers().get(id);
        try {
          if (!found) {
            LOGGER.info(String.format("internalPromote %s as new backup | start", id));

            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.promoteToBackupServer(gameState);
            gameState.setBackup(next); // set only after promoteToBackupServer is successful

            LOGGER.info(String.format("internalPromote %s as new backup | start", id));

            found = true; // once set to true, start broadcasting changes to other players

          } else {
            LOGGER.info(String.format("Notify player %s about new backup | start", id));

            Registry registry = LocateRegistry.getRegistry(next.getIp(), next.getPortNumber());
            GameInterface stub = (GameInterface) registry.lookup(next.getPlayerId());
            stub.updateServers(gameState.getPrimary(), gameState.getBackup());

            LOGGER.info(String.format("Notify player %s about new backup | end", id));
          }

        } catch (RemoteException | NotBoundException e) {
          LOGGER.severe("Unable to connect to player: " + next.getPlayerId());
          removeList.add(next);
        }
      }

      for (Player player : removeList) {
        gameState.exitPlayer(player);
      }

      LOGGER.info("player size after promotion: " + gameState.getPlayers().keySet().size());

      notifyTracker();
    }
  }
}
