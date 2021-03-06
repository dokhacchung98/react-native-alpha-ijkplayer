package com.reactnativealphaijkplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.UiThreadUtil;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ISurfaceTextureHolder;
import tv.danmaku.ijk.media.player.ISurfaceTextureHost;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class TextureRenderView extends TextureView implements IRenderView {
  private static final String TAG = "TextureRenderView";
  private MeasureHelper mMeasureHelper;
  private int widthMeasureSpec, heightMeasureSpec;

  public TextureRenderView(Context context) {
    super(context);
    initView(context);
  }

  public TextureRenderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initView(context);
  }

  public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initView(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initView(context);
  }

  private void initView(Context context) {
    mMeasureHelper = new MeasureHelper(this);
    mSurfaceCallback = new SurfaceCallback(this);
    setSurfaceTextureListener(mSurfaceCallback);
  }

  @Override
  public View getView() {
    return this;
  }

  @Override
  public boolean shouldWaitForResize() {
    return false;
  }

  @Override
  protected void onDetachedFromWindow() {
    mSurfaceCallback.willDetachFromWindow();
    super.onDetachedFromWindow();
    mSurfaceCallback.didDetachFromWindow();
  }

  @Override
  public void setVideoSize(int videoWidth, int videoHeight) {
    if (videoWidth > 0 && videoHeight > 0) {
      mMeasureHelper.setVideoSize(videoWidth, videoHeight);
      Log.e(TAG, "requestLayout setVideoSize");
      requestLayout();
    }
  }

  @Override
  public void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen) {
    if (videoSarNum > 0 && videoSarDen > 0) {
      mMeasureHelper.setVideoSampleAspectRatio(videoSarNum, videoSarDen);
      requestLayout();
    }
  }

  @Override
  public void setVideoRotation(int degree) {
    mMeasureHelper.setVideoRotation(degree);
    setRotation(degree);
  }

  @Override
  public void setAspectRatio(int aspectRatio) {
    mMeasureHelper.setAspectRatio(aspectRatio);
    requestLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    this.widthMeasureSpec = widthMeasureSpec;
    this.heightMeasureSpec = heightMeasureSpec;
    mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec);
    Log.e(TAG, "setMeasuredDimension: " + mMeasureHelper.getMeasuredWidth() + " " + mMeasureHelper.getMeasuredHeight());
    setMeasuredDimension(mMeasureHelper.getMeasuredWidth(), mMeasureHelper.getMeasuredHeight());
  }

  public IRenderView.ISurfaceHolder getSurfaceHolder() {
    return new InternalSurfaceHolder(this, mSurfaceCallback.mSurfaceTexture, mSurfaceCallback);
  }

  private static final class InternalSurfaceHolder implements IRenderView.ISurfaceHolder {
    private TextureRenderView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private ISurfaceTextureHost mSurfaceTextureHost;

    public InternalSurfaceHolder(@NonNull TextureRenderView textureView,
                                 @Nullable SurfaceTexture surfaceTexture,
                                 @NonNull ISurfaceTextureHost surfaceTextureHost) {
      mTextureView = textureView;
      mSurfaceTexture = surfaceTexture;
      mSurfaceTextureHost = surfaceTextureHost;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void bindToMediaPlayer(IMediaPlayer mp) {
      if (mp == null)
        return;

      if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) &&
        (mp instanceof ISurfaceTextureHolder)) {
        ISurfaceTextureHolder textureHolder = (ISurfaceTextureHolder) mp;
        mTextureView.mSurfaceCallback.setOwnSurfaceTexture(false);

        SurfaceTexture surfaceTexture = textureHolder.getSurfaceTexture();
        if (surfaceTexture != null) {
          mTextureView.setSurfaceTexture(surfaceTexture);
        } else {
          textureHolder.setSurfaceTexture(mSurfaceTexture);
          textureHolder.setSurfaceTextureHost(mTextureView.mSurfaceCallback);
        }
      } else {
        mp.setSurface(openSurface());
      }
    }

    @NonNull
    @Override
    public IRenderView getRenderView() {
      return mTextureView;
    }

    @Nullable
    @Override
    public SurfaceHolder getSurfaceHolder() {
      return null;
    }

    @Nullable
    @Override
    public SurfaceTexture getSurfaceTexture() {
      return mSurfaceTexture;
    }

    @Nullable
    @Override
    public Surface openSurface() {
      if (mSurfaceTexture == null)
        return null;
      return new Surface(mSurfaceTexture);
    }
  }

  //-------------------------
  // SurfaceHolder.Callback
  //-------------------------

  @Override
  public void addRenderCallback(IRenderCallback callback) {
    mSurfaceCallback.addRenderCallback(callback);
  }

  @Override
  public void removeRenderCallback(IRenderCallback callback) {
    mSurfaceCallback.removeRenderCallback(callback);
  }

  private SurfaceCallback mSurfaceCallback;

  private static final class SurfaceCallback implements TextureView.SurfaceTextureListener, ISurfaceTextureHost {
    private SurfaceTexture mSurfaceTexture;
    private boolean mIsFormatChanged;
    private int mWidth;
    private int mHeight;

    private boolean mOwnSurfaceTexture = true;
    private boolean mWillDetachFromWindow = false;
    private boolean mDidDetachFromWindow = false;

    private WeakReference<TextureRenderView> mWeakRenderView;
    private Map<IRenderCallback, Object> mRenderCallbackMap = new ConcurrentHashMap<IRenderCallback, Object>();

    public SurfaceCallback(@NonNull TextureRenderView renderView) {
      mWeakRenderView = new WeakReference<TextureRenderView>(renderView);
    }

    public void setOwnSurfaceTexture(boolean ownSurfaceTexture) {
      mOwnSurfaceTexture = ownSurfaceTexture;
    }

    public void addRenderCallback(@NonNull IRenderCallback callback) {
      mRenderCallbackMap.put(callback, callback);

      ISurfaceHolder surfaceHolder = null;
      if (mSurfaceTexture != null) {
        if (surfaceHolder == null)
          surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), mSurfaceTexture, this);
        callback.onSurfaceCreated(surfaceHolder, mWidth, mHeight);
      }

      if (mIsFormatChanged) {
        if (surfaceHolder == null)
          surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), mSurfaceTexture, this);
        callback.onSurfaceChanged(surfaceHolder, 0, mWidth, mHeight);
      }
    }

    public void removeRenderCallback(@NonNull IRenderCallback callback) {
      mRenderCallbackMap.remove(callback);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      mSurfaceTexture = surface;
      mIsFormatChanged = false;
      mWidth = 0;
      mHeight = 0;

      ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
      for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
        renderCallback.onSurfaceCreated(surfaceHolder, 0, 0);
      }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      mSurfaceTexture = surface;
      mIsFormatChanged = true;
      mWidth = width;
      mHeight = height;

      ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
      for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
        renderCallback.onSurfaceChanged(surfaceHolder, 0, width, height);
      }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      mSurfaceTexture = surface;
      mIsFormatChanged = false;
      mWidth = 0;
      mHeight = 0;

      ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
      for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
        renderCallback.onSurfaceDestroyed(surfaceHolder);
      }

      Log.e(TAG, "onSurfaceTextureDestroyed: destroy: " + mOwnSurfaceTexture);
      return mOwnSurfaceTexture;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    //-------------------------
    // ISurfaceTextureHost
    //-------------------------

    @Override
    public void releaseSurfaceTexture(SurfaceTexture surfaceTexture) {
      if (surfaceTexture == null) {
        Log.e(TAG, "releaseSurfaceTexture: null");
      } else if (mDidDetachFromWindow) {
        if (surfaceTexture != mSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: didDetachFromWindow(): release different SurfaceTexture");
          surfaceTexture.release();
        } else if (!mOwnSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: didDetachFromWindow(): release detached SurfaceTexture");
          surfaceTexture.release();
        } else {
          Log.e(TAG, "releaseSurfaceTexture: didDetachFromWindow(): already released by TextureView");
        }
      } else if (mWillDetachFromWindow) {
        if (surfaceTexture != mSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: willDetachFromWindow(): release different SurfaceTexture");
          surfaceTexture.release();
        } else if (!mOwnSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: willDetachFromWindow(): re-attach SurfaceTexture to TextureView");
          setOwnSurfaceTexture(true);
        } else {
          Log.e(TAG, "releaseSurfaceTexture: willDetachFromWindow(): will released by TextureView");
        }
      } else {
        if (surfaceTexture != mSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: alive: release different SurfaceTexture");
          surfaceTexture.release();
        } else if (!mOwnSurfaceTexture) {
          Log.e(TAG, "releaseSurfaceTexture: alive: re-attach SurfaceTexture to TextureView");
          setOwnSurfaceTexture(true);
        } else {
          Log.e(TAG, "releaseSurfaceTexture: alive: will released by TextureView");
        }
      }
    }

    public void willDetachFromWindow() {
      Log.e(TAG, "willDetachFromWindow()");
      mWillDetachFromWindow = true;
    }

    public void didDetachFromWindow() {
      Log.e(TAG, "didDetachFromWindow()");
      mDidDetachFromWindow = true;
    }
  }

  @Override
  public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
    super.onInitializeAccessibilityEvent(event);
    event.setClassName(TextureRenderView.class.getName());
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.setClassName(TextureRenderView.class.getName());
  }

  private final Runnable measureAndLayout = new Runnable() {
    @Override
    public void run() {
      if (widthMeasureSpec != 0 && heightMeasureSpec != 0) {
        Log.e(TAG, "post measureAndLayout");
        measure(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, String.format("getLeft(), getTop(), getRight(), getBottom() %d %d %d %d", getLeft(), getTop(), getRight(), getBottom()));
        Log.e(TAG, String.format("getLeft(), getTop(), getRight(), getBottom() %d %d %d %d", getMeasuredHeight(), getMeasuredWidth(), View.MeasureSpec.getSize(getMeasuredHeight()), View.MeasureSpec.getSize(getMeasuredWidth())));
      }
    }
  };


  @Override
  public void requestLayout() {
    Log.e(TAG, "Overrided requestLayout called");
    super.requestLayout();

    UiThreadUtil.runOnUiThread(new Runnable() {
      public void run() {
        if (widthMeasureSpec != 0 && heightMeasureSpec != 0) {
          // TODO fix this
          Log.e(TAG, "ui measureAndLayout");
          measure(widthMeasureSpec, heightMeasureSpec);
          Log.e(TAG, String.format("getLeft(), getTop(), getRight(), getBottom() %d %d %d %d", getLeft(), getTop(), getRight(), getBottom()));
          Log.e(TAG, String.format("getLeft(), getTop(), getRight(), getBottom() %d %d %d %d", getMeasuredHeight(), getMeasuredWidth(), View.MeasureSpec.getSize(getMeasuredHeight()), View.MeasureSpec.getSize(getMeasuredWidth())));

          int left = getLeft();
          int top = getTop();
          int right = getRight();
          int bottom = getBottom();

          int height = View.MeasureSpec.getSize(getMeasuredHeight());
          int width = View.MeasureSpec.getSize(getMeasuredWidth());

          int h1 = bottom - top;
          layout(getLeft(), top + (h1 - height) / 2, getRight(), top + (h1 - height) / 2 + height);
        }

      }
    });
  }
}
