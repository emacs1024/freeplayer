package dai.android.app.free.player;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dai.android.app.free.player.data.Address;

//
// https://raw.githubusercontent.com/emacs1024/freeplayer/master/app/src/main/assets/playlist.json
//

public class MainActivity extends AppCompatActivity implements PlayAddress.ICallBack {
    private final static String TAG = "MainActivity";

    private Vibrator mVibrator;

    private SurfaceCallBack mSurfaceCallBack;
    private SurfaceView mVideoDisplay;
    private FrameLayout mBoxView;
    private TextView mTxtInfo;

    private String mStrName;
    private List<Address> mAddresses;
    private AtomicInteger mPlayIndex = new AtomicInteger(0);
    private static final String STR_KEY = "PlayIndex";

    private float mBaseBrightness = 50F;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null != savedInstanceState) {
            mPlayIndex.set(savedInstanceState.getInt(STR_KEY, 0));
        }

        initView();

        // 获取播放地址
        PlayAddress.getInstance().getPlayData(this);

        setBrightness(mBaseBrightness);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (null != mVibrator) {
            mVibrator.cancel();
        }

        // release the player
        VideoPlayer.getInstance().release();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        VideoPlayer.getInstance().release();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STR_KEY, mPlayIndex.get());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPlayIndex.set(savedInstanceState.getInt(STR_KEY, 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
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


    private void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = lp.screenBrightness + brightness / 255.0F;
        if (lp.screenBrightness > 1) {
            lp.screenBrightness = 1;

            mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (null != mVibrator) {
                long[] pattern = {10, 20};
                mVibrator.vibrate(pattern, -1);
            }
        } else if (lp.screenBrightness < 0.2) {
            lp.screenBrightness = 0.2F;
            mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (null != mVibrator) {
                long[] pattern = {10, 20};
                mVibrator.vibrate(pattern, -1);
            }
        }
        getWindow().setAttributes(lp);
    }


    private void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mBoxView = findViewById(R.id.BoxView);
        mVideoDisplay = findViewById(R.id.videoDisplay);
        mTxtInfo = findViewById(R.id.txtInfo);

        View parent = mBoxView.getRootView();
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

        mBoxView.setLayoutParams(layoutParams);

        mSurfaceCallBack = new SurfaceCallBack(this);
        mVideoDisplay.getHolder().addCallback(mSurfaceCallBack);
    }


    // 一些地址网站:
    // https://www.belaw.cn/2430.html
    // https://raw.githubusercontent.com/EvilCult/iptv-m3u-maker/master/tv.m3u8

    // 老友记
    private static final String URI = "http://aldirect.hls.huya.com/huyalive/29169025-2686220018-11537227127170531328-2847699120-10057-A-1524041208-1_1200.m3u8";

    private void startPlay(SurfaceHolder holder, int index) {
        Log.d(TAG, "[startPlay]");

        String strUrl = URI;
        mStrName = "老友记";
        if (null != mAddresses && !mAddresses.isEmpty()) {
            if (index >= mAddresses.size()) {
                index = 0;
                mPlayIndex.set(index);
            } else if (index < 0) {
                index = mAddresses.size() - 1;
                mPlayIndex.set(index);
            }
            Address address = mAddresses.get(index);
            mStrName = address.name;
            strUrl = address.url;
        }

        VideoPlayer.getInstance().setDataSource(VideoPlayer.PlayerCode.IJK, new MyPlayCallBack(this), holder, strUrl);
        mTxtInfo.post(() -> mTxtInfo.setText(mStrName));
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
                        startPlay(mVideoDisplay.getHolder(), mPlayIndex.incrementAndGet());
                    } else if (currentPosX - posDownX < 0 && (Math.abs(currentPosX - posDownX) > 450)) {
                        Log.d(TAG, "[onTouch]: go to <--");
                        startPlay(mVideoDisplay.getHolder(), mPlayIndex.decrementAndGet());
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
            // Log.d(TAG, "[onBufferingUpdate]: percent=" + percent);
            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": buffer=" + percent;
                ref.get().mTxtInfo.setText(txt);
            });
        }

        @Override
        public void onCompletion() {
            Log.d(TAG, "[onCompletion]");
            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": finish";
                ref.get().mTxtInfo.setText(txt);
            });
        }

        @Override
        public void onError(int what, int extra) {
            Log.e(TAG, "[onError]: what=" + what + ", extra=" + extra);
            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": error> what=" + what + " extra=" + extra;
                ref.get().mTxtInfo.setText(txt);
            });
        }

        @Override
        public void onInfo(int what, int extra) {
            Log.d(TAG, "[onInfo]: what=" + what + ", extra=" + extra);
            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": info> what=" + what + " extra=" + extra;
                ref.get().mTxtInfo.setText(txt);
            });
        }

        @Override
        public void onPrepared() {
            Log.d(TAG, "[onPrepared]");

            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": prepared";
                ref.get().mTxtInfo.setText(txt);
            });

            ref.get().start();
        }

        @Override
        public void onSeekComplete() {
            Log.d(TAG, "[onSeekComplete]");
        }

        @Override
        public void onVideoSizeChanged(int width, int height) {
            Log.d(TAG, "[onVideoSizeChanged] width=" + width + ", height=" + height);

            ref.get().mTxtInfo.post(() -> {
                String txt = ref.get().mStrName + ": size changed> width=" + width + " height=" + height;
                ref.get().mTxtInfo.setText(txt);
            });
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
            ref.get().startPlay(holder, ref.get().mPlayIndex.get());
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
