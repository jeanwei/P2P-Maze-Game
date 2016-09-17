package com.p2p.maze;

import java.io.Serializable;

/**
 *
 *
 */
public class TrackerState implements Serializable {
  int n;
  int k;
  String primary;
  String backup;

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

  String getPrimary() {
    return primary;
  }

  void setPrimary(String primary) {
    this.primary = primary;
  }

  String getBackup() {
    return backup;
  }

  void setBackup(String backup) {
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
