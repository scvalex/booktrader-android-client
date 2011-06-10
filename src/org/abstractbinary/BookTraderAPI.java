package org.abstractbinary.booktrader;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    static final String BASE_URL = "http://abstractbinary.org:6543";
    static final String LOGIN_URL = BASE_URL + "/users/login";
    static final String LOGOUT_URL = BASE_URL + "/users/logut";
    static final String SEARCH_URL = BASE_URL + "/books/search";

    /* Internal API */
    static final int LOGIN_DONE      = 0;
    static final int LOGIN_ERROR     = 1;
    static final int LOGIN_START     = 2;
    static final int LOGOUT_START    = 3;
    static final int LOGOUT_FINISHED = 4;
    static final int LOGOUT_ERROR    = 5;
    static final int SEARCH_START    = 6;
    static final int SEARCH_FINISHED = 7;
    static final int SEARCH_FAILED   = 8;

    /* Network communications */
    HttpClient httpClient;
    HttpContext httpContext = new BasicHttpContext();

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
    void doLogin(String username, String password, final Handler handler) {
        final HttpPost httpPost = new HttpPost(LOGIN_URL);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("username", username));
        values.add(new BasicNameValuePair("password", password));
        values.add(new BasicNameValuePair("Login", "Login"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(values));
        } catch (Exception e) {
            sendMessage(handler, LOGIN_ERROR, e);
        }

        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response = httpClient.execute(httpPost, httpContext);
                        CookieStore cookieJar = (CookieStore)httpContext.getAttribute(ClientContext.COOKIE_STORE);
                        boolean loggedIn = false;
                        for (Cookie c : cookieJar.getCookies()) {
                            if (c.getName().equals("auth_tkt")) {
                                loggedIn = true;
                            }
                        }
                        if (loggedIn)
                            sendMessage(handler, LOGIN_DONE, response);
                        else
                            sendMessage(handler, LOGIN_ERROR, new RuntimeException("no auth ticket"));
                    } catch (Exception e) {
                        sendMessage(handler, LOGIN_ERROR, e);
                    }
                }
            });
        sendMessage(handler, LOGIN_START, null);
        t.start();
    }

    /** Perform the remote logout and switch to not logged in state. */
    void doLogout(final Handler handler) {
        final HttpGet httpGet = new HttpGet(LOGOUT_URL);

        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response = httpClient.execute(httpGet, httpContext);
                        sendMessage(handler, LOGOUT_FINISHED, null);
                    } catch (Exception e) {
                        sendMessage(handler, LOGOUT_ERROR, e);
                    }
                }
            });
        sendMessage(handler, LOGOUT_START, null);
        t.start();
    }

    /** Perform the search query. */
    void doSearch(String query, final Handler handler) {
        Uri.Builder uri = new Uri.Builder();
        uri.appendQueryParameter("query", query);
        uri.appendQueryParameter("format", "json");
        uri.appendQueryParameter("Search", "Search");
        String searchUrl = SEARCH_URL + uri.build().toString();
        Log.v(TAG, "querrying: " + searchUrl);
        final HttpGet httpGet = new HttpGet(searchUrl);

        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response = httpClient.execute(httpGet, httpContext);
                        SearchResult result = makeResult(response);
                        sendMessage(handler, SEARCH_FINISHED, result);
                    } catch (Exception e) {
                        sendMessage(handler, SEARCH_FAILED, e);
                    }
                }
            });
        sendMessage(handler, SEARCH_START, null);
        t.start();
    }

    SearchResult makeResult(HttpResponse response) throws IOException, JSONException {
        JSONObject json = new JSONObject(responseToString(response));
        if (json.getString("status").equals("error")) {
            throw new RuntimeException(json.getString("reason"));
        }
        SearchResult r = new SearchResult();
        r.totalItems = Integer.valueOf(json.getString("total_items"));
        JSONArray jsonResult = json.getJSONArray("result");
        for (int i = 0; i < jsonResult.length(); ++i) {
            JSONObject jsonBook = jsonResult.getJSONObject(i);
            JSONArray jsonAuthors = jsonBook.getJSONArray("authors");
            List<String> authors = new ArrayList<String>();
            for (int j = 0; j < jsonAuthors.length(); ++j)
                authors.add(jsonAuthors.getString(j));
            r.books.add(new SearchResult.Book(jsonBook.getString("title"),
                                              jsonBook.getString("subtitle"),
                                              jsonBook.getString("publisher"),
                                              authors,
                                              jsonBook.getString("thumbnail"),
                                              jsonBook.getString("smallThumbnail")));
        }
        return r;
    }

    void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }

    /** Return the String body of a HttpResponse. */
    String responseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.getEntity().writeTo(stream);
        return stream.toString();
    }
}
