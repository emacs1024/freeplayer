package dai.android.app.free.player;

import android.util.Log;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

public final class VideoPlayer {
    private static final String TAG = "VideoPlayer";

    private static class Holder {
        private static final VideoPlayer instance = new VideoPlayer();
    }

    public static VideoPlayer getInstance() {
        return Holder.instance;
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    public enum PlayerCode {
        ANDROID,
        IJK
    }


    private VideoPlayer() {
        mCallBackInner = new PlayerCallBackImpl(this);
    }

    private void createPlayer(PlayerCode code) {
        if (null != mPlayer) {
            mPlayer.release();
            mPlayer = null;
        }
        switch (code) {
            case IJK: {
                mPlayer = new IjkMediaPlayer();
                break;
            }
            case ANDROID: {
                mPlayer = new AndroidMediaPlayer();
                break;
            }
        }
    }


    private IMediaPlayer mPlayer;

    private final Object DataSourceLocker = new Object();
    private final PlayerCallBackImpl mCallBackInner;
    private volatile IPlayerCallBack mCallBack;

    public void updateDisplay(SurfaceHolder holder) {
        if (null != mPlayer) {
            mPlayer.setDisplay(holder);
        }
    }

    public void setDataSource(PlayerCode code, IPlayerCallBack cb, SurfaceHolder holder, String uri) {
        Log.d(TAG, "[setDataSource]: uri = " + uri);
        synchronized (DataSourceLocker) {
            createPlayer(code);
            mCallBack = cb;
        }

        if (null == mPlayer) {
            Log.e(TAG, "create player of " + code + " failed.");
            return;
        }

        attachListener();

        mPlayer.setDisplay(holder);
        try {
            mPlayer.setDataSource(uri);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "[setDataSource] failed.", e);
        }
    }

    public void release() {
        if (null == mPlayer) return;

        mPlayer.release();
        mPlayer.setScreenOnWhilePlaying(false);
        synchronized (DataSourceLocker) {
            mCallBack = null;
        }
    }

    public void stop() {
        if (null == mPlayer) return;

        mPlayer.stop();
        mPlayer.setScreenOnWhilePlaying(false);
    }


    public void start() {
        if (null == mPlayer) return;

        mPlayer.start();
        mPlayer.setScreenOnWhilePlaying(true);
    }

    public void pause() {
        if (null == mPlayer) return;

        mPlayer.pause();
    }

    public void seekTo(long whereTo) {
        if (null == mPlayer) return;

        mPlayer.seekTo(whereTo);
    }


    private void attachListener() {
        if (null == mPlayer) return;

        mPlayer.setOnBufferingUpdateListener(mCallBackInner);
        mPlayer.setOnCompletionListener(mCallBackInner);
        mPlayer.setOnErrorListener(mCallBackInner);
        mPlayer.setOnInfoListener(mCallBackInner);
        mPlayer.setOnPreparedListener(mCallBackInner);
        mPlayer.setOnSeekCompleteListener(mCallBackInner);
        mPlayer.setOnVideoSizeChangedListener(mCallBackInner);
    }


    private void notifyOnBufferingUpdate(int percent) {
        if (null != mCallBack) {
            mCallBack.onBufferingUpdate(percent);
        }
    }

    private void notifyOnComplete() {
        if (null != mCallBack) {
            mCallBack.onCompletion();
        }
    }

    private void notifyOnError(int what, int extra) {
        if (null != mCallBack) {
            mCallBack.onError(what, extra);
        }
    }

    private void notifyOnInfo(int what, int extra) {
        if (null != mCallBack) {
            mCallBack.onInfo(what, extra);
        }
    }

    private void notifyOnPrepared() {
        if (null != mCallBack) {
            mCallBack.onPrepared();
        }
    }

    private void notifyOnSeekComplete() {
        if (null != mCallBack) {
            mCallBack.onSeekComplete();
        }
    }

    private void notifyOnVideoSizeChanged(int width, int height) {
        if (null != mCallBack) {
            mCallBack.onVideoSizeChanged(width, height);
        }
    }


    private static class PlayerCallBackImpl implements
            IMediaPlayer.OnPreparedListener,
            IMediaPlayer.OnCompletionListener,
            IMediaPlayer.OnBufferingUpdateListener,
            IMediaPlayer.OnSeekCompleteListener,
            IMediaPlayer.OnVideoSizeChangedListener,
            IMediaPlayer.OnErrorListener,
            IMediaPlayer.OnInfoListener,
            IMediaPlayer.OnTimedTextListener {

        private final WeakReference<VideoPlayer> ref;

        private PlayerCallBackImpl(VideoPlayer player) {
            ref = new WeakReference<>(player);
        }

        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
            ref.get().notifyOnBufferingUpdate(i);
        }

        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            ref.get().notifyOnComplete();
        }

        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
            ref.get().notifyOnError(what, extra);
            return true;
        }

        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
            ref.get().notifyOnInfo(what, extra);
            return false;
        }

        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            ref.get().notifyOnPrepared();
        }

        @Override
        public void onSeekComplete(IMediaPlayer iMediaPlayer) {
            ref.get().notifyOnSeekComplete();
        }

        @Override
        public void onTimedText(IMediaPlayer iMediaPlayer, IjkTimedText ijkTimedText) {
        }

        @Override
        public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int width, int height, int i2, int i3) {
            ref.get().notifyOnVideoSizeChanged(width, height);
        }
    }

}
