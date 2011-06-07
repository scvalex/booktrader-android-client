package org.abstractbinary.booktrader;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;


class BookAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    Context context;
    SearchResult result = null;

    public BookAdapter(Context context) {
        super();

        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (result == null || position >= result.books.size())
            return null;

        LinearLayout bookThumb;
        if (convertView == null || !(convertView instanceof LinearLayout))
            bookThumb = (LinearLayout)View.inflate(context, R.layout.book_thumb, null);
        else
            bookThumb = (LinearLayout)convertView;

        ((TextView)bookThumb.findViewById(R.id.book_title)).setText(result.books.get(position).title);

        return bookThumb;
    }

    public long getItemId(int position) {
        if (result == null)
            return -1;
        return position;        // FIXME: wtf is this?
    }

    public Object getItem(int position) {
        if (result == null || position >= result.books.size())
            return null;
        return result.books.get(position);
    }

    public int getCount() {
        if (result == null)
            return 0;
        return result.books.size();
    }

    public void displaySearchResult(SearchResult result) {
        this.result = result;
        notifyDataSetChanged();
    }
}
