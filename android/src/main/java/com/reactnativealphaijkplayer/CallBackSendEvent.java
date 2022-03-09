package com.reactnativealphaijkplayer;

import com.facebook.react.bridge.WritableMap;

public interface CallBackSendEvent {
  void sendEvent(String eventName, WritableMap data);
}
