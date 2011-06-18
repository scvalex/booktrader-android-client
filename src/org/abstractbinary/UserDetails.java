package org.abstractbinary.booktrader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class UserDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Background downloading */
    Handler requestHandler;

    /* Internal gubbins */
    Person user;
    String username;
    TextView usernameLabel;
    ImageView avatarView;

    /* Activity lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);

        username = getIntent().getData().getPathSegments().get(0);

        usernameLabel = (TextView)findViewById(R.id.user_username_label);
        usernameLabel.setText(username);

        avatarView = (ImageView)findViewById(R.id.user_avatar_view);

        requestHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.PersonResult r;
                    DownloadCache.DownloadResult rd;
                    switch (msg.what) {
                    case ObjectCache.OBJECT_GET_STARTED:
                        // whoosh
                        break;
                    case ObjectCache.PERSON_GOT:
                        r = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGot(r.username, (Person)r.result);
                        break;
                    case ObjectCache.OBJECT_GET_FAILED:
                        handlePersonGetFailed((Exception)msg.obj);
                        break;
                    case DownloadCache.DOWNLOAD_DONE:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadDone(rd.url, (Drawable)rd.result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadError(rd.url, (Exception)rd.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type" +
                                                   msg.what);
                    }
                }
            };

        ObjectCache.getInstance().getPersonDetails(username, requestHandler);
    }


    /* Handlers */

    void handlePersonGot(String username, Person person) {
        user = person;
        user.getAvatar(requestHandler);
        usernameLabel.setText(user.username);
        ((TextView)findViewById(R.id.user_location_label)).setText(user.location);
        ((TextView)findViewById(R.id.user_about_label)).setText(user.about);
    }

    void handlePersonGetFailed(Exception exception) {
        Log.v(TAG, "failed to get: " + exception);
        Toast.makeText(this, "trouble bubble", Toast.LENGTH_SHORT).show();
        // whoosh
    }

    void handleDownloadDone(String url, Drawable image) {
        user.avatar = image;
        avatarView.setImageDrawable(user.avatar);
    }

    void handleDownloadError(String url, Exception e) {
        Log.v(TAG, "failed to download " + url + " because " + e);
        //whoosh
    }
}
