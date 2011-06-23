package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class NewMessage extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    String recipientUsername;

    /* Commonly used widgets */
    EditText recipientEdit;


    /* Activity life-cycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_message);

        recipientEdit = (EditText)findViewById(R.id.recipient_edit);

        if (getIntent().getData().getPathSegments().size() > 0) {
            recipientUsername =
                getIntent().getData().getPathSegments().get(0);
            recipientEdit.setText(recipientUsername);
        }

        if (!BookTraderAPI.getInstance().loggedIn) {
            Log.e(TAG, "got to send message page without being loggedin");
            finish();
        }

        ((EditText)findViewById(R.id.sender_edit)).setText
            (BookTraderAPI.getInstance().currentUser);
    }


    /* Callbacks */

    public void sendMessage(View v) {
        Toast.makeText(this, "sending message", Toast.LENGTH_SHORT).show();
    }

    public void cancel(View v) {
        Log.v(TAG, "message cancelled");
        finish();
    }
}
