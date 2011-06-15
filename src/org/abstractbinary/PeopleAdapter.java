package org.abstractbinary.booktrader;

import android.content.Context;
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


class PeopleAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    Context context;
    List<String> usernames = new ArrayList<String>();
    Map<String, Person> people = new HashMap<String, Person>();
    Handler requestHandler;


    /* Public API */

    public PeopleAdapter(Context context) {
        super();

        this.context = context;

        requestHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.PersonResult r =
                        (BookTraderAPI.PersonResult)msg.obj;
                    switch (msg.what) {
                    case BookTraderAPI.PERSON_GET_START:
                        // whoosh
                        break;
                    case BookTraderAPI.PERSON_GOT:
                        handlePersonGot(r.username, (Person)r.result);
                        break;
                    case BookTraderAPI.PERSON_GET_FAILED:
                        handlePersonGetFailed(r.username, (Exception)r.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type" +
                                                   msg.what);
                    }
                }
            };
    }

    public void setData(List<String> usernames) {
        this.usernames.clear();
        people.clear();
        for (String username : usernames) {
            this.usernames.add(username);
            BookTraderAPI.getInstance().doGetPerson(username, requestHandler);
        }
        notifyDataSetChanged();
    }


    /* Adapter methods. */

    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= getCount() || position < 0)
            return null;

        LinearLayout personRow;
        if (!(convertView instanceof FrameLayout))
            personRow = (LinearLayout)View.inflate(context,
                                                   R.layout.person_row, null);
        else
            personRow = (LinearLayout)convertView;

        Person person = (Person)getItem(position);
        if (person != null && person.avatar != null) {
            ((ImageView)personRow.findViewById
             (R.id.person_avatar)).setImageDrawable(person.avatar);
        } else {
            ((ImageView)personRow.findViewById
             (R.id.person_avatar)).setImageDrawable
                (context.getResources().getDrawable(R.drawable.avatar));
            if (person != null)
                ((TextView)personRow.findViewById
                 (R.id.person_username)).setText(person.username);
            else
                ((TextView)personRow.findViewById
                 (R.id.person_username)).setText(usernames.get(position));
        }

        return personRow;
    }

    public long getItemId(int position) {
        if (position >= getCount() || position < 0)
            return -1;        // FIXME: What does this even do?
        return position;
    }

    public Object getItem(int position) {
        if (position >= getCount() || position < 0)
            return null;
        return people.get(usernames.get(position));
    }

    public int getCount() {
        return usernames.size();
    }


    /* Handlers */

    void handlePersonGot(String username, Person person) {
        people.put(username, person);
        notifyDataSetChanged();
    }

    void handlePersonGetFailed(String username, Exception exception) {
        Log.v(TAG, "failed to get " + username + ": " + exception);
        // whoosh
    }
}
