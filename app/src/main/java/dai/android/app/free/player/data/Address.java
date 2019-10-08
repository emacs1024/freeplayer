package dai.android.app.free.player.data;

import androidx.annotation.NonNull;

public class Address {

    public final String name;
    public final boolean enable;
    public final String url;

    public Address(String name, String url, boolean enable) {
        this.name = name;
        this.url = url;
        this.enable = enable;
    }

    @NonNull
    @Override
    public String toString() {
        return "name: '" + name + "', enable:" + enable + ", url: '" + url + "'";
    }
}
