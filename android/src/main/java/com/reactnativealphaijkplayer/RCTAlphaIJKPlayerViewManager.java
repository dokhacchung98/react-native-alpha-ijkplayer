package com.reactnativealphaijkplayer;

import android.util.Log;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public class RCTAlphaIJKPlayerViewManager extends ViewGroupManager<RCTIJKPlayerView> {
  private static final String REACT_CLASS = "AlphaIJKPlayer";

  public static final String REACT_ON_VIDEO_PROGRESS = "onVideoProgressRN";
  public static final String REACT_ON_VIDEO_PREPARE = "onVideoPrepareRN";
  public static final String REACT_ON_VIDEO_ERROR = "onVideoErrorRN";
  public static final String REACT_ON_VIDEO_COMPLETE = "onVideoCompleteRN";
  public static final String REACT_ON_VIDEO_INFO = "onVideoInfoRN";

  private static final int V_startVideo = 261;
  private static final int V_pauseVideo = 262;
  private static final int V_releaseVideo = 264;
  private static final int V_changeVolume = 265;
  private static final int V_seekVideo = 266;
  private static final int V_stopVideo = 267;
  private static final int V_resumeVideo = 268;

  private static final String REACT_EVENT_START_VIDEO = "startVideo";
  private static final String REACT_EVENT_PAUSE_VIDEO = "pauseVideo";
  private static final String REACT_EVENT_RELEASE_VIDEO = "releaseVideo";
  private static final String REACT_EVENT_VOLUME = "changeVolume";
  private static final String REACT_EVENT_SEEK_VIDEO = "seekVideo";
  private static final String REACT_EVENT_STOP_VIDEO = "stopVideo";
  private static final String REACT_EVENT_RESUME_VIDEO = "resumeVideo";

  public RCTAlphaIJKPlayerViewManager() {
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public RCTIJKPlayerView createViewInstance(ThemedReactContext context) {
    return new RCTIJKPlayerView(context);
  }

  @ReactProp(name = "src")
  public void setSourceVideo(RCTIJKPlayerView playerView, String src) {
    if (src == null || src.isEmpty()) {
      Log.e(REACT_CLASS, "setSourceVideo error path");
      return;
    }
    playerView.playVideoSource(src);
  }

  @ReactProp(name = "isMuted", defaultBoolean = false)
  public void setVideoIsMuted(RCTIJKPlayerView playerView, boolean isMuted) {
    playerView.setVideoisMuted(isMuted);
  }

  @ReactProp(name = "isPlay", defaultBoolean = true)
  public void setVideoIsPlay(RCTIJKPlayerView playerView, boolean isPlay) {
    playerView.setVideoIsPlay(isPlay);
  }

  @Override
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.<String, Object>builder()
      .put(REACT_ON_VIDEO_PROGRESS, MapBuilder.of("registrationName", REACT_ON_VIDEO_PROGRESS))
      .put(REACT_ON_VIDEO_PREPARE, MapBuilder.of("registrationName", REACT_ON_VIDEO_PREPARE))
      .put(REACT_ON_VIDEO_ERROR, MapBuilder.of("registrationName", REACT_ON_VIDEO_ERROR))
      .put(REACT_ON_VIDEO_COMPLETE, MapBuilder.of("registrationName", REACT_ON_VIDEO_COMPLETE))
      .put(REACT_ON_VIDEO_INFO, MapBuilder.of("registrationName", REACT_ON_VIDEO_INFO))
      .build();
  }

//  @Nullable
//  @Override
//  public Map getExportedCustomBubblingEventTypeConstants() {
//    return MapBuilder.builder()
//      .put(
//        REACT_ON_VIDEO_PROGRESS,
//        MapBuilder.of(
//          "phasedRegistrationNames",
//          MapBuilder.of("bubbled", REACT_ON_VIDEO_PROGRESS)))
//      .put(
//        REACT_ON_VIDEO_PREPARE,
//        MapBuilder.of(
//          "phasedRegistrationNames",
//          MapBuilder.of("bubbled", REACT_ON_VIDEO_PREPARE)))
//      .put(
//        REACT_ON_VIDEO_ERROR,
//        MapBuilder.of(
//          "phasedRegistrationNames",
//          MapBuilder.of("bubbled", REACT_ON_VIDEO_ERROR)))
//      .put(
//        REACT_ON_VIDEO_COMPLETE,
//        MapBuilder.of(
//          "phasedRegistrationNames",
//          MapBuilder.of("bubbled", REACT_ON_VIDEO_COMPLETE)))
//      .put(
//        REACT_ON_VIDEO_INFO,
//        MapBuilder.of(
//          "phasedRegistrationNames",
//          MapBuilder.of("bubbled", REACT_ON_VIDEO_INFO)))
//      .build();
//  }

  @Override
  public Map<String, Integer> getCommandsMap() {
    Map<String, Integer> map = new HashMap();
    map.put(REACT_EVENT_START_VIDEO, V_startVideo);
    map.put(REACT_EVENT_PAUSE_VIDEO, V_pauseVideo);
    map.put(REACT_EVENT_VOLUME, V_changeVolume);
    map.put(REACT_EVENT_SEEK_VIDEO, V_seekVideo);
    map.put(REACT_EVENT_STOP_VIDEO, V_stopVideo);
    map.put(REACT_EVENT_RELEASE_VIDEO, V_releaseVideo);
    map.put(REACT_EVENT_RESUME_VIDEO, V_resumeVideo);
    return map;
  }

  @Override
  public void receiveCommand(@Nonnull RCTIJKPlayerView playerView, int commandId, @javax.annotation.Nullable ReadableArray args) {
//    Log.e(REACT_CLASS, "receiveCommand: " + commandId);
    switch (commandId) {
      case V_startVideo:
        if (args.size() > 0 && args.getType(0) == ReadableType.String) {
          String url = args.getString(0);
          playerView.start(url);
        }
        break;
      case V_pauseVideo:
        playerView.pause();
        break;
      case V_changeVolume:
        if (args.size() > 0 && args.getType(0) == ReadableType.Boolean) {
          boolean isMuted = args.getBoolean(0);
          playerView.setVideoisMuted(isMuted);
        }
        break;
      case V_seekVideo:
        if (args.size() > 0 && args.getType(0) == ReadableType.Number) {
          long vSeek = (long) args.getDouble(0);
          playerView.seekTo(vSeek);
        }
        break;
      case V_releaseVideo:
        playerView.releaseVideo();
        break;
      case V_stopVideo:
        playerView.stop();
        break;
      case V_resumeVideo:
        playerView.resume();
        break;
    }
  }
}
