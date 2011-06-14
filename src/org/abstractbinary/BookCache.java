package org.abstractbinary.booktrader;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;


class BookCache {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Static constants */
    static final int BOOK_GET_STARTED = 300;
    static final int BOOK_GOT         = 301;
    static final int BOOK_GET_FAILED  = 302;

    /* Cache on db stuff */
    BookTraderOpenHelper dbHelper;

    /* Internal and external handlers */
    Map<String, Handler> requestHandlers = new HashMap<String, Handler>();
    Handler detailsHandler;

    /** Singleton */
    static private BookCache instance = new BookCache();

    private BookCache() {
        detailsHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.BookDetailsResult r;
                    switch (msg.what) {
                    case BookTraderAPI.DETAILS_START:
                        // already sent out; whoosh!
                        break;
                    case BookTraderAPI.DETAILS_GOT:
                        r = (BookTraderAPI.BookDetailsResult)msg.obj;
                        handleDetailsGot(r.bookIdentifier, (Book)r.result);
                        break;
                    case BookTraderAPI.DETAILS_ERROR:
                        r = (BookTraderAPI.BookDetailsResult)msg.obj;
                        handleDetailsError(r.bookIdentifier,
                                           (Exception)r.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type: " +
                                                   msg.what);
                    }
                }
            };
    }

    /* Public API */
    static BookCache getInstance() {
        return instance;
    }

    void setDbHelper(BookTraderOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /** Get a book's details, first from cache, then from HTTP API.
     * Note that this might send *TWO* messages back (one for the
     * cache and one for the API). */
    void getBookDetails(String bookIdentifier, Handler handler) {
        sendMessage(handler, BOOK_GET_STARTED, null);
        try {
            if (dbHelper == null)
                throw new RuntimeException("no cache installed");

            byte[] fromDb = dbHelper.cacheQuery(bookIdentifier);
            if (fromDb == null)
                throw new RuntimeException("not in cache");

            sendMessage(handler, BOOK_GOT, new Book
                        (new JSONObject(new String(fromDb))));
        } catch (Exception e) {
        }

        // fall back to API regardless

        requestHandlers.put(bookIdentifier, handler);
        BookTraderAPI.getInstance().doGetBookDetails(bookIdentifier,
                                                     this.detailsHandler);
    }


    /* Handlers */

    void handleDetailsGot(String bookIdentifier, Book book) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, BOOK_GOT, book);
            dbHelper.cacheRemove(bookIdentifier);
            dbHelper.cacheInsert(bookIdentifier, book.jsonString.getBytes());
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.v(TAG, "Except: " + e);
            // whoosh
        }
    }

    void handleDetailsError(String bookIdentifier, Exception exception) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, BOOK_GET_FAILED, exception);
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.v(TAG, "Except: " + e);
            // whoosh
        }
    }


    /* Utility */

    static void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }
}
