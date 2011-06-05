package org.abstractbinary;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

class BookAdapter extends BaseAdapter {
    Context context;

    public BookAdapter(Context context) {
        super();

        this.context = context;
    }

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
