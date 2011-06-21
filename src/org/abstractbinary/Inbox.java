package org.abstractbinary.booktrader;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;


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

        if (BookTraderAPI.getInstance().loggedIn)
            ObjectCache.getInstance().getAllMessages(requestHandler);
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


    /* Handlers */

    void handleMessagesGot(Messages messages) {
        summaryText.setText("Inbox: " + messages.all.size() +
                            " (" + messages.unread.size() + ")");
        messagesAdapter.setData(messages);
    }
}
