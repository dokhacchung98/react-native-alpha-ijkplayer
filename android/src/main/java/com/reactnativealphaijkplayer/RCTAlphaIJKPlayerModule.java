package com.reactnativealphaijkplayer;

import android.app.Activity;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

class RCTAlphaIJKPlayerModule extends ReactContextBaseJavaModule {
  private static final String TAG = "RCTAlphaIJKPlayerModule";


  private final ReactApplicationContext _reactContext;

  public RCTAlphaIJKPlayerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    _reactContext = reactContext;

  }

  @Override
  public String getName() {
    return TAG;
  }

  @Nullable
  @Override
  public Map<String, Object> getConstants() {
    return Collections.unmodifiableMap(new HashMap<String, Object>() {
      {
      }
    });
  }

  @ReactMethod
  public void playbackInfo(final Promise promise) {
    IjkVideoView player = RCTIJKPlayer.getViewInstance().getPlayer();
    WritableMap data = new WritableNativeMap();
    int currentPlaybackTime = player.getCurrentPosition() / 1000;
    int duration = player.getDuration() / 1000;
    int bufferingProgress = player.getBufferPercentage();
    int playbackState = player.CurrentState();

    data.putString("currentPlaybackTime", Integer.toString(currentPlaybackTime));
    data.putString("duration", Integer.toString(duration));
    data.putString("playableDuration", "");
    data.putString("bufferingProgress", Integer.toString(bufferingProgress));
    data.putString("playbackState", Integer.toString(playbackState));
    data.putString("loadState", "");
    data.putString("isPreparedToPlay", "");
    promise.resolve(data);
  }
}
