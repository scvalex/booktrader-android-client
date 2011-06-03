package org.abstractbinary;

import android.app.*;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class BookTrader extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Login-state stuff */
    static final int STATE_NOT_LOGGED_IN = 0;
    static final int STATE_LOGGING_IN = 1;
    static final int STATE_LOGGED_IN = 2;
    int state;

    /* Dialogs */
    static final int DIALOG_LOGIN = 0;

    /* Often used widgets */
    FrameLayout menuBar;
    Button loginButton;

    /* Internal gubbins */
    String apiKey = "Your Key";


    /* Application life-cycle */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        menuBar = (FrameLayout)findViewById(R.id.menu_bar);

        loginButton = (Button)findViewById(R.id.login_button);

        state = STATE_NOT_LOGGED_IN;

        Log.v(TAG, "BookTrader running...");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOGIN:
            dialog = new Dialog(this);

            dialog.setContentView(R.layout.login_dialog);
            dialog.setTitle("Login");

            final EditText apiKeyField =
                (EditText)dialog.findViewById(R.id.api_key_field);
            apiKeyField.setText(apiKey);
            final Button button =
                (Button)dialog.findViewById(R.id.login_dialog_button);
            button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        apiKey = apiKeyField.getText().toString();
                        Log.v(TAG, "API key changed to: " + apiKey);
                        dialog.dismiss();
                        doLogin();
                    }
            });

            break;
        default:
            throw new RuntimeException("Unknown dialog type: " + id);
        }
        return dialog;
    }


    /* Event handlers */

    /** Called when the login button is pressed. */
    public void logIn(View v) {
        nextState();
    }


    /* Helpers */

    /** Used to cycle from one login-state to the next. */
    void nextState() {
        switch (state) {
        case STATE_NOT_LOGGED_IN:
            state = STATE_LOGGING_IN;
            loginButton.setEnabled(false);
            showDialog(DIALOG_LOGIN);
            break;
        case STATE_LOGGING_IN:
            state = STATE_LOGGED_IN;
            break;
        case STATE_LOGGED_IN:
            state = STATE_NOT_LOGGED_IN;
            loginButton.setEnabled(true);
            break;
        default:
            throw new RuntimeException("unknown state: " + state);
        }

        Log.v(TAG, "Now in state " + state);

        View v = menuBar.getChildAt(menuBar.getChildCount() - 1);
        menuBar.removeView(v);
        menuBar.addView(v, 0);
    }

    /** Perform the remote login and switch to login state if successful. */
    void doLogin() {
        Toast.makeText(this, "Doing login (and much more)...", Toast.LENGTH_SHORT).show();
    }
}
