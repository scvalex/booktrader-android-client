package org.abstractbinary.booktrader;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


class MessagesAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    Context context;
    Messages messages;
    Handler handler;
    Map<String, Person> people = new HashMap<String, Person>();

    /* Constructor */

    public MessagesAdapter(Context context) {
        this.context = context;

        handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    DownloadCache.DownloadResult r;
                    BookTraderAPI.PersonResult rp;
                    switch (msg.what) {
                    case DownloadCache.DOWNLOAD_DONE:
                        r = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadDone(r.url, (Drawable)r.result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        r = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadError(r.url, (Exception)r.result);
                        break;
                    case ObjectCache.OBJECT_GET_STARTED:
                        //whooosh
                        break;
                    case ObjectCache.PERSON_GOT:
                        rp = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGot(rp.username, (Person)rp.result);
                        break;
                    case ObjectCache.OBJECT_GET_FAILED:
                        rp = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonFailed(rp.username, (Exception)rp.result);
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
            if (people.containsKey(other)) {
                Person p = people.get(other);
                if (p.avatar != null) {
                    ((ImageView)messageRow.findViewById
                     (R.id.user_avatar_view)).setImageDrawable(p.avatar);
                }
            } else {
                ObjectCache.getInstance().getPersonDetails(other, handler,
                                                           false);
            }
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
        for (Person p : people.values())
            if (p.avatarSource.equals(url)) {
                p.avatar = image;
                break;
            }
        notifyDataSetChanged();
    }

    void handleDownloadError(String url, Exception exception) {
        Log.w(TAG, "image download failed: " + url + " because " + exception);
    }

    void handlePersonGot(String username, Person person) {
        Log.v(TAG, "person details got: " + username);
        people.put(username, person);
        person.getAvatar(handler);
    }

    void handlePersonFailed(String username, Exception exception) {
        Log.w(TAG, "user get failed: " + username + " because " + exception);
    }
}
