package org.abstractbinary.booktrader;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class PeopleList {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    LinearLayout host;
    Context context;
    List<String> usernames;
    Map<String, Person> people;
    Handler requestHandler;


    /* Public API */

    public PeopleList(LinearLayout host, Context context) {
        super();

        this.host = host;
        this.context = context;

        requestHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.PersonResult r;
                    DownloadCache.DownloadResult rd;
                    switch (msg.what) {
                    case BookTraderAPI.PERSON_GET_START:
                        // whoosh
                        break;
                    case BookTraderAPI.PERSON_GOT:
                        r = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGot(r.username, (Person)r.result);
                        break;
                    case BookTraderAPI.PERSON_GET_FAILED:
                        r = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGetFailed(r.username, (Exception)r.result);
                        break;
                    case DownloadCache.DOWNLOAD_DONE:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadDone(rd.url, (Drawable)rd.result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadError(rd.url, (Exception)rd.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type" +
                                                   msg.what);
                    }
                }
            };
    }

    public void setData(List<String> usernames) {
        if (this.usernames != null)
            this.host.removeAllViews();

        this.usernames = new ArrayList<String>();
        people = new HashMap<String, Person>();
        for (String username : usernames) {
            this.usernames.add(username);
            LinearLayout personRow = (LinearLayout)View.inflate
                (context, R.layout.person_row, null);
            this.host.addView(personRow);
            ((ImageView)personRow.findViewById
             (R.id.person_avatar)).setImageDrawable
                (context.getResources().getDrawable(R.drawable.avatar));
            ((TextView)personRow.findViewById
             (R.id.person_username)).setText(username);
            BookTraderAPI.getInstance().doGetPerson(username, requestHandler);
        }
    }


    /* Handlers */

    void handlePersonGot(String username, Person person) {
        people.put(username, person);
        if (person.avatarSource.length() > 0)
            person.getAvatar(requestHandler);
    }

    void handlePersonGetFailed(String username, Exception exception) {
        Log.v(TAG, "failed to get " + username + ": " + exception);
        // whoosh
    }

    void handleDownloadDone(String url, Drawable image) {
        for (int i = 0; i < usernames.size(); ++i) {
            Person p = people.get(usernames.get(i));
            if (p.avatarSource.equals(url)) {
                p.avatar = image;
                ((ImageView)host.getChildAt(i).findViewById
                 (R.id.person_avatar)).setImageDrawable(p.avatar);
                return;
            }
        }
    }

    void handleDownloadError(String url, Exception e) {
        Log.v(TAG, "failed to download " + url + " because " + e);
        //whoosh
    }
}
