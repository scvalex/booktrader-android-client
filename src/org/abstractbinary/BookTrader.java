package org.abstractbinary;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class BookTrader extends Activity {
    private final static int STATE_NOT_LOGGED_IN = 0;
    private final static int STATE_LOGGING_IN = 1;
    private final static int STATE_LOGGED_IN = 2;
    private int state;

    private FrameLayout menuBar;
    private Button loginButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        menuBar = (FrameLayout)findViewById(R.id.menu_bar);

        loginButton = (Button)findViewById(R.id.login_button);

        state = STATE_NOT_LOGGED_IN;
    }

    public void logIn(View v) {
        Toast.makeText(this, "Log in", Toast.LENGTH_SHORT).show();
        nextState();
    }

    private void nextState() {
        if (state == STATE_NOT_LOGGED_IN) {
            state = STATE_LOGGING_IN;
            loginButton.setEnabled(false);
        } else if (state == STATE_LOGGING_IN) {
            state = STATE_LOGGED_IN;
        } else if (state == STATE_LOGGED_IN) {
            state = STATE_NOT_LOGGED_IN;
            loginButton.setEnabled(true);
        } else {
            throw new RuntimeException("unknown state");
        }

        View v = menuBar.getChildAt(menuBar.getChildCount() - 1);
        menuBar.removeView(v);
        menuBar.addView(v, 0);
    }
}
