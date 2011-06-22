package org.abstractbinary.booktrader;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;


class MessagesAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    Context context;
    Messages messages;
    Handler downloadHandler;

    /* Constructor */

    public MessagesAdapter(Context context) {
        this.context = context;

        downloadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    DownloadCache.DownloadResult r;
                    switch (msg.what) {
                    case DownloadCache.DOWNLOAD_DONE:
                        r = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadDone(r.url, (Drawable)r.result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        r = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadError(r.url, (Exception)r.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message: " +
                                                   msg.what);
                    };
                }
            };
    }

    /* Adapter methods */

    public View getView(int position, View convertView, ViewGroup parent) {
        if (messages == null || position < 0 || position >= getCount())
            return null;

        LinearLayout messageRow;
        // FIXME user the id instead of instanceof to check
        if (convertView == null || !(convertView instanceof LinearLayout))
            messageRow = (LinearLayout)View.inflate(context, R.layout.message_row, null);
        else
            messageRow = (LinearLayout)convertView;

        Messages.Message first =
            ((List<Messages.Message>)getItem(position)).get(0);
        if (first.apples != null) {
            ((TextView)messageRow.findViewById(R.id.message_subject)).setText(first.subject);
        } else {
            String other = first.recipient;
            if (other.equals(BookTraderAPI.getInstance().currentUser))
                other = first.sender;
            ((TextView)messageRow.findViewById(R.id.message_subject)).setText(other);
        }

        return messageRow;
    }

    public long getItemId(int position) {
        if (messages == null || position < 0 || position >= getCount())
            return -1;
        return position;
    }

    public Object getItem(int position) {
        if (messages == null || position < 0 || position >= getCount())
            return null;
        return messages.messages.get(messages.all.get(position));
    }

    public int getCount() {
        if (messages == null)
            return 0;
        return messages.all.size();
    }


    /* Public API */

    public void setData(Messages messages) {
        if (messages == null)
            return;

        this.messages = messages;

        notifyDataSetChanged();
    }


    /* Handlers */

    void handleDownloadDone(String url, Drawable image) {
        Log.v(TAG, "image download done: " + url);
    }

    void handleDownloadError(String url, Exception exception) {
        Log.w(TAG, "image download failed: " + url + " because " + exception);
    }
}
