package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.common.io.CharStreams;


class DownloadCache {
    static class DownloadResult {
        String url;
        Object result;

        private DownloadResult() {
        }

        public DownloadResult(String url, Object result) {
            this.url = url;
            this.result = result;
        }
    }

    interface ResponseHandler {
        public void handleResponse(InputStream respStream);
    }

    /* Debugging */
    static final String TAG = "BookTrader";

    /* Thread pool */
    ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(4);

    /* Singleton */
    private static DownloadCache instance = new DownloadCache();

    /* Common names */
    static final int DOWNLOAD_DONE  = 200;
    static final int DOWNLOAD_ERROR = 201;

    /* net stuff */
    HttpContext httpContext;

    /* Cache on db stuff */
    BookTraderOpenHelper dbHelper;


    /* Public API */

    private DownloadCache() {
        httpContext = new BasicHttpContext();
    }

    static public DownloadCache getInstance() {
        return instance;
    }

    void setHttpContext(HttpContext newContext) {
        this.httpContext = newContext;
    }

    void setDbHelper(BookTraderOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /** Get URL's value as a String.  First try the cache.  Fall back
     * to normal HTTP requests. */
    void getString(final String url, final Handler handler) {
        try {
            sendMessage(handler, DOWNLOAD_DONE,
                        new DownloadResult(url, new String(getCached(url))));
            return;
        } catch (Exception e) {
            // whoosh
        }

        getHttp(url, handler, new ResponseHandler() {
                public void handleResponse(InputStream respStream) {
                    try {
                        sendMessage
                            (handler, DOWNLOAD_DONE, new DownloadResult
                             (url, CharStreams.toString
                              (new InputStreamReader(respStream))));
                    } catch (Exception e) {
                        sendMessage(handler, DOWNLOAD_ERROR,
                                    new DownloadResult(url, e));
                    }
                }
            });
    }

    /** Get URL's value as a Drawable.  First try the cache.  Fall
     * back to normal HTTP requests. */
    void getDrawable(final String url, final Handler handler) {
        try {
            sendMessage
                (handler, DOWNLOAD_DONE, new DownloadResult
                 (url, Drawable.createFromStream
                  (new ByteArrayInputStream(getCached(url)), "cache on db")));
            return;
        } catch (Exception e) {
            // whoosh
        }

        getHttp(url, handler, new ResponseHandler() {
                public void handleResponse(InputStream respStream) {
                    try {
                        sendMessage
                            (handler, DOWNLOAD_DONE, new DownloadResult
                             (url,
                              Drawable.createFromStream(respStream, url)));
                    } catch (Exception e) {
                        sendMessage(handler, DOWNLOAD_ERROR,
                                    new DownloadResult(url, e));
                    }

                }
            });
    }


    /* Internal gubbins */

    /** Get the requested URL from the cache.  If it doesn't exist or
     * if the cache isn't installed, throw an error. */
    byte[] getCached(String url) {
        if (dbHelper == null)
            throw new RuntimeException("no cache installed");

        byte[] fromDb = dbHelper.cacheQuery(url);
        if (fromDb == null)
            throw new RuntimeException("not in cache");

        return fromDb;
    }

    /** Get the requested URL via plain old HTTP.  Before executing
     * the response handler, insert the data into the cache. */
    void getHttp(final String url, final Handler handler,
                 final ResponseHandler responseHandler) {
        final HttpGet httpGet = new HttpGet(url);

        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpParams params = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(params, 4000);
                        HttpConnectionParams.setSoTimeout(params, 4000);
                        HttpClient httpClient = new DefaultHttpClient(params);
                        HttpResponse response = httpClient.execute(httpGet, httpContext);

                        InputStream fromDb;
                        if (dbHelper != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            response.getEntity().writeTo(stream);
                            fromDb = new ByteArrayInputStream(stream.toByteArray());
                            dbHelper.cacheInsert(url, stream.toByteArray());
                        } else {
                            fromDb = response.getEntity().getContent();
                        }

                        responseHandler.handleResponse(fromDb);
                    } catch (Exception e) {
                        sendMessage(handler, DOWNLOAD_ERROR,
                                    new DownloadResult(url, e));
                    }
                }
            });
    }

    void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }
}
