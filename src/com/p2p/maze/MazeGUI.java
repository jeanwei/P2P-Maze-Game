package com.p2p.maze;

import javafx.scene.layout.Border;

import java.awt.*;
import javax.swing.*;

/**
 * Created by CheunPin on 18/9/2016.
 */
public class MazeGUI extends JFrame{

    private GameState gameState;
    private Player player;
    private JPanel mainPanel;
    private JPanel playerListPanel;
    private JPanel mazePanel;

    public MazeGUI(Player player) {
        this.player = player;
        initWindow();
        preparePanel();
    }

    private void initWindow()
    {
        setTitle("Player: " + this.player.getPlayerId());  // "super" Frame sets its title
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(1200, 800);
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainPanel = new JPanel();
        setContentPane(this.mainPanel);
    }

    private void preparePanel(){
        this.mainPanel.setLayout(new BorderLayout());
        this.playerListPanel = new JPanel();
        this.mazePanel = new JPanel();
        this.mainPanel.add(this.playerListPanel, BorderLayout.LINE_START);
        this.mainPanel.add(this.mazePanel, BorderLayout.CENTER);

        DefaultListModel listModel = new DefaultListModel();
        listModel.addElement("Jane Doe " + 1);
        listModel.addElement("John Smith " + 2);
        listModel.addElement("Kathy Green " + 3);

        //Create the list and put it in a scroll pane.
        JList list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(list);
        this.playerListPanel.setLayout(new BorderLayout());
        this.playerListPanel.add(listScrollPane, BorderLayout.CENTER);

        this.mazePanel.setLayout(new GridLayout(15, 15));
        for(int i=0; i<15; i++)
            for(int j=0; j<15; j++)
                this.mazePanel.add(new JTextField(i + " " + j));
    }

    public static void main(String [] args) {
        MazeGUI mazeGUI = new MazeGUI(new Player("P1", "10.30.20.11", 8088));
        mazeGUI.repaint();
    }
}
