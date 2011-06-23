package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import android.app.AlertDialog;


public class NewMessage extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Constants */
    static final int DIALOG_INVALID_RECIPIENT = 0;

    /* Internal gubbins */
    String recipientUsername;
    Handler handler;
    List<String> validUsernames;

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

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_INVALID_RECIPIENT:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No such recipient")
                .setMessage("Check the recipient again")
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

    public void sendMessage(View v) {
        String recipient = recipientEdit.getText().toString();
        if (validUsernames != null && !validUsernames.contains(recipient)) {
            showDialog(DIALOG_INVALID_RECIPIENT);
            return;
        }

        Toast.makeText(this, "sending message", Toast.LENGTH_SHORT).show();
    }

    public void cancel(View v) {
        Log.v(TAG, "message cancelled");
        finish();
    }


    /* Handlers */

    void handleGetUsersDone(List<String> usernames) {
        Log.v(TAG, "got " + usernames.size() + " users");
        validUsernames = usernames;
        recipientEdit.setAdapter
            (new ArrayAdapter
             (this, android.R.layout.simple_dropdown_item_1line, usernames));
    }
}
