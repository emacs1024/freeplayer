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

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private SurfaceCallBack mSurfaceCallBack;
    private SurfaceView mVideoDisplay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        VideoPlayer.getInstance().release();
    }


    // 漫威电影
    // private static final String URI = "http://aldirect.hls.huya.com/huyalive/30765679-2504742278-10757786168918540288-3049003128-10057-A-0-1_1200.m3u8";

    // 雍正王朝
    // https 不支持
    // private static final String URI = "https://txdirect.hls.huya.com/huyalive/29359996-2689277426-11550358594541060096-2847699098-10057-A-0-1_1200.m3u8";
    private static final String URI = "http://txdirect.hls.huya.com/huyalive/29359996-2689277426-11550358594541060096-2847699098-10057-A-0-1_1200.m3u8";

    // 日本电视台
    // private static final String URI = "http://192.240.127.34:1935/live/cs14.stream/media_1254.m3u8";

    // 老友记
    // private static final String URI = "http://aldirect.hls.huya.com/huyalive/29169025-2686220018-11537227127170531328-2847699120-10057-A-1524041208-1_1200.m3u8";

    private void startPlay(SurfaceHolder holder) {
        Log.d(TAG, "[startPlay]");
        VideoPlayer.getInstance().setDataSource(VideoPlayer.PlayerCode.IJK, new MyPlayCallBack(this), holder, URI);
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
                    } else if (currentPosX - posDownX < 0 && (Math.abs(currentPosX - posDownX) > 450)) {
                        Log.d(TAG, "[onTouch]: go to <--");
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
