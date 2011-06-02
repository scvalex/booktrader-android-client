package org.abstractbinary;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class BookTrader extends Activity {
    private static int STATE_NOT_LOGGED_IN = 0;
    private int state;

    private FrameLayout menuBar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        menuBar = (FrameLayout)findViewById(R.id.menu_bar);

        state = STATE_NOT_LOGGED_IN;
    }

    public void logIn(View v) {
        Toast.makeText(this, "Log in", Toast.LENGTH_SHORT).show();
        nextState();
    }

    private void nextState() {
        Toast.makeText(this, "next state", Toast.LENGTH_SHORT).show();
        View v = menuBar.getChildAt(menuBar.getChildCount() - 1);
        menuBar.removeView(v);
        menuBar.addView(v, 0);
    }
}
