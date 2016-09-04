package com.p2p.maze;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Game
 *
 * Player in the maze game
 * First player join the game will become primary server
 * Second player join the game will become secondary server
 * In the event of primary or secondary server crash, the system should promote a new pair of primary/secondary servers
 */
public class Game implements GameState {

  public int n  = 0;
  public int k = 0;
  public String portNumber = "";
  public String primaryServer = "";
  public String backupServer = "";
  public String playerId = null;



  public Game(String portNumber, int n, int k, String playerId){
    this.portNumber = portNumber;
    this.n = n;
    this.k = k;
    this.playerId = playerId;
  }

  public int getN() {
    return n;
  }

  public void setN(int n) {
    this.n = n;
  }

  public int getK() {
    return k;
  }

  @Override
  public void setK(int k) {
    this.k = k;
  }

  public String getPortNumber() {
    return portNumber;
  }

  public void setPortNumber(String portNumber) {
    this.portNumber = portNumber;
  }

  public String getPrimaryServer() {
    return primaryServer;
  }

  public void setPrimaryServer(String primaryServer) {
    this.primaryServer = primaryServer;
  }

  public String getBackupServer() {
    return backupServer;
  }

  public void setBackupServer(String backupServer) {
    this.backupServer = backupServer;
  }

  @Override
  public String getPlayerId() {
    return playerId;
  }

  @Override
  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }

  public static void main(String[] args) {
    if (args.length < 3){
      System.err.println("Invalid input to start the game");
      return;
    }

    System.err.println("s1");
    String portNumber = args[1];
    String playerId =  args[2];
    if (playerId == null || playerId.length() !=2){
      System.err.println("Invalid player id");
      return;
    }
    try {
      System.err.println("s2");
      Registry registry = LocateRegistry.getRegistry();
      TrackerInfo stub = (TrackerInfo) registry.lookup("Tracker");
      String primary = stub.getPrimaryServer();
      String backup = stub.getBackupServer();
      int n = stub.getN();
      int k = stub.getK();

      System.out.println("Tracker response: ");
      System.out.println("[N]: " + n);
      System.out.println("[k]: " + k);
      System.out.println("[primary]: " + primary);
      System.out.println("[backup]: " + backup);

      Game game = new Game(portNumber, n, k, playerId);

      if (primary == null || primary.length() <1){
        System.out.println("Update primary server: ");
        stub.setPrimaryServer(playerId);
        game.setPrimaryServer(playerId);
        TrackerInfo stub2 = (TrackerInfo) registry.lookup("Tracker");
        System.out.println("Updated primary server: " + stub2.getPrimaryServer());
      } else if (backup == null || backup.length() < 1){
        System.out.println("Update backup server: ");
        stub.setBackupServer(playerId);
        game.setBackupServer(playerId);
        TrackerInfo stub2 = (TrackerInfo) registry.lookup("Tracker");
        System.out.println("Updated backup server: " + stub2.getBackupServer());
      }

      GameState gameState = (GameState) UnicastRemoteObject.exportObject(game, 0);

      // Bind the remote object's stub in the registry
      registry.bind(playerId, gameState);

      System.err.println("Player ready: " + playerId);

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      e.printStackTrace();
    }
  }
}
