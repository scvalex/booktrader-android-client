package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;


public class UserDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Misc constants */
    static final int DIALOG_ABOUT_USER = 0;

    /* Background downloading */
    Handler requestHandler;

    /* Internal gubbins */
    Person user;
    String username;
    TextView usernameLabel;
    ImageView avatarView;
    Button aboutUserButton;

    /* Activity lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);

        username = getIntent().getData().getPathSegments().get(0);

        usernameLabel = (TextView)findViewById(R.id.user_username_label);
        usernameLabel.setText(username);

        avatarView = (ImageView)findViewById(R.id.user_avatar_view);

        aboutUserButton = (Button)findViewById(R.id.about_user_button);

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

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_ABOUT_USER:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(user.username + " (" + user.location + ")")
                .setMessage(user.about)
                .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            break;
        default:
            throw new RuntimeException("Unknown dialog type: " + id);
        }
        return dialog;
    }


    /* Callbacks */

    public void moreUser(View v) {
        showDialog(DIALOG_ABOUT_USER);
    }


    /* Handlers */

    void handlePersonGot(String username, Person person) {
        user = person;
        user.getAvatar(requestHandler);
        usernameLabel.setText(user.username);
        aboutUserButton.setEnabled(true);
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
