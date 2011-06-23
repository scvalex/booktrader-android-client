package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;


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
    }
}
