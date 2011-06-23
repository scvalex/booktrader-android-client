package org.abstractbinary.booktrader;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class Inbox extends ListActivity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    static final int DIALOG_LOADING = 0;
    MessagesAdapter messagesAdapter;
    Handler requestHandler;

    /* Common widgets */
    TextView summaryText;
    ProgressDialog loadingDialog;

    /* Acitvity life-cycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.inbox);

        summaryText = (TextView)findViewById(R.id.summary_text);

        messagesAdapter = new MessagesAdapter(this);
        setListAdapter(messagesAdapter);

        requestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (loadingDialog != null)
                    loadingDialog.dismiss();
                switch (msg.what) {
                case ObjectCache.OBJECT_GET_STARTED:
                    showDialog(DIALOG_LOADING);
                    break;
                case ObjectCache.MESSAGES_GOT:
                    handleMessagesGot((Messages)msg.obj);
                    break;
                case ObjectCache.OBJECT_GET_FAILED:
                    Log.v(TAG, "blast it: " + (Exception)msg.obj);
                    finish();
                    break;
                default:
                    throw new RuntimeException("trader got unknown msg: " +
                                               msg.what);
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh(null);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOADING:
            loadingDialog = new ProgressDialog(this);
            dialog = loadingDialog;
            loadingDialog.setMessage
                (getResources().getText(R.string.loading));
            break;
        default:
            throw new RuntimeException("unknown dialog type: " + id);
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.inbox, menu);
        return true;
    }


    /* Callbacks */

    /** Called when a menu item is selected. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.goto_home_menu:
            startActivity(new Intent
                          (Intent.ACTION_VIEW,
                           Uri.EMPTY, this, BookTrader.class)
                          .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        case R.id.refresh_menu:
            refresh(null);
            return true;
        case R.id.new_message_menu:
            newMessage(null);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /** Called when the compose button is pressed. */
    public void newMessage(View v) {
        startActivity(new Intent
                      (Intent.ACTION_VIEW,
                       Uri.EMPTY, this, NewMessage.class));
    }

    /** Called when the refresh button is pressed. */
    public void refresh(View v) {
        if (BookTraderAPI.getInstance().loggedIn)
            ObjectCache.getInstance().getAllMessages(requestHandler);
    }


    /* Handlers */

    void handleMessagesGot(Messages messages) {
        summaryText.setText("Inbox: " + messages.all.size() +
                            " (" + messages.unread.size() + ")");
        messagesAdapter.setData(messages);
    }
}
