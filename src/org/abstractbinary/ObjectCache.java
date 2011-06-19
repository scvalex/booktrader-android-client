package org.abstractbinary.booktrader;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;


class ObjectCache {
    /* Debugging */
    static final String TAG = "BookTrader";

    interface CacheHandler {
        public void handleCached(byte[] fromDb) throws Exception;
        public void getFromAPI();
    }

    /* Static constants */
    static final int OBJECT_GET_STARTED = 300;
    static final int BOOK_GOT           = 301;
    static final int PERSON_GOT         = 302;
    static final int OBJECT_GET_FAILED  = 303;

    /* Cache on db stuff */
    BookTraderOpenHelper dbHelper;

    /* Internal and external handlers */
    Map<String, Handler> requestHandlers = new HashMap<String, Handler>();
    Handler detailsHandler;

    /** Singleton */
    static private ObjectCache instance = new ObjectCache();

    private ObjectCache() {
        detailsHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.BookDetailsResult r;
                    BookTraderAPI.PersonResult rp;
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
                    case BookTraderAPI.PERSON_GET_START:
                        // whoosh!
                        break;
                    case BookTraderAPI.PERSON_GOT:
                        rp = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGot(rp.username, rp);
                        break;
                    case BookTraderAPI.PERSON_GET_FAILED:
                        rp = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonError(rp.username, (Exception)rp.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type: " +
                                                   msg.what);
                    }
                }
            };
    }

    /* Public API */
    static ObjectCache getInstance() {
        return instance;
    }

    void setDbHelper(BookTraderOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /** Get a book's details. */
    void getBookDetails(final String bookIdentifier, final Handler handler) {
        getCached(bookIdentifier, handler,
                  new CacheHandler() {
                      public void handleCached(byte[] fromDb)
                          throws Exception
                      {
                          sendMessage(handler, BOOK_GOT, new Book
                                      (new JSONObject(new String(fromDb))));
                      }

                      public void getFromAPI() {
                          BookTraderAPI.getInstance().doGetBookDetails
                              (bookIdentifier, detailsHandler);
                      }
                  });
    }

    /** Get a person's details. */
    void getPersonDetails(final String username, final Handler handler) {
        getCached(username, handler,
                  new CacheHandler() {
                      public void handleCached(byte[] fromDb)
                          throws Exception
                      {
                          sendMessage
                              (handler, PERSON_GOT,
                               new BookTraderAPI.PersonResult
                               (username, new Person
                                (username,
                                 new JSONObject(new String(fromDb)))));
                      }

                      public void getFromAPI() {
                          BookTraderAPI.getInstance().doGetPerson
                              (username, detailsHandler);
                      }
                  });
    }

    /** Get an object, first from cache, then from HTTP API.
     * Note that this might send *TWO* messages back (one for the
     * cache and one for the API). */
    void getCached(String key, Handler handler, CacheHandler cacheHandler) {
        sendMessage(handler, OBJECT_GET_STARTED, null);
        try {
            if (dbHelper == null)
                throw new RuntimeException("no cache installed");

            byte[] fromDb = dbHelper.cacheQuery(key);
            if (fromDb == null)
                throw new RuntimeException("not in cache");

            cacheHandler.handleCached(fromDb);
        } catch (Exception e) {
        }

        // fall back to API regardless

        requestHandlers.put(key, handler);
        cacheHandler.getFromAPI();
    }

    /** Insert a book into the cache (or replace the existing one). */
    synchronized void insertBook(Book book) {
        dbHelper.cacheRemove(book.identifier);
        dbHelper.cacheInsert(book.identifier, book.jsonString.getBytes());
    }

    /** Insert a person into the cache (or replace the existing one). */
    synchronized void insertPerson(Person person) {
        dbHelper.cacheRemove(person.username);
        dbHelper.cacheInsert(person.username, person.jsonString.getBytes());
    }


    /* Handlers */

    void handleDetailsGot(String bookIdentifier, Book book) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, BOOK_GOT, book);
            insertBook(book);
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.v(TAG, "Except: " + e);
            // whoosh
        }
    }

    void handleDetailsError(String bookIdentifier, Exception exception) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.v(TAG, "Except: " + e);
            // whoosh
        }
    }

    void handlePersonGot(String username,
                         BookTraderAPI.PersonResult personResult) {
        try {
            Handler handler = requestHandlers.get(username);
            sendMessage(handler, PERSON_GOT, personResult);
            insertPerson((Person)personResult.result);
            requestHandlers.remove(username);
        } catch (Exception e) {
            Log.v(TAG, "Except: " + e);
            // whoosh
        }
    }

    void handlePersonError(String username, Exception exception) {
        try {
            Handler handler = requestHandlers.get(username);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(username);
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
