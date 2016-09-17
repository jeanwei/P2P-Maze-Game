package com.p2p.maze;

import java.io.Serializable;

/**
 * Created by CheunPin on 17/9/2016.
 */
public class Position implements Serializable{
  public int posX;
  public int posY;

  public Position(int newPosX, int newPosY)
  {
    this.posX = newPosX;
    this.posY = newPosY;
  }

  @Override
  public String toString() {
    return "Position{" +
        "posX=" + posX +
        ", posY=" + posY +
        '}';
  }
}
