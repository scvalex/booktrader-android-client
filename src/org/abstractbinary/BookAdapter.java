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
import android.widget.TextView;
import android.widget.Toast;


class BookAdapter extends BaseAdapter {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Commonly used stuff */
    Context context;
    SearchResult result = null;
    Handler downloadHandler;


    /* Constructor */

    public BookAdapter(Context context) {
        super();

        this.context = context;
        downloadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case DownloadCache.DOWNLOAD_DONE:
                        handleDownloadDone((Drawable)msg.obj);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        handleDownloadError((Exception)msg.obj);
                        break;
                    default:
                        throw new RuntimeException("unknown message: " + msg.what);
                    }
                }
            };
    }


    /* Adapter methods */

    public View getView(int position, View convertView, ViewGroup parent) {
        if (result == null || position >= result.books.size())
            return null;

        FrameLayout bookThumb;
        if (convertView == null || !(convertView instanceof FrameLayout))
            bookThumb = (FrameLayout)View.inflate(context, R.layout.book_thumb, null);
        else
            bookThumb = (FrameLayout)convertView;

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


    /* Public api */

    public void displaySearchResult(SearchResult result) {
        this.result = result;

        DownloadCache cache = DownloadCache.getInstance();
        for (SearchResult.Book book : result.books) {
            String url = book.thumbnailSource;
            if (url == null || url.length() == 0)
                url = book.smallThumbnailSource;
            if (url != null && url.length() > 0)
                cache.getDrawable(url, downloadHandler);
        }

        notifyDataSetChanged();
    }


    /* Internal gubbins */

    void handleDownloadDone(Drawable image) {
        Log.v(TAG, "image download done");
        Toast.makeText(context, "image downloaded", Toast.LENGTH_SHORT).show();
    }

    void handleDownloadError(Exception e) {
        Log.v(TAG, "image download failed: " + e);
    }
}
