package org.abstractbinary.booktrader;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SearchResult {
    /* Debugging */
    static final String TAG = "BookTrader";

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
                return Book.FILLER_BOOK;
        }
        return books.get(index);
    }
}
