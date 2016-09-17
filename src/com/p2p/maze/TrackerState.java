package com.p2p.maze;

import java.io.Serializable;

/**
 *
 *
 */
public class TrackerState implements Serializable {
  int n;
  int k;
  Player primary;
  Player backup;

  int getN() {
    return n;
  }

  void setN(int n) {
    this.n = n;
  }

  int getK() {
    return k;
  }

  void setK(int k) {
    this.k = k;
  }

  Player getPrimary() {
    return primary;
  }

  void setPrimary(Player primary) {
    this.primary = primary;
  }

  Player getBackup() {
    return backup;
  }

  void setBackup(Player backup) {
    this.backup = backup;
  }

  @Override
  public String toString() {
    return "TrackerState{" +
        "n=" + n +
        ", k=" + k +
        ", primary='" + primary + '\'' +
        ", backup='" + backup + '\'' +
        '}';
  }
}
