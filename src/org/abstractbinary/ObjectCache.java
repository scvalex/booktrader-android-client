package org.abstractbinary.booktrader;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
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
    static final int MESSAGES_GOT       = 304;
    static final int USERS_GOT          = 305;
    static final String MESSAGES_LIST_KEY = "/messages/list";
    static final String USERS_LIST_KEY = "/users";

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
                    case BookTraderAPI.ALL_MESSAGES_START:
                        // whoosh!
                        break;
                    case BookTraderAPI.MESSAGES_GOT:
                        handleMessagesGot((Messages)msg.obj);
                        break;
                    case BookTraderAPI.MESSAGES_ERROR:
                        handleMessagesError((Exception)msg.obj);
                        break;
                    case BookTraderAPI.GET_USERS_DONE:
                        handleUsersGot((List<String>)msg.obj);
                        break;
                    case BookTraderAPI.GET_USERS_FAILED:
                        handleUsersError((Exception)msg.obj);
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

    /** Get al la user's details. */
    void getAllMessages(final Handler handler) {
        getCached(MESSAGES_LIST_KEY, handler, true,
                  new CacheHandler() {
                      public void handleCached(byte[] fromDb)
                          throws Exception
                      {
                          sendMessage(handler, MESSAGES_GOT,
                                      new Messages
                                      (new JSONObject
                                       (new String(fromDb))));
                      }

                      public void getFromAPI() {
                          BookTraderAPI.getInstance().doGetAllMessages
                              (detailsHandler);
                      }
                  });
    }

    /** Get al la user's details. */
    void getAllUsers(final Handler handler) {
        getCached(USERS_LIST_KEY, handler, true,
                  new CacheHandler() {
                      public void handleCached(byte[] fromDb)
                          throws Exception
                      {
                          JSONArray jsonUsers  =
                              new JSONArray(new String(fromDb));
                          List<String> users = new ArrayList<String>();
                          for (int i = 0; i < jsonUsers.length(); ++i)
                              users.add(jsonUsers.getString(i));
                          sendMessage(handler, USERS_GOT, users);
                      }

                      public void getFromAPI() {
                          BookTraderAPI.getInstance().doGetAllUsers
                              (detailsHandler);
                      }
                  });
    }

    /** Get a book's details. */
    void getBookDetails(final String bookIdentifier, final Handler handler) {
        getCached(bookIdentifier, handler, true,
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

    /** Get a person's details always refreshing. */
    void getPersonDetails(String username, Handler handler) {
        getPersonDetails(username, handler, true);
    }

    /** Get a person's details. */
    void getPersonDetails(final String username, final Handler handler,
                          boolean refresh) {
        getCached(username, handler, refresh,
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
    void getCached(String key, Handler handler, boolean refresh,
                   CacheHandler cacheHandler) {
        sendMessage(handler, OBJECT_GET_STARTED, null);
        try {
            if (dbHelper == null)
                throw new RuntimeException("no cache installed");

            byte[] fromDb = dbHelper.cacheQuery(key);
            if (fromDb == null)
                throw new RuntimeException("not in cache");

            cacheHandler.handleCached(fromDb);

            if (!refresh)
                return;
        } catch (Exception e) {
        }

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

    synchronized void insertMessages(Messages messages) {
        dbHelper.cacheRemove(MESSAGES_LIST_KEY);
        dbHelper.cacheInsert(MESSAGES_LIST_KEY,
                             messages.jsonString.getBytes());
    }

    synchronized void insertUsers(List<String> users) {
        dbHelper.cacheRemove(USERS_LIST_KEY);
        dbHelper.cacheInsert(USERS_LIST_KEY,
                             (new JSONArray(users)).toString().getBytes());
    }


    /* Handlers */

    void handleDetailsGot(String bookIdentifier, Book book) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, BOOK_GOT, book);
            insertBook(book);
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception getting details: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handleDetailsError(String bookIdentifier, Exception exception) {
        try {
            Handler handler = requestHandlers.get(bookIdentifier);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(bookIdentifier);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception err'ing details: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handlePersonGot(String username,
                         BookTraderAPI.PersonResult personResult) {
        try {
            Handler handler = requestHandlers.get(username);
            if (handler == null)
                Log.e(TAG, "Handler is null!");
            sendMessage(handler, PERSON_GOT, personResult);
            insertPerson((Person)personResult.result);
            requestHandlers.remove(username);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception getting person: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handlePersonError(String username, Exception exception) {
        try {
            Handler handler = requestHandlers.get(username);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(username);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception err'ing person: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handleMessagesGot(Messages messages) {
        try {
            Handler handler = requestHandlers.get(MESSAGES_LIST_KEY);
            sendMessage(handler, MESSAGES_GOT, messages);
            insertMessages(messages);
            requestHandlers.remove(MESSAGES_LIST_KEY);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception getting messages: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handleMessagesError(Exception exception) {
        try {
            Handler handler = requestHandlers.get(MESSAGES_LIST_KEY);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(MESSAGES_LIST_KEY);
        } catch (Exception e) {
            Log.e(TAG,  "Nested exception erring messages: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handleUsersGot(List<String> users) {
        try {
            Handler handler = requestHandlers.get(USERS_LIST_KEY);
            sendMessage(handler, USERS_GOT, users);
            insertUsers(users);
            requestHandlers.remove(USERS_LIST_KEY);
        } catch (Exception e) {
            Log.e(TAG, "Nested exception getting users: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    void handleUsersError(Exception exception) {
        try {
            Handler handler = requestHandlers.get(USERS_LIST_KEY);
            sendMessage(handler, OBJECT_GET_FAILED, exception);
            requestHandlers.remove(USERS_LIST_KEY);
        } catch (Exception e) {
            Log.e(TAG,  "Nested exception erring users: " +
                  e + " (" + e.getCause() + ")");
            // whoosh
        }
    }

    /* Utility */

    static void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }
}
