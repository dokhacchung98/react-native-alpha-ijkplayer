package com.reactnativealphaijkplayer;

import static com.reactnativealphaijkplayer.RCTAlphaIJKPlayerViewManager.REACT_ON_VIDEO_COMPLETE;
import static com.reactnativealphaijkplayer.RCTAlphaIJKPlayerViewManager.REACT_ON_VIDEO_ERROR;
import static com.reactnativealphaijkplayer.RCTAlphaIJKPlayerViewManager.REACT_ON_VIDEO_INFO;
import static com.reactnativealphaijkplayer.RCTAlphaIJKPlayerViewManager.REACT_ON_VIDEO_PREPARE;
import static com.reactnativealphaijkplayer.RCTAlphaIJKPlayerViewManager.REACT_ON_VIDEO_PROGRESS;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkVideoView extends FrameLayout implements LifecycleEventListener {
  private String TAG = "IjkVideoView";
  private Uri mUri;
  private Map<String, String> mHeaders;

  private static final int STATE_ERROR = -1;
  private static final int STATE_IDLE = 0;
  private static final int STATE_PREPARING = 1;
  private static final int STATE_PREPARED = 2;
  private static final int STATE_PLAYING = 3;
  private static final int STATE_PAUSED = 4;
  private static final int STATE_PLAYBACK_COMPLETED = 5;

  private static final int PROGRESS_UPDATE_INTERVAL_MILLS = 500;

  private int mCurrentState = STATE_IDLE;
  private int mTargetState = STATE_IDLE;

  private IRenderView.ISurfaceHolder mSurfaceHolder = null;
  private IMediaPlayer mMediaPlayer = null;
  private int mVideoWidth;
  private int mVideoHeight;
  private int mSurfaceWidth;
  private int mSurfaceHeight;
  private int mVideoRotationDegree;
  private int mCurrentBufferPercentage;
  private int mSeekWhenPrepared;

  private Context mAppContext;
  private IRenderView mRenderView;
  private int mVideoSarNum;
  private int mVideoSarDen;

  private boolean isPlay = true;
  private boolean isMuted = false;

  public static boolean isPostHandler = false;

  private CallBackSendEvent callBackSendEvent;

  public void setCallBackSendEvent(CallBackSendEvent callBackSendEvent) {
    this.callBackSendEvent = callBackSendEvent;
  }

  public IjkVideoView(ReactContext context) {
    super(context);
    initVideoView(context);
  }

  public IjkVideoView(ReactContext context, AttributeSet attrs) {
    super(context, attrs);
    initVideoView(context);
  }

  public IjkVideoView(ReactContext context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initVideoView(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public IjkVideoView(ReactContext context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initVideoView(context);
  }

  private Handler mHandler = new Handler();
  public static int mDuration;
  private Runnable progressUpdateRunnable = new Runnable() {
    @Override
    public void run() {
      if (mMediaPlayer == null || mDuration == 0) {
        isPostHandler = false;
        return;
      }
      isPostHandler = true;
      long currProgress = mMediaPlayer.getCurrentPosition();
      int mCurrProgress = (int) Math.ceil((currProgress * 1.0f) / 1000);
//      Log.e(TAG, "OnProgress: " + mCurrProgress + " - " + mMediaPlayer.getDuration() + " -- " + isPostHandler);
      WritableMap data = new WritableNativeMap();
      data.putInt("currentPosition", mCurrProgress);
      data.putDouble("duration", mMediaPlayer.getDuration());
      sendEvent(REACT_ON_VIDEO_PROGRESS, data);
//      if (!isPostHandler) {
      mHandler.postDelayed(progressUpdateRunnable, PROGRESS_UPDATE_INTERVAL_MILLS);
//      }
    }
  };

  private void initVideoView(ReactContext context) {
    mAppContext = context.getApplicationContext();
    mVideoWidth = 0;
    mVideoHeight = 0;

    context.addLifecycleEventListener(this);

    initRenders();

    setFocusable(true);
    setFocusableInTouchMode(true);
    requestFocus();
    mCurrentState = STATE_IDLE;
    mTargetState = STATE_IDLE;
  }

  public void setRenderView(IRenderView renderView) {
    if (mRenderView != null) {
      if (mMediaPlayer != null)
        mMediaPlayer.setDisplay(null);

      View renderUIView = mRenderView.getView();
      mRenderView.removeRenderCallback(mSHCallback);
      mRenderView = null;
      removeView(renderUIView);
    }

    if (renderView == null)
      return;

    mRenderView = renderView;
    renderView.setAspectRatio(mCurrentAspectRatio);
    Log.e(TAG, String.format("videosize setRenderView %d %d\n", mVideoWidth, mVideoHeight));
    if (mVideoWidth > 0 && mVideoHeight > 0)
      renderView.setVideoSize(mVideoWidth, mVideoHeight);
    if (mVideoSarNum > 0 && mVideoSarDen > 0)
      renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

    View renderUIView = mRenderView.getView();
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT,
      Gravity.CENTER);
    renderUIView.setLayoutParams(lp);
    addView(renderUIView);

    mRenderView.addRenderCallback(mSHCallback);
    mRenderView.setVideoRotation(mVideoRotationDegree);
  }

  public void setRender(int render) {
    switch (render) {
      case RENDER_NONE:
        setRenderView(null);
        break;
      case RENDER_TEXTURE_VIEW: {
        TextureRenderView renderView = new TextureRenderView(getContext());
        if (mMediaPlayer != null) {
          renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
          renderView.setVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
          Log.e(TAG, String.format("videosize mMediaPlayer %d %d\n", mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight()));
          renderView.setVideoSampleAspectRatio(mMediaPlayer.getVideoSarNum(), mMediaPlayer.getVideoSarDen());
          renderView.setAspectRatio(mCurrentAspectRatio);
        }
        setRenderView(renderView);
        break;
      }
      case RENDER_SURFACE_VIEW: {
        SurfaceRenderView renderView = new SurfaceRenderView(getContext());
        setRenderView(renderView);
        break;
      }
      default:
        Log.e(TAG, String.format(Locale.getDefault(), "invalid render %d\n", render));
        break;
    }
  }

  public void setVideoPath(String path) {
    setVideoURI(Uri.parse(path));
  }

  public void setVideoURI(Uri uri) {
    setVideoURI(uri, null);
  }

  private void setVideoURI(Uri uri, Map<String, String> headers) {
    mUri = uri;
    mHeaders = headers;
    mSeekWhenPrepared = 0;
    openVideo();
    requestLayout();
    invalidate();
  }

  public void stopPlayback() {
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
      mMediaPlayer.release();
      mMediaPlayer = null;

      mCurrentState = STATE_IDLE;
      mTargetState = STATE_IDLE;
      AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
      am.abandonAudioFocus(null);
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void openVideo() {
    if (mUri == null || mSurfaceHolder == null) {
      return;
    }
    release(false);

    AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
    am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

    try {
      createPlayer();

      mCurrentBufferPercentage = 0;
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
      } else {
        mMediaPlayer.setDataSource(mUri.toString());
      }
      bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setScreenOnWhilePlaying(true);
      mMediaPlayer.prepareAsync();

      mCurrentState = STATE_PREPARING;
    } catch (IOException ex) {
      Log.w(TAG, "Unable to open content: " + mUri, ex);
      mCurrentState = STATE_ERROR;
      mTargetState = STATE_ERROR;
      mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
    } catch (IllegalArgumentException ex) {
      Log.w(TAG, "Unable to open content: " + mUri, ex);
      mCurrentState = STATE_ERROR;
      mTargetState = STATE_ERROR;
      mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
    }
  }

  IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
    new IMediaPlayer.OnVideoSizeChangedListener() {
      public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        mVideoSarNum = mp.getVideoSarNum();
        mVideoSarDen = mp.getVideoSarDen();
        if (mVideoWidth != 0 && mVideoHeight != 0) {
          if (mRenderView != null) {
            Log.e(TAG, String.format("videosize onVideoSizeChanged setVideoSize %d %d", mVideoWidth, mVideoHeight));

            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
          }
          requestLayout();
        }
      }
    };

  IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
    public void onPrepared(IMediaPlayer mp) {
      mCurrentState = STATE_PREPARED;
      Log.e(TAG, "OnPreparedListener");

      mDuration = (int) Math.ceil(mMediaPlayer.getDuration() / 1000);

      mVideoWidth = mp.getVideoWidth();
      mVideoHeight = mp.getVideoHeight();

      int seekToPosition = mSeekWhenPrepared;
      if (seekToPosition != 0) {
        seekTo(seekToPosition);
      }
      if (mVideoWidth != 0 && mVideoHeight != 0) {
        if (mRenderView != null) {
          mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
          Log.e(TAG, String.format("videosize onPrepared setVideoSize %d %d\n", mVideoWidth, mVideoHeight));
          mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
          if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
            if (mTargetState == STATE_PLAYING) {
              start();
            }
          }
        }
      } else {
        if (mTargetState == STATE_PLAYING) {
          start();
        }
      }

      sendEvent(REACT_ON_VIDEO_PREPARE, new WritableNativeMap());
      setVideoIsPlay(isPlay);
      setVideoMuted(isMuted);
    }
  };

  private IMediaPlayer.OnCompletionListener mCompletionListener =
    mp -> {
      Log.e(TAG, "OnCompletionListener");
      mCurrentState = STATE_PLAYBACK_COMPLETED;
      mTargetState = STATE_PLAYBACK_COMPLETED;
      sendEvent(REACT_ON_VIDEO_COMPLETE, new WritableNativeMap());
    };

  private IMediaPlayer.OnInfoListener mInfoListener =
    new IMediaPlayer.OnInfoListener() {
      public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
        Log.e(TAG, "OnInfoListener");
        switch (arg1) {
          case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            Log.e(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
            break;
          case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
            Log.e(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
            break;
          case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
            Log.e(TAG, "MEDIA_INFO_BUFFERING_START:");
            break;
          case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
            Log.e(TAG, "MEDIA_INFO_BUFFERING_END:");
            break;
          case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
            Log.e(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
            break;
          case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            Log.e(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
            break;
          case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            Log.e(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
            break;
          case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            Log.e(TAG, "MEDIA_INFO_METADATA_UPDATE:");
            break;
          case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
            Log.e(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
            break;
          case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
            Log.e(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
            break;
          case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
            mVideoRotationDegree = arg2;
            Log.e(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
            if (mRenderView != null)
              mRenderView.setVideoRotation(arg2);
            break;
          case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
            Log.e(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
            break;
        }

        WritableMap data = new WritableNativeMap();
        int currentPlaybackTime = (int) (mMediaPlayer.getCurrentPosition() / 1000);
        int duration = (int) (mMediaPlayer.getDuration() / 1000);

        data.putString("currentPlaybackTime", Integer.toString(currentPlaybackTime));
        data.putString("duration", Integer.toString(duration));
        data.putString("playableDuration", "");
        data.putString("loadState", "");
        data.putString("isPreparedToPlay", "");

        sendEvent(REACT_ON_VIDEO_INFO, data);
        return true;
      }
    };

  private IMediaPlayer.OnErrorListener mErrorListener =
    (mp, framework_err, impl_err) -> {
      Log.e(TAG, "OnErrorListener: " + framework_err + "," + impl_err);
      mCurrentState = STATE_ERROR;
      mTargetState = STATE_ERROR;
      WritableMap data = new WritableNativeMap();
      data.putInt("errorCode", framework_err);
      sendEvent(REACT_ON_VIDEO_ERROR, data);
      return false;
    };

  private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
    new IMediaPlayer.OnBufferingUpdateListener() {
      public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        mCurrentBufferPercentage = percent;
      }
    };

  private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
    if (mp == null)
      return;

    if (holder == null) {
      mp.setDisplay(null);
      return;
    }

    holder.bindToMediaPlayer(mp);
  }

  IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
    @Override
    public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
      if (holder.getRenderView() != mRenderView) {
        Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
        return;
      }

      mSurfaceWidth = w;
      mSurfaceHeight = h;
      boolean isValidState = (mTargetState == STATE_PLAYING);
      boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
      if (mMediaPlayer != null && isValidState && hasValidSize) {
        if (mSeekWhenPrepared != 0) {
          seekTo(mSeekWhenPrepared);
        }
        start();
      }
    }

    @Override
    public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
      if (holder.getRenderView() != mRenderView) {
        Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
        return;
      }

      mSurfaceHolder = holder;
      if (mMediaPlayer != null)
        bindSurfaceHolder(mMediaPlayer, holder);
      else
        openVideo();
    }

    @Override
    public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
      if (holder.getRenderView() != mRenderView) {
        Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
        return;
      }

      mSurfaceHolder = null;
      releaseWithoutStop();
    }
  };

  public void releaseWithoutStop() {
    if (mMediaPlayer != null)
      mMediaPlayer.setDisplay(null);
  }

  public void release(boolean cleartargetstate) {
    if (mMediaPlayer != null) {
      mMediaPlayer.reset();
      mMediaPlayer.release();
      mMediaPlayer = null;
      mCurrentState = STATE_IDLE;
      if (cleartargetstate) {
        mTargetState = STATE_IDLE;
      }
      AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
      am.abandonAudioFocus(null);
    }
  }

  public void start() {
    isPlay = true;
    if (isInPlaybackState()) {
      mMediaPlayer.start();
      mCurrentState = STATE_PLAYING;
      if (!isPostHandler) {
        mHandler.post(progressUpdateRunnable);
      }
    }
    mTargetState = STATE_PLAYING;
  }

  public void pause() {
    isPlay = false;
    if (isInPlaybackState()) {
      if (mMediaPlayer.isPlaying()) {
        mMediaPlayer.pause();
        mCurrentState = STATE_PAUSED;
      }
      mHandler.removeCallbacks(progressUpdateRunnable);
      isPostHandler = false;
    }
    mTargetState = STATE_PAUSED;
  }

  public void setVideoIsPlay(boolean isPlay) {
    this.isPlay = isPlay;
    if (this.isPlay) {
      start();
    } else {
      pause();
    }
  }

  public void setVideoMuted(boolean isMuted) {
    this.isMuted = isMuted;
    if (mMediaPlayer != null) {
      mMediaPlayer.setVolume(this.isMuted ? 0 : 1, this.isMuted ? 0 : 1);
    }
  }

  public void suspend() {
    release(false);
  }

  public void resume() {
    isPlay = true;
    start();
  }

  public int getDuration() {
    if (isInPlaybackState()) {
      return (int) mMediaPlayer.getDuration();
    }

    return -1;
  }

  public int getCurrentPosition() {
    if (isInPlaybackState()) {
      return (int) mMediaPlayer.getCurrentPosition();
    }
    return 0;
  }

  public void seekTo(int msec) {
    if (isInPlaybackState()) {
      mMediaPlayer.seekTo(msec);
      mSeekWhenPrepared = 0;
    } else {
      mSeekWhenPrepared = msec;
    }
  }

  public boolean isPlaying() {
    return isInPlaybackState() && mMediaPlayer.isPlaying();
  }

  public int CurrentState() {
    int state;
    switch (this.mCurrentState) {
      case STATE_ERROR:
        state = 3;
        break;
      case STATE_PLAYING:
        if (mMediaPlayer.isPlaying()) {
          state = 1;
        } else {
          state = 1;
        }
        break;
      case STATE_PAUSED:
        state = 2;
        break;
      case STATE_IDLE:
      default:
        state = 0;
        break;
    }
    return state;
  }

  public int getBufferPercentage() {
    if (mMediaPlayer != null) {
      return mCurrentBufferPercentage;
    }
    return 0;
  }

  private boolean isInPlaybackState() {
    return (mMediaPlayer != null &&
      mCurrentState != STATE_ERROR &&
      mCurrentState != STATE_IDLE &&
      mCurrentState != STATE_PREPARING);
  }

  private int mCurrentAspectRatio = IRenderView.AR_16_9_FIT_PARENT;

  public static final int RENDER_NONE = 0;
  public static final int RENDER_SURFACE_VIEW = 1;
  public static final int RENDER_TEXTURE_VIEW = 2;

  private void initRenders() {
    setRender(RENDER_TEXTURE_VIEW);
  }

  public void createPlayer() {
    IjkMediaPlayer ijkMediaPlayer = null;
    if (mUri != null) {
      ijkMediaPlayer = new IjkMediaPlayer();
      ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_ERROR);

      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);

      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);

      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
    }
    mMediaPlayer = ijkMediaPlayer;

    mMediaPlayer.setOnPreparedListener(mPreparedListener);
    mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
    mMediaPlayer.setOnCompletionListener(mCompletionListener);
    mMediaPlayer.setOnErrorListener(mErrorListener);
    mMediaPlayer.setOnInfoListener(mInfoListener);
    mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
  }

  private void sendEvent(String eventName, WritableMap data) {
    if (callBackSendEvent != null) {
      callBackSendEvent.sendEvent(eventName, data);
    }
  }

  @Override
  public void onHostResume() {
    Log.e(TAG, "onHostResume");
    if (isPlay && mMediaPlayer != null) {
      mMediaPlayer.start();
      if (!isPostHandler) {
        mHandler.post(progressUpdateRunnable);
      }
    }
  }

  @Override
  public void onHostPause() {
    Log.e(TAG, "onHostPause");
    if (mMediaPlayer != null) {
      mMediaPlayer.pause();
      mHandler.removeCallbacks(progressUpdateRunnable);
      pause();
    }
  }

  @Override
  public void onHostDestroy() {
    Log.e(TAG, "onHostDestroy");
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
      mMediaPlayer.release();
      mHandler.removeCallbacks(progressUpdateRunnable);
      isPostHandler = false;
    }
  }
}
