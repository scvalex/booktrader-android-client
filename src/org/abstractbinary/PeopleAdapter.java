package org.abstractbinary.booktrader;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


class PeopleAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    Context context;


    /* Public API */

    public PeopleAdapter(Context context) {
        super();

        this.context = context;
    }


    /* Adapter methods. */

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
