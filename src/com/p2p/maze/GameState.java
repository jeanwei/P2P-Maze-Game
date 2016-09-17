package com.p2p.maze;

/**
 * GameState map - Game State
 *
 */
public class GameState extends TrackerState {

  @Override
  public String toString() {
    return "GameState{" +
        "n=" + n +
        ", k=" + k +
        ", primary='" + primary + '\'' +
        ", backup='" + backup + '\'' +
        '}';
  }


}
