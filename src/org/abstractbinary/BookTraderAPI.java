package org.abstractbinary.booktrader;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


class BookTraderAPI {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Singleton */
    private static BookTraderAPI instance = new BookTraderAPI();

    /* Remote API */
    static final String BASE_URL   = "http://www.doc.ic.ac.uk/project/2010/271/g1027114";
    static final String USERS_URL  = BASE_URL + "/users";
    static final String LOGIN_URL  = USERS_URL + "/login";
    static final String LOGOUT_URL = USERS_URL + "/logout";
    static final String BOOKS_URL  = BASE_URL + "/books";
    static final String SEARCH_URL = BASE_URL + "/search";
    static final String MESSAGES_URL = BASE_URL + "/messages";
    static final String NEW_MESSAGE_URL = MESSAGES_URL + "/new";

    static class BookDetailsResult {
        String bookIdentifier;
        Object result;

        private BookDetailsResult() {
        }

        public BookDetailsResult(String bookIdentifier, Object result) {
            this.bookIdentifier = bookIdentifier;
            this.result = result;
        }
    }

    static class PersonResult {
        String username;
        Object result;

        private PersonResult() {
        }

        public PersonResult(String username, Object result) {
            this.username = username;
            this.result = result;
        }
    }

    /* Internal API */
    static final int LOGIN_DONE         = 100;
    static final int LOGIN_ERROR        = 101;
    static final int LOGIN_START        = 102;
    static final int LOGOUT_START       = 103;
    static final int LOGOUT_FINISHED    = 104;
    static final int LOGOUT_ERROR       = 105;
    static final int SEARCH_START       = 106;
    static final int SEARCH_FINISHED    = 107;
    static final int SEARCH_FAILED      = 108;
    static final int DETAILS_START      = 109;
    static final int DETAILS_GOT        = 110;
    static final int DETAILS_ERROR      = 111;
    static final int DETAILS_HAVE       = 112;
    static final int DETAILS_WANT       = 113;
    static final int DETAILS_REMOVE     = 114;
    static final int PERSON_GET_START   = 115;
    static final int PERSON_GOT         = 116;
    static final int PERSON_GET_FAILED  = 117;
    static final int ALL_MESSAGES_START = 118;
    static final int MESSAGES_GOT       = 119;
    static final int MESSAGES_ERROR     = 120;
    static final int GET_USERS_DONE     = 121;
    static final int GET_USERS_FAILED   = 122;
    static final int SEND_START         = 123;
    static final int SEND_DONE          = 124;
    static final int SEND_ERROR         = 125;

    /* Network communications */
    HttpClient httpClient;
    HttpContext httpContext = new BasicHttpContext();

    /* State */
    boolean loggedIn;
    String currentUser;

    /* Thread pool */
    /* Note that since we're using a shared HttpClient, we should not
     * have a pool of more than *ONE* thread. */
    ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);

    private BookTraderAPI() {
        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 4000);
        HttpConnectionParams.setSoTimeout(params, 4000);
        httpClient = new DefaultHttpClient(params);
    }

    static public BookTraderAPI getInstance() {
        return instance;
    }

    static public void reset() {
        instance = new BookTraderAPI();
    }

    /** Perform the remote login.
     *  Cheers for:
     *  <a href="http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient">Executing a HTTP POST Request with HttpClient</a> */
    void doLogin(final String username, String password,
                 final Handler handler) {
        final HttpPost httpPost = new HttpPost(LOGIN_URL);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("username", username));
        values.add(new BasicNameValuePair("password", password));
        values.add(new BasicNameValuePair("Login", "Login"));
        values.add(new BasicNameValuePair("format", "json"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(values));
        } catch (Exception e) {
            sendMessage(handler, LOGIN_ERROR, e);
        }

        sendMessage(handler, LOGIN_START, null);
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpPost, httpContext);
                        CookieStore cookieJar =
                            (CookieStore)httpContext.getAttribute
                            (ClientContext.COOKIE_STORE);
                        loggedIn = false;
                        currentUser = null;
                        for (Cookie c : cookieJar.getCookies()) {
                            if (c.getName().equals("auth_tkt")) {
                                loggedIn = true;
                            }
                        }
                        if (loggedIn) {
                            currentUser = username;
                            sendMessage(handler, LOGIN_DONE, response);
                        } else {
                            sendMessage(handler, LOGIN_ERROR,
                                        new RuntimeException("no auth tkt"));
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "Login error " + e.getCause());
                        sendMessage(handler, LOGIN_ERROR, e);
                    }
                }
            });
    }

    /** Perform the remote logout and switch to not logged in state. */
    void doLogout(final Handler handler) {
        final HttpGet httpGet = new HttpGet(LOGOUT_URL);

        sendMessage(handler, LOGOUT_START, null);
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        sendMessage(handler, LOGOUT_FINISHED, null);
                    } catch (Exception e) {
                        sendMessage(handler, LOGOUT_ERROR, e);
                    }
                }
            });
    }

    /** Perform the search query. */
    void doSearch(String query, final Handler handler) {
        doSearch(query, new SearchResult(query), 0, handler);
    }

    /** Get more search results for result. */
    void moreSearchResults(SearchResult result, int startIndex,
                           Handler handler) {
        doSearch(result.query, result, startIndex, handler);
    }

    /** Perform the search query by appending to RESULT. */
    void doSearch(String query, final SearchResult result,
                  int startIndex, final Handler handler) {
        Uri.Builder uri = new Uri.Builder();
        uri.appendQueryParameter("query", query);
        uri.appendQueryParameter("type", "books");
        uri.appendQueryParameter("start_index", String.valueOf(startIndex));
        uri.appendQueryParameter("limit", String.valueOf(20));
        uri.appendQueryParameter("format", "json");
        uri.appendQueryParameter("Search", "Search");
        String searchUrl = SEARCH_URL + uri.build().toString();
        Log.v(TAG, "querrying: " + searchUrl);

        Handler downloadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case DownloadCache.DOWNLOAD_DONE:
                        try {
                            updateResult(result,
                                         (String)(
                                             (DownloadCache.DownloadResult)
                                             msg.obj).result);
                        } catch (Exception e) {
                            BookTraderAPI.sendMessage(handler, SEARCH_FAILED,
                                                      e);
                        }
                        BookTraderAPI.sendMessage(handler, SEARCH_FINISHED,
                                                  result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        BookTraderAPI.sendMessage
                            (handler, SEARCH_FAILED,
                             (Exception)(
                                 (DownloadCache.DownloadResult)
                                 msg.obj).result);
                        break;
                    default:
                        throw new RuntimeException("unknown message: " +
                                                   msg.what);
                    }
                }
            };

        sendMessage(handler, SEARCH_START, null);
        DownloadCache.getInstance().getString(searchUrl, downloadHandler);
    }

    /** Get the a book's details. */
    void doGetBookDetails(final String bookIdentifier,
                          final Handler handler) {
        final HttpGet httpGet = new HttpGet(BOOKS_URL +
                                            "/" + bookIdentifier +
                                            "?format=json");

        sendMessage(handler, DETAILS_START,
                    new BookDetailsResult(bookIdentifier, null));
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        json = json.getJSONObject("book");
                        sendMessage(handler, DETAILS_GOT,
                                    new BookDetailsResult(bookIdentifier,
                                                          new Book(json)));
                    } catch (Exception e) {
                        sendMessage(handler, DETAILS_ERROR,
                                    new BookDetailsResult(bookIdentifier, e));
                    }
                }
            });
    }

    /** Get all a user's messages. */
    void doGetAllMessages(final Handler handler) {
        final HttpGet httpGet = new HttpGet(MESSAGES_URL +
                                            "/list" +
                                            "?format=json");

        sendMessage(handler, ALL_MESSAGES_START, null);
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        sendMessage(handler, MESSAGES_GOT,
                                    new Messages(json));
                    } catch (Exception e) {
                        sendMessage(handler, MESSAGES_ERROR, e);
                    }
                }
            });
    }

    /** Get all a usernames. */
    void doGetAllUsers(final Handler handler) {
        final HttpGet httpGet = new HttpGet(USERS_URL + "?format=json");

        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        if (json.getString("status").equals("error"))
                            throw new RuntimeException
                                ("error alling:" + json.getString("reason"));
                        JSONArray jsonUsers = json.getJSONArray("users");
                        List<String> users = new ArrayList<String>();
                        for (int i = 0; i < jsonUsers.length(); ++i)
                            users.add(jsonUsers.getString(i));
                        sendMessage(handler, GET_USERS_DONE, users);
                    } catch (Exception e) {
                        sendMessage(handler, GET_USERS_FAILED, e);
                    }
                }
            });
    }

    /** Have a book. */
    void doHave(String bookIdentifier, Handler handler) {
        doSomething("have", DETAILS_HAVE, bookIdentifier, handler);
    }

    /** Have a book. */
    void doWant(String bookIdentifier, Handler handler) {
        doSomething("want", DETAILS_WANT, bookIdentifier, handler);
    }

    /** Have a book. */
    void doRemove(String bookIdentifier, Handler handler) {
        doSomething("remove", DETAILS_REMOVE, bookIdentifier, handler);
    }

    /** Do something naughty to a book. */
    void doSomething(final String what, final int whatCode,
                     final String bookIdentifier, final Handler handler) {
        final HttpGet httpGet = new HttpGet(BOOKS_URL +
                                            "/" + bookIdentifier +
                                            "/" + what +
                                            "?format=json");

        sendMessage(handler, DETAILS_START, null);
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        if (json.getString("status").equals("error"))
                            throw new RuntimeException
                                ("error having " + json.getString("reason"));
                        sendMessage(handler, whatCode, bookIdentifier);
                    } catch (Exception e) {
                        sendMessage(handler, DETAILS_ERROR, e);
                    }
                }
            });
    }

    /** Get a person's details. */
    void doGetPerson(final String username, final Handler handler) {
        final HttpGet httpGet = new HttpGet(USERS_URL +
                                            "/" + username +
                                            "?format=json");

        sendMessage(handler, PERSON_GET_START,
                    new PersonResult(username, null));
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpGet, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        if (json.getString("status").equals("error"))
                            throw new RuntimeException
                                ("error persing" + json.getString("reason"));
                        sendMessage(handler, PERSON_GOT,
                                    new PersonResult(username,
                                                     new Person(username,
                                                                json)));

                    } catch (Exception e) {
                        sendMessage(handler, PERSON_GET_FAILED,
                                    new PersonResult(username, e));
                    }
                }
            });
    }

    /** Get all a usernames. */
    void doSendMessage(String recipient, String subject, String body,
                       final Handler handler) {
        final HttpPost httpPost = new HttpPost(NEW_MESSAGE_URL);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("recipient", recipient));
        values.add(new BasicNameValuePair("subject", subject));
        values.add(new BasicNameValuePair("body", body));
        values.add(new BasicNameValuePair("Send", "Send"));
        values.add(new BasicNameValuePair("format", "json"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(values));
        } catch (Exception e) {
            sendMessage(handler, SEND_ERROR, e);
        }

        sendMessage(handler, SEND_START, null);
        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpPost, httpContext);
                        JSONObject json =
                            new JSONObject(responseToString(response));
                        if (json.getString("status").equals("error"))
                            throw new RuntimeException
                                ("error sending:" + json.getString("reason"));
                        sendMessage(handler, SEND_DONE, null);
                    } catch (Exception e) {
                        Log.v(TAG, "Send error " + e.getCause());
                        sendMessage(handler, SEND_ERROR, e);
                    }
                }
            });
    }

    /** Update the given RESULT by adding new books from RESPONSE. */
    void updateResult(SearchResult result, String response)
        throws IOException, JSONException
    {
        JSONObject json = new JSONObject(response);
        if (json.getString("status").equals("error")) {
            throw new RuntimeException(json.getString("reason"));
        }
        result.totalItems = Integer.valueOf(json.getString("total_items"));
        JSONArray jsonResult = json.getJSONArray("google_books");
        ObjectCache bc = ObjectCache.getInstance();
        for (int i = 0; i < jsonResult.length(); ++i) {
            JSONObject jsonBook = jsonResult.getJSONObject(i);
            synchronized (result) {
                Book book = new Book(jsonBook);
                bc.insertBook(book);
                result.books.add(book);
            }
        }
    }


    /* Utility */

    static void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }

    /** Return the String body of a HttpResponse. */
    String responseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.getEntity().writeTo(stream);
        return stream.toString();
    }
}
