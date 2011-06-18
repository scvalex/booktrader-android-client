package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

class Person {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal state */
    String username = "";
    String avatarSource = "";
    String location = "Somewhere Far Away";
    String about = "Proud to be a BookTrader!";
    Drawable avatar;
    String jsonString;


    /* Public API */

    private Person() {
    }

    public Person(String username, JSONObject json)
        throws JSONException
    {
        this.username = username;
        this.avatarSource = json.getString("gravatar");
        this.location = json.getString("location");
        this.about = json.getString("about");
        this.jsonString = json.toString();
    }

    public void getAvatar(Handler handler) {
        DownloadCache.getInstance().getDrawable(avatarSource, handler);
    }
}
