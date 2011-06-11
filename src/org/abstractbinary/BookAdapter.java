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
                    case BookTraderAPI.SEARCH_START:
                        // wooho!
                        break;
                    case BookTraderAPI.SEARCH_FINISHED:
                        handleSearchResult((SearchResult)msg.obj);
                        break;
                    case BookTraderAPI.SEARCH_FAILED:
                        handleSearchFailed((Exception)msg.obj);
                        break;
                    default:
                        throw new RuntimeException("unknown message: " +
                                                   msg.what);
                    }
                }
            };
    }


    /* Adapter methods */

    public View getView(int position, View convertView, ViewGroup parent) {
        if (result == null)
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
            // Start fetching the cover
            prefetchCover(position);
            if (position % 2 == 0) {
                for (int i = 2; i < 10; i++)
                    prefetchCover(position + i);
            }

            // Display a generic cover in the meantime
            String coverText = book.title;
            if (book.authors.size() > 0) {
                coverText += "\nby\n";
                for (int i = 0; i < book.authors.size(); i++) {
                    coverText += book.authors.get(i);
                    if (i != book.authors.size() - 1)
                        coverText += ", ";
                }
            }
            if (coverText.length() > 70)
                coverText = coverText.substring(0, 70);
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
        if (result == null)
            return null;
        return result.get(position, downloadHandler);
    }

    public int getCount() {
        if (result == null)
            return 0;
        return result.totalItems;
    }


    /* Public api */

    public void displaySearchResult(SearchResult result) {
        this.result = result;
        notifyDataSetChanged();
    }


    /* Internal gubbins */

    void handleDownloadDone(DownloadCache.DownloadResult result) {
        synchronized (result) {
            for (SearchResult.Book book : this.result.books) {
                if (book.getBestCoverSource() == result.url) {
                    book.image = (Drawable)result.result;
                    notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    void handleDownloadError(DownloadCache.DownloadResult result) {
        Log.v(TAG, "image download failed: " + result.url +
              " because " + (Exception)result.result);
        synchronized (result) {
            for (SearchResult.Book book : this.result.books) {
                if (book.getBestCoverSource() == result.url) {
                    book.thumbnailSource = "";
                    book.smallThumbnailSource = "";
                    break;
                }
            }
        }

    }

    /** This is only called for *more* results, so the underlying
     * RESULT should not have changed. */
    void handleSearchResult(SearchResult result) {
        if (result != this.result)
            Log.v(TAG, "wtf?  got different search result: " + result.query);
        displaySearchResult(result);
    }

    void handleSearchFailed(Exception e) {
        Log.v(TAG, "fu.  more search results failed: " + e);
    }


    /* Internal gubbins */

    void prefetchCover(int position) {
        SearchResult.Book book = (SearchResult.Book)getItem(position);
        if (book != SearchResult.FILLER_BOOK && book.image == null) {
            String url = book.getBestCoverSource();
            if (url != null)
                DownloadCache.getInstance().getDrawable(url, downloadHandler);
        }
    }
}
