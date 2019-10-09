package dai.android.app.free.player;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.List;

import dai.android.app.free.player.data.Address;

//
// https://raw.githubusercontent.com/emacs1024/freeplayer/master/app/src/main/assets/playlist.json
//

public class MainActivity extends AppCompatActivity implements PlayAddress.ICallBack {
    private final static String TAG = "MainActivity";

    private SurfaceCallBack mSurfaceCallBack;
    private SurfaceView mVideoDisplay;
    private List<Address> mAddresses;

    private int mPlayIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();

        // 获取播放地址
        PlayAddress.getInstance().getPlayData(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        VideoPlayer.getInstance().release();
    }

    @Override
    public void onComplete(List<Address> addresses) {
        if (null == addresses || addresses.isEmpty()) {
            Log.e(TAG, "[onComplete]: empty play address");
            mAddresses = null;
            return;
        }
        mAddresses = addresses;
    }

    private void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mVideoDisplay = findViewById(R.id.videoDisplay);

        View parent = mVideoDisplay.getRootView();
        if (null != parent) {
            parent.setOnTouchListener(mOnTouchListener);
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            Point point = new Point();
            getWindowManager().getDefaultDisplay().getSize(point);

            int width = point.x;
            int height = width * 9 / 16;

            layoutParams = new RelativeLayout.LayoutParams(width, height);
        }

        mVideoDisplay.setLayoutParams(layoutParams);

        mSurfaceCallBack = new SurfaceCallBack(this);
        mVideoDisplay.getHolder().addCallback(mSurfaceCallBack);
    }


    // 一些地址网站:
    // https://www.belaw.cn/2430.html
    // https://raw.githubusercontent.com/EvilCult/iptv-m3u-maker/master/tv.m3u8

    // 老友记
    private static final String URI = "http://aldirect.hls.huya.com/huyalive/29169025-2686220018-11537227127170531328-2847699120-10057-A-1524041208-1_1200.m3u8";

    private void startPlay(SurfaceHolder holder) {
        Log.d(TAG, "[startPlay]");

        String strUrl = URI;
        if (null != mAddresses && !mAddresses.isEmpty()) {
            if (mPlayIndex >= mAddresses.size()) {
                mPlayIndex = 0;
            }
            Address address = mAddresses.get(mPlayIndex++);
            Log.d(TAG, "current play info:\n" + address);

            strUrl = address.url;
        }

        VideoPlayer.getInstance().setDataSource(VideoPlayer.PlayerCode.IJK, new MyPlayCallBack(this), holder, strUrl);
    }

    private void start() {
        Log.d(TAG, "[start]");
        VideoPlayer.getInstance().start();
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private float posDownX = 0;
        private float posDownY = 0;

        private float currentPosX = 0;
        private float currentPosY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    posDownX = event.getX();
                    posDownY = event.getY();
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    currentPosX = event.getX();
                    currentPosY = event.getY();
                    break;
                }

                case MotionEvent.ACTION_UP: {
                    if (currentPosX - posDownX > 0 && (Math.abs(currentPosX - posDownX) > 450)) {
                        Log.d(TAG, "[onTouch]: go to -->");
                        startPlay(mVideoDisplay.getHolder());
                    } else if (currentPosX - posDownX < 0 && (Math.abs(currentPosX - posDownX) > 450)) {
                        Log.d(TAG, "[onTouch]: go to <--");
                        startPlay(mVideoDisplay.getHolder());
                    }

                    break;
                }
            }

            return false;
        }
    };


    private static class MyPlayCallBack implements IPlayerCallBack {

        private final WeakReference<MainActivity> ref;

        MyPlayCallBack(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void onBufferingUpdate(int percent) {
            Log.d(TAG, "[onBufferingUpdate]: percent=" + percent);
        }

        @Override
        public void onCompletion() {
            Log.d(TAG, "[onCompletion]");
        }

        @Override
        public void onError(int what, int extra) {
            Log.e(TAG, "[onError]: what=" + what + ", extra=" + extra);
        }

        @Override
        public void onInfo(int what, int extra) {
            Log.d(TAG, "[onInfo]: what=" + what + ", extra=" + extra);
        }

        @Override
        public void onPrepared() {
            Log.d(TAG, "[onPrepared]");
            ref.get().start();
        }

        @Override
        public void onSeekComplete() {
            Log.d(TAG, "[onSeekComplete]");
        }

        @Override
        public void onVideoSizeChanged(int width, int height) {
            Log.d(TAG, "[onVideoSizeChanged] width=" + width + ", height=" + height);
        }
    }


    private static class SurfaceCallBack implements SurfaceHolder.Callback {

        private final WeakReference<MainActivity> ref;

        SurfaceCallBack(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "[surfaceCreated]");
            ref.get().startPlay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "[surfaceChanged]: format=" + format + ", width=" + width + ", height=" + height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "[surfaceDestroyed]");
        }
    }
}
