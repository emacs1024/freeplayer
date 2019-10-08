package dai.android.app.free.player;

public interface IPlayerCallBack {

    void onBufferingUpdate(int percent);

    void onCompletion();

    void onError(int what, int extra);

    void onInfo(int what, int extra);

    void onPrepared();

    void onSeekComplete();

    void onVideoSizeChanged(int width, int height);
}
