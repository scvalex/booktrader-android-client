package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

    /* Debugging */
    static final String TAG = "BookTrader";

    /* Thread pool */
    ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(4);

    /* Singleton */
    private static DownloadCache instance = new DownloadCache(new BasicHttpContext());

    /* Common names */
    static final int DOWNLOAD_DONE = 0;
    static final int DOWNLOAD_ERROR = 1;

    /* net stuff */
    HttpContext httpContext;


    /* Public API */

    private DownloadCache() {
    }

    public DownloadCache(HttpContext context) {
        this.httpContext = context;

        instance = this;
    }

    static public DownloadCache getInstance() {
        return instance;
    }

    void getDrawable(final String url, final Handler handler) {
        final HttpGet httpGet = new HttpGet(url);

        pool.execute(new Runnable() {
                public void run() {
                    try {
                        HttpParams params = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(params, 4000);
                        HttpConnectionParams.setSoTimeout(params, 4000);
                        HttpClient httpClient = new DefaultHttpClient(params);
                        HttpResponse response = httpClient.execute(httpGet, httpContext);
                        Drawable drawable = Drawable.createFromStream(response.getEntity().getContent(), url);
                        sendMessage(handler, DOWNLOAD_DONE,
                                    new DownloadResult(url, drawable));
                    } catch (Exception e) {
                        sendMessage(handler, DOWNLOAD_ERROR,
                                    new DownloadResult(url, e));
                    }
                }
            });
    }


    /* Internal gubbins */

    void sendMessage(Handler handler, int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }
}
