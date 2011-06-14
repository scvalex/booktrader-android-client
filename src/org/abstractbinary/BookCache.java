package org.abstractbinary.booktrader;

import android.os.Handler;

class BookCache {
    /** Singleton */
    static private BookCache instance = new BookCache();

    /* Public API */
    static BookCache getInstance() {
        return instance;
    }

    void getBookDetails(String bookIdentifier, Handler handler) {
        BookTraderAPI.getInstance().doGetBookDetails(bookIdentifier, handler);
    }
}
