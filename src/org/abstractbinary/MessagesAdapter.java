package org.abstractbinary.booktrader;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


class MessagesAdapter extends BaseAdapter {
    /* Internal gubbins */
    Context context;

    /* Constructor */

    public MessagesAdapter(Context context) {
        this.context = context;
    }

    /* Adapter methods */

    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public long getItemId(int position) {
        return -1;
    }

    public Object getItem(int position) {
        return null;
    }

    public int getCount() {
        return 0;
    }
}
