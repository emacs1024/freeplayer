package dai.android.app.free.player;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dai.android.app.free.player.data.Address;

public class PlayAddress {
    private static final String TAG = "PlayAddress";

    public interface ICallBack {
        void onComplete(List<Address> addresses);
    }

    private final List<Address> mPlayAddress = new ArrayList<>();

    private static final String JSON_URL = "https://raw.githubusercontent.com/emacs1024/freeplayer/master/app/src/main/assets/playlist.json";

    private PlayAddress() {
    }


    public void getPlayData(ICallBack cb) {
        Thread thread = new Thread(() -> {
            if (!mPlayAddress.isEmpty()) {
                cb.onComplete(mPlayAddress);
                return;
            }

            if (createPlayAddress()) {
                cb.onComplete(mPlayAddress);
            } else {
                cb.onComplete(null);
            }
        });
        thread.start();
    }

    private boolean createPlayAddress() {
        try {
            URL url = new URL(JSON_URL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            SSLContext TLSCtx = SSLContext.getInstance("TLS");

            MyX509TrustManager x509Trust = new MyX509TrustManager();
            TrustManager[] trustManagerArray = {x509Trust};
            TLSCtx.init(null, trustManagerArray, new SecureRandom());

            SSLSocketFactory factory = TLSCtx.getSocketFactory();
            connection.setHostnameVerifier((hostname, session) -> true);
            connection.setSSLSocketFactory(factory);
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            int flag = 0;
            byte[] buffer = new byte[1024];
            while (-1 != (flag = inputStream.read(buffer))) {
                sb.append(new String(buffer, 0, flag));
            }

            String strJson = sb.toString();
            if (TextUtils.isEmpty(strJson)) {
                Log.e(TAG, "can not get raw data from github");
                return false;
            }

            mPlayAddress.clear();

            JSONArray jsonArray = new JSONArray(strJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                if (null == object) continue;

                String name = object.getString("name");
                boolean enable = object.getBoolean("enable");
                if (!enable) {
                    Log.w(TAG, "Name: " + name + " not valid now");
                    continue;
                }
                String address = object.getString("address");

                Address item = new Address(name, address, enable);
                mPlayAddress.add(item);
            }

            return !mPlayAddress.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    private static class MyX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private static PlayAddress sInstall;

    public static PlayAddress getInstance() {
        if (null == sInstall) {
            sInstall = new PlayAddress();
        }
        return sInstall;
    }

}
