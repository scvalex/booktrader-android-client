package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SearchResult {
    /* Debugging */
    static final String TAG = "BookTrader";

    public static class Book {
        String identifier;
        String title;
        String subtitle;
        String publisher;
        List<String> authors;
        String thumbnailSource;
        String smallThumbnailSource;
        Drawable image;

        /** Note: call by name; be careful */
        public Book(String id, String title, String subtitle,
                    String publisher,
                    List<String> authors, String thumbnailSource,
                    String smallThumbnailSource) {
            this.identifier = id;
            this.title = title;
            this.subtitle = subtitle;
            this.publisher = publisher;
            this.authors = authors;
            this.thumbnailSource = thumbnailSource;
            this.smallThumbnailSource = smallThumbnailSource;
        }

        /** Get the best known cover image.  Return null if does not exist. */
        public String getBestCoverSource() {
            String url = thumbnailSource;
            if (url == null || url.length() == 0)
                url = smallThumbnailSource;
            if (url != null && url.length() > 0)
                return url;
            return null;
        }
    }


    /** Used when we scroll past the last result in the search view. */
    static final Book FILLER_BOOK = new Book("", "", "", "",
                                             new ArrayList<String>(),
                                             "", "");


    /* Configuration */
    String query;

    /* Storage for the actual result */
    int totalItems;
    List<Book> books = new ArrayList<Book>();

    /* Request de-duplication */
    Set<Integer> alreadyGetting = new HashSet<Integer>();


    /* Public API */

    private SearchResult() {
    }

    public SearchResult(String query) {
        this.query = query;
    }

    /** Get the book at INDEX.  If the book is not available yet,
     * return FILLER_BOOK and request it.  If the book is not in the
     * result, return null. */
    public synchronized Book get(int index, Handler handler) {
        if (index >= books.size() - 10) {
            int nextIndex = this.books.size();
            if (!alreadyGetting.contains(nextIndex)) {
                Log.v(TAG, "getting more books... " + index);
                /* Temporary memory leak; only for the SearchResult's
                 * lifetime, so I don't care. */
                alreadyGetting.add(nextIndex);
                BookTraderAPI.getInstance().moreSearchResults
                    (this, nextIndex, handler);
            }
            if (index >= books.size())
                return FILLER_BOOK;
        }
        return books.get(index);
    }
}
