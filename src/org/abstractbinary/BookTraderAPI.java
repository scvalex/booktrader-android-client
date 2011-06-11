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
    static final String BASE_URL = "http://abstractbinary.org:6543";
    static final String LOGIN_URL = BASE_URL + "/users/login";
    static final String LOGOUT_URL = BASE_URL + "/users/logut";
    static final String SEARCH_URL = BASE_URL + "/books/search";

    /* Internal API */
    static final int LOGIN_DONE      = 100;
    static final int LOGIN_ERROR     = 101;
    static final int LOGIN_START     = 102;
    static final int LOGOUT_START    = 103;
    static final int LOGOUT_FINISHED = 104;
    static final int LOGOUT_ERROR    = 105;
    static final int SEARCH_START    = 106;
    static final int SEARCH_FINISHED = 107;
    static final int SEARCH_FAILED   = 108;

    /* Network communications */
    HttpClient httpClient;
    HttpContext httpContext = new BasicHttpContext();

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

        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response =
                            httpClient.execute(httpPost, httpContext);
                        CookieStore cookieJar =
                            (CookieStore)httpContext.getAttribute
                            (ClientContext.COOKIE_STORE);
                        boolean loggedIn = false;
                        for (Cookie c : cookieJar.getCookies()) {
                            if (c.getName().equals("auth_tkt")) {
                                loggedIn = true;
                            }
                        }
                        if (loggedIn)
                            sendMessage(handler, LOGIN_DONE, response);
                        else
                            sendMessage(handler, LOGIN_ERROR,
                                        new RuntimeException("no auth tkt"));
                    } catch (Exception e) {
                        sendMessage(handler, LOGIN_ERROR, e);
                    }
                }
            });
        sendMessage(handler, LOGIN_START, null);
    }

    /** Perform the remote logout and switch to not logged in state. */
    void doLogout(final Handler handler) {
        final HttpGet httpGet = new HttpGet(LOGOUT_URL);

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
        sendMessage(handler, LOGOUT_START, null);
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
        uri.appendQueryParameter("start_index", String.valueOf(startIndex));
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

        DownloadCache.getInstance().getString(searchUrl, downloadHandler);

        sendMessage(handler, SEARCH_START, null);
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
        JSONArray jsonResult = json.getJSONArray("result");
        for (int i = 0; i < jsonResult.length(); ++i) {
            JSONObject jsonBook = jsonResult.getJSONObject(i);
            JSONArray jsonAuthors = jsonBook.getJSONArray("authors");
            List<String> authors = new ArrayList<String>();
            for (int j = 0; j < jsonAuthors.length(); ++j)
                authors.add(jsonAuthors.getString(j));
            synchronized (result) {
                result.books.add
                    (new SearchResult.Book
                     (jsonBook.getString("title"),
                      jsonBook.getString("subtitle"),
                      jsonBook.getString("publisher"),
                      authors,
                      jsonBook.getString("thumbnail"),
                      jsonBook.getString("smallThumbnail")));
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
