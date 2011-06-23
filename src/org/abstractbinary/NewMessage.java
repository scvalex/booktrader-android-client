package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import java.util.List;


public class NewMessage extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    String recipientUsername;
    Handler handler;

    /* Commonly used widgets */
    AutoCompleteTextView recipientEdit;


    /* Activity life-cycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_message);

        if (!BookTraderAPI.getInstance().loggedIn) {
            Log.e(TAG, "got to send message page without being loggedin");
            finish();
        }

        recipientEdit =
            (AutoCompleteTextView)findViewById(R.id.recipient_edit);

        if (getIntent().getData().getPathSegments().size() > 0) {
            recipientUsername =
                getIntent().getData().getPathSegments().get(0);
            recipientEdit.setText(recipientUsername);
        }

        ((EditText)findViewById(R.id.sender_edit)).setText
            (BookTraderAPI.getInstance().currentUser);

        handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case ObjectCache.OBJECT_GET_STARTED:
                        // whoosh
                        break;
                    case ObjectCache.USERS_GOT:
                        handleGetUsersDone((List<String>)msg.obj);
                        break;
                    case ObjectCache.OBJECT_GET_FAILED:
                        Log.e(TAG, "couldn't get users: " +
                              (Exception)msg.obj);
                        break;
                    default:
                        throw new RuntimeException("unknown message type: " +
                                                   msg.what);
                    }
                }
            };

        ObjectCache.getInstance().getAllUsers(handler);
    }


    /* Callbacks */

    public void sendMessage(View v) {
        Toast.makeText(this, "sending message", Toast.LENGTH_SHORT).show();
    }

    public void cancel(View v) {
        Log.v(TAG, "message cancelled");
        finish();
    }


    /* Handlers */

    void handleGetUsersDone(List<String> usernames) {
        Log.v(TAG, "got " + usernames.size() + " users");
        recipientEdit.setAdapter
            (new ArrayAdapter
             (this, android.R.layout.simple_dropdown_item_1line, usernames));
    }
}
