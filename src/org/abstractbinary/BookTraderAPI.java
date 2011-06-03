package org.abstractbinary;

import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

class BookTraderAPI {
    /* Remote API */
    static final String LOGIN_URL = "http://abstractbinary.org:6543/users/login";

    /* Internal API */
    static final int LOGIN_RESPONSE = 0;
    static final int LOGIN_ERROR    = 1;
    static final int LOGIN_START    = 2;

    Handler handler;

    /* Network communications */
    HttpClient httpClient = AndroidHttpClient.newInstance("BookTrader/0.1");
    HttpContext httpContext = new BasicHttpContext();

    private BookTraderAPI() {
    }

    public BookTraderAPI(Handler handler) {
        this.handler = handler;

        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /** Perform the remote login and switch to login state if successful.
     *  Cheers for:
     *  <a href="http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient">Executing a HTTP POST Request with HttpClient</a> */
    void doLogin(String username, String password) {
        final HttpPost httpPost = new HttpPost(LOGIN_URL);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("username", username));
        values.add(new BasicNameValuePair("password", password));
        values.add(new BasicNameValuePair("Login", "Login"));
        values.add(new BasicNameValuePair("came_from", "/"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(values));
        } catch (Exception e) {
            sendMessage(LOGIN_ERROR, e);
        }

        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpResponse response = httpClient.execute(httpPost, httpContext);
                        sendMessage(LOGIN_RESPONSE, response);
                    } catch (Exception e) {
                        sendMessage(LOGIN_ERROR, e);
                    }
                }
            });
        sendMessage(LOGIN_START, null);
        t.start();
    }

    /** Return the current HttpContext */
    HttpContext getHttpContext() {
        return httpContext;
    }

    void sendMessage(int what, Object obj) {
        handler.sendMessage(Message.obtain(handler, what, obj));
    }
}
