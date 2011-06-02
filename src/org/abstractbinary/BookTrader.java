package org.abstractbinary;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class BookTrader extends Activity {
    public class SpecialMenuBar extends View {
        public SpecialMenuBar(Context context) {
            super(context);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}
