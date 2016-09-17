package com.p2p.maze;

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

    public static final int CMD_GAME_STATE  = 0;
    public static final int CMD_MOVE_WEST   = 1;
    public static final int CMD_MOVE_SOUTH  = 2;
    public static final int CMD_MOVE_EAST   = 3;
    public static final int CMD_MOVE_NORTH  = 4;
    public static final int CMD_EXIT        = 9;

    public String playerId = null;
    public String portNumber = "";

    public int n = 0;
    public int k = 0;
    public String primaryServer = "";
    public String backupServer = "";

    private Game() {

    }

    public Game(String portNumber, int n, int k, String playerId) {
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

    public void setK(int k) {
        this.k = k;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public String getPrimary() {
        return primaryServer;
    }

    public void setPrimary(String primaryServer) {
        this.primaryServer = primaryServer;
    }

    public String getBackup() {
        return backupServer;
    }

    public void setBackup(String backupServer) {
        this.backupServer = backupServer;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    private void contactTracker() {

    }

    private void initPosition() {

    }

    private void run() {

    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Invalid input to start the game");
            return;
        }

        System.err.println("s1");
        String portNumber = args[1];
        String playerId = args[2];
        if (playerId == null || playerId.length() != 2) {
            System.err.println("Invalid player id");
            return;
        }
        try {
            Game game = new Game();

            // contact tracker and get tracker state: n, k, primary, backup
            game.contactTracker();

            // generate random position and call move (repeat till a move is valid)
            game.initPosition();

            // infinite loop that waits for user input and make a move
            game.run();


            System.err.println("s2");
            Registry registry = LocateRegistry.getRegistry();
            TrackerInterface stub = (TrackerInterface) registry.lookup("Tracker");
            String primary = stub.getPrimaryServer();
            String backup = stub.getBackupServer();
            int n = stub.getN();
            int k = stub.getK();

            System.out.println("Tracker response: ");
            System.out.println("[N]: " + n);
            System.out.println("[k]: " + k);
            System.out.println("[primary]: " + primary);
            System.out.println("[backup]: " + backup);

            if (primary == null || primary.length() < 1) {
                System.out.println("Update primary server: ");
                stub.setPrimaryServer(playerId);
                game.setPrimary(playerId);
                TrackerInterface stub2 = (TrackerInterface) registry.lookup("Tracker");
                System.out.println("Updated primary server: " + stub2.getPrimaryServer());
            } else if (backup == null || backup.length() < 1) {
                System.out.println("Update backup server: ");
                stub.setBackupServer(playerId);
                game.setBackup(playerId);
                TrackerInterface stub2 = (TrackerInterface) registry.lookup("Tracker");
                System.out.println("Updated backup server: " + stub2.getBackupServer());
            }

            GameInterface iGame = (GameInterface) UnicastRemoteObject.exportObject(game, 0);

            // Bind the remote object's stub in the registry
            registry.bind(playerId, iGame);

            System.err.println("Player ready: " + playerId);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
