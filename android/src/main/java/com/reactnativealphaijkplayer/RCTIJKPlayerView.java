package com.reactnativealphaijkplayer;

import android.app.Activity;
import android.util.Log;
import android.widget.FrameLayout;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RCTIJKPlayerView extends FrameLayout implements CallBackSendEvent {
  private static final String TAG = "RCTIJKPlayerView";
  private final ReactContext _context;
  private Activity activity;
  private FrameLayout framelayout;
  private IjkVideoView mIJKPlayerView;

  public IjkVideoView getPlayer() {
    return this.mIJKPlayerView;
  }

  public RCTIJKPlayerView(ReactContext context) {
    super(context);
    this._context = context;

    IjkMediaPlayer.loadLibrariesOnce(null);
    IjkMediaPlayer.native_profileBegin("libijkplayer.so");
    mIJKPlayerView = new IjkVideoView(context);
    mIJKPlayerView.setCallBackSendEvent(this::sendEvent);

    RCTIJKPlayer.getInstance().setIJKPlayerView(this);

    addView(mIJKPlayerView);
  }

  public void init(Activity activity) {
    this.activity = activity;
  }

  public void refresh() {
    Log.e(TAG, "view refresh");
    this.postInvalidate();
    UiThreadUtil.runOnUiThread(() -> requestLayout());
  }

  public void start(final String URL) {
    Log.e(TAG, String.format("start URL %s", URL));
    UiThreadUtil.runOnUiThread(() -> {
      mIJKPlayerView.setVideoPath(URL);
      mIJKPlayerView.start();
    });
  }

  public void stop() {
    Log.e(TAG, String.format("stop"));
    mIJKPlayerView.stopPlayback();
  }

  public void pause() {
    Log.e(TAG, String.format("pause"));
    mIJKPlayerView.pause();
  }

  public void resume() {
    Log.e(TAG, String.format("resume"));
    UiThreadUtil.runOnUiThread(() -> mIJKPlayerView.resume());

  }

  public void shutdown() {
    Log.e(TAG, String.format("shutdown"));
    mIJKPlayerView.release(true);
  }

  public void seekTo(double currentPlaybackTime) {
    int position = (int) (currentPlaybackTime * 1000);
    Log.e(TAG, "seekTo " + currentPlaybackTime + ", " + position);
    mIJKPlayerView.seekTo(position);
  }

  public void playVideoSource(String src) {
    Log.e(TAG, "playVideoSource: " + src);
    mIJKPlayerView.setVideoPath(src);
    mIJKPlayerView.start();
  }

  public void setVideoIsPlay(boolean isPlay) {
    mIJKPlayerView.setVideoIsPlay(isPlay);
  }

  public void setVideoisMuted(boolean isMuted) {
    mIJKPlayerView.setVideoMuted(isMuted);
  }

  public void releaseVideo() {
    mIJKPlayerView.release(true);
  }

  @Override
  public void sendEvent(String eventName, WritableMap params) {
    ReactContext reactContext = (ReactContext) getContext();

    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), eventName, params);
  }
}
