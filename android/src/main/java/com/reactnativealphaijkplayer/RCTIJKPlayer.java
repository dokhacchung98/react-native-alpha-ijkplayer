package com.reactnativealphaijkplayer;

public class RCTIJKPlayer {

  private static final RCTIJKPlayer ourInstance = new RCTIJKPlayer();
  private RCTIJKPlayerView mIJKPlayerView;


  public static RCTIJKPlayer getInstance() {
    return ourInstance;
  }

  public static RCTIJKPlayerView getViewInstance() {
    return ourInstance.mIJKPlayerView;
  }

  public void setIJKPlayerView(RCTIJKPlayerView mIJKPlayerView) {
    this.mIJKPlayerView = mIJKPlayerView;
  }

  private RCTIJKPlayer() {
  }
}
