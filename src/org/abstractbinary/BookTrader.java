package org.abstractbinary;

import android.app.Activity;
import android.os.Bundle;

public class BookTrader extends Activity {
    private static int STATE_NOT_LOGGED_IN = 0;
    private int state;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        state = STATE_NOT_LOGGED_IN;
    }
}
