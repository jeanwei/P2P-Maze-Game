package com.p2p.maze;

import java.io.Serializable;

/**
 *
 */
public class Player implements Serializable {
  private String playerId = null;
  private String ip;
  private int portNumber;

  public Player(String playerId, String ip, int portNumber) {
    this.playerId = playerId;
    this.ip = ip;
    this.portNumber = portNumber;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  @Override
  public String toString() {
    return "Player{" +
        "playerId='" + playerId + '\'' +
        ", ip='" + ip + '\'' +
        ", portNumber=" + portNumber +
        '}';
  }
}
