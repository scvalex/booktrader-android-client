package org.abstractbinary.booktrader;

import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class FixedSearchResult extends SearchResult {
    /* Public API */

    public FixedSearchResult(List<Book> books) {
        this.books = new ArrayList<Book>(books);
        this.totalItems = books.size();
    }

    /** Get the book at INDEX. */
    public synchronized Book get(int index, Handler handler) {
        return get(index);
    }
}
