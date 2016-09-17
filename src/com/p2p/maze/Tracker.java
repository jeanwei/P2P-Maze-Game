package com.p2p.maze;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Tracker
 * <p>
 * Tracker will have a well-known IP address and port number
 * Tracker knows the values of N and K.
 * In addition, tracker will inform new players of current Primary and Secondary servers
 */
public class Tracker implements TrackerInterface {
    public int n = 0;
    public int k = 0;
    public String portNumber;
    public String primaryServer = "";
    public String backupServer = "";

    public Tracker(String portNumber, int n, int k) {
        this.portNumber = portNumber;
        this.n = n;
        this.k = k;
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

    public String getPrimaryServer() {
        return primaryServer;
    }

    public void setPrimaryServer(String primaryServer) throws RemoteException {
        this.primaryServer = primaryServer;
    }

    public String getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(String backupServer) {
        this.backupServer = backupServer;
    }

    public TrackerState register(String playerId) throws RemoteException {
        // if no player, set to primary

        // if has primary only, set to backup

        // return n, k, primary and backup

        return null;
    }

    public static void main(String args[]) {

        if (args.length < 3) {
            System.err.println("Invalid input to create tracker");
            return;
        }

        try {
            String portNumber = args[0];
            int n = Integer.parseInt(args[1]);
            int k = Integer.parseInt(args[2]);

            Tracker tracker = new Tracker(portNumber, n, k);
            TrackerInterface stub = (TrackerInterface) UnicastRemoteObject.exportObject(tracker, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Tracker", stub);


            System.err.println("Tracker Server ready");
            System.err.println("portNumber: " + portNumber);
            System.err.println("n: " + n);
            System.err.println("k: " + k);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
