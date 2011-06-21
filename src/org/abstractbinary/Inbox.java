package org.abstractbinary.booktrader;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.TextView;


public class Inbox extends ListActivity {
    /* Internal gubbins */
    MessagesAdapter messagesAdapter;

    /* Common widgets */
    TextView summaryText;

    /* Acitvity life-cycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.inbox);

        summaryText = (TextView)findViewById(R.id.summary_text);

        messagesAdapter = new MessagesAdapter(this);
    }
}
