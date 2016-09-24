package com.p2p.maze;

import javafx.scene.layout.Border;

import java.awt.*;
import java.util.Scanner;
import javax.swing.*;
import java.util.*;

/**
 * Created by CheunPin on 18/9/2016.
 */
public class MazeGUI extends JFrame{

    private Player player;
    private JPanel mainPanel;
    private JPanel playerListPanel;
    private JPanel mazePanel;
    private JList playerList;
    private JTextField [][] mazeGrid;

    public MazeGUI(Player player, GameState gameState) {
        this.player = player;
        this.initWindow();
        this.preparePanel(gameState);
        this.updateGameState(gameState);
        setVisible(true);
        this.pack();
    }

    private void initWindow() {
        setTitle("Player: " + this.player.getPlayerId());  // "super" Frame sets its title
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(1200, 800);
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainPanel = new JPanel();
        setContentPane(this.mainPanel);
    }

    private void preparePanel(GameState gameState){
        this.mainPanel.setLayout(new BorderLayout());
        this.playerListPanel = new JPanel();
        this.mazePanel = new JPanel();
        this.mainPanel.add(this.playerListPanel, BorderLayout.LINE_START);
        this.mainPanel.add(this.mazePanel, BorderLayout.CENTER);

        DefaultListModel listModel = new DefaultListModel();
        //Create the list and put it in a scroll pane.
        this.playerList = new JList(listModel);
        this.playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(this.playerList);
        this.playerListPanel.setLayout(new BorderLayout());
        this.playerListPanel.add(listScrollPane, BorderLayout.CENTER);

        this.mazePanel.setLayout(new GridLayout(gameState.getN(), gameState.getN()));
        this.mazeGrid = new JTextField[gameState.getN()][gameState.getN()];
        for(int i=0; i<this.mazeGrid.length; i++) {
            for (int j = 0; j < this.mazeGrid[i].length; j++) {
                this.mazeGrid[i][j] = new JTextField(i + " " + j);
                this.mazePanel.add(this.mazeGrid[i][j]);
            }
        }
    }

    public void updateGameState(GameState gameState){
        DefaultListModel listModel = (DefaultListModel)this.playerList.getModel();
        listModel.removeAllElements(); // remove list of players and update latest list of player
        for(String playerID: gameState.getPlayers().keySet()){
            listModel.addElement(playerID + " " + gameState.getPlayers().get(playerID).getScore());
        }
        String [][] maze = gameState.getMaze();
        for(int i=0; i<maze.length; i++) {
            for (int j = 0; j < maze[i].length; j++) {
                if (maze[i][j] == null) { // empty
                    this.mazeGrid[i][j].setText("");
                } else if (gameState.getPrimary() != null &&
                        maze[i][j].equals(gameState.getPrimary().getPlayerId())) {
                    this.mazeGrid[i][j].setText(String.format("(%2d, %2d) ", i, j) + maze[i][j] + " P");
                } else if (gameState.getBackup() != null &&
                        maze[i][j].equals(gameState.getBackup().getPlayerId())) {
                    this.mazeGrid[i][j].setText(String.format("(%2d, %2d) ", i, j) + maze[i][j] + " B");
                } else this.mazeGrid[i][j].setText(String.format("(%2d, %2d) ", i, j) + maze[i][j]);
            }
        }
        this.mazePanel.repaint();
        this.playerListPanel.repaint();
        this.mainPanel.repaint();
        this.repaint();
    }

    public static void main(String [] args) {
        Player primary = new Player("P1", "10.30.20.11", 8088);
        TrackerState trackerState = new TrackerState();
        trackerState.setPrimary(primary);
        trackerState.setK(10);
        trackerState.setN(15);
        GameState gameState = new GameState(trackerState);
        gameState.initGameState();
        gameState.addNewPlayer(primary);
        MazeGUI mazeGUI = new MazeGUI(primary, gameState);
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        System.out.println("New player P2");
        Player backupPlayer = new Player("P2", "localhost", 8088);
        gameState.addNewPlayer(backupPlayer);
        gameState.setBackup(backupPlayer);
        mazeGUI.updateGameState(gameState);
        scanner.nextLine();
        System.out.println("Moving right");
        gameState.move(primary, 1, 0);
        mazeGUI.updateGameState(gameState);
    }
}
