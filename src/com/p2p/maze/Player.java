package com.p2p.maze;

import java.io.Serializable;

/**
 *
 */
public class Player implements Serializable {
  private String playerId = null;
  private String ip;
  private int portNumber;
  private Position position;
  private int score;

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

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  @Override
  public String toString() {
    return "Player{" +
        "playerId='" + playerId + '\'' +
        ", ip='" + ip + '\'' +
        ", portNumber=" + portNumber +
        ", position=" + position +
        ", score=" + score +
        '}';
  }
}
