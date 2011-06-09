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
                        handleDownloadDone((DownloadCache.DownloadResult)msg.obj);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        handleDownloadError((DownloadCache.DownloadResult)msg.obj);
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

        SearchResult.Book book = (SearchResult.Book)getItem(position);
        if (book.image != null) { // thumbnail
            bookThumb.findViewById(R.id.book_cover_text).setVisibility(View.INVISIBLE);
            ((ImageView)bookThumb.findViewById(R.id.book_cover_image)).setImageDrawable(book.image);
        } else {                // no thumbnail
            String coverText = book.title;
            if (book.authors.size() > 0) {
                coverText += "\nby\n";
                for (String author : book.authors)
                    coverText += author + ", ";
            }
            ((TextView)bookThumb.findViewById(R.id.book_title)).setText(coverText);
            ((ImageView)bookThumb.findViewById(R.id.book_cover_image)).setImageDrawable(context.getResources().getDrawable(R.drawable.book_thumb));
            bookThumb.findViewById(R.id.book_cover_text).setVisibility(View.VISIBLE);
        }

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
            String url = book.getBestCoverSource();
            if (url != null)
                cache.getDrawable(url, downloadHandler);
        }

        notifyDataSetChanged();
    }


    /* Internal gubbins */

    void handleDownloadDone(DownloadCache.DownloadResult result) {
        Log.v(TAG, "image download done: " + result.url);
        for (SearchResult.Book book : this.result.books) {
            if (book.getBestCoverSource() == result.url) {
                book.image = (Drawable)result.result;
                notifyDataSetChanged();
                break;
            }
        }
    }

    void handleDownloadError(DownloadCache.DownloadResult result) {
        Log.v(TAG, "image download failed: " + result.url +
              " because " + (Exception)result.result);
    }
}
