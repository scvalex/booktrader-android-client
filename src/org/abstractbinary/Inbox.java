package org.abstractbinary.booktrader;

import android.app.ListActivity;
import android.os.Bundle;


public class Inbox extends ListActivity {
    /* Internal gubbins */
    MessagesAdapter messagesAdapter;

    /* Acitvity life-cycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        messagesAdapter = new MessagesAdapter(this);
    }
}
