package org.abstractbinary;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.http.Header;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.client.protocol.ClientContext;
import android.net.http.AndroidHttpClient;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import android.os.Handler;
import android.os.Message;

public class BookTrader extends Activity {
    /* Remote API */
    static final String LOGIN_URL = "http://146.169.25.146:6543/users/login";

    /* Debugging */
    static final String TAG = "BookTrader";

    /* Login-state stuff */
    static final int STATE_NOT_LOGGED_IN = 0;
    static final int STATE_LOGGING_IN = 1;
    static final int STATE_LOGGED_IN = 2;
    int state;
    Map<Integer, View> loginStates = new HashMap<Integer, View>();

    /* Network communications */
    HttpClient httpClient = AndroidHttpClient.newInstance("BookTrader/0.1");
    HttpContext httpContext = new BasicHttpContext();
    static final int LOGIN_RESPONSE = 0;
    static final int LOGIN_ERROR = 1;
    Handler loginHandler;

    /* Dialogs */
    static final int DIALOG_LOGIN = 0;

    /* Often used widgets */
    FrameLayout menuBar;
    Button loginButton;

    /* Internal gubbins */
    String username = null;
    String password = null;


    /* Application life-cycle */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        menuBar = (FrameLayout)findViewById(R.id.menu_bar);

        loginButton = (Button)findViewById(R.id.login_button);

        populateLoginStates();
        state = STATE_NOT_LOGGED_IN;

        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        loginHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case LOGIN_RESPONSE:
                    BookTrader.this.handleLoginResponse((HttpResponse)msg.obj);
                    break;
                case LOGIN_ERROR:
                    BookTrader.this.handleLoginFailure((Exception)msg.obj);
                    break;
                }
            }
        };

        Log.v(TAG, "BookTrader running...");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOGIN:
            dialog = new Dialog(this);

            dialog.setContentView(R.layout.login_dialog);
            dialog.setTitle("Login");

            final EditText usernameField =
                (EditText)dialog.findViewById(R.id.username_field);
            if (username != null) {
                usernameField.setText(username);
            }
            final EditText passwordField =
                (EditText)dialog.findViewById(R.id.password_field);
            if (password != null) {
                passwordField.setText(password);
            }
            final Button button =
                (Button)dialog.findViewById(R.id.login_dialog_button);
            button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        username = usernameField.getText().toString();
                        password = passwordField.getText().toString();
                        Log.v(TAG, "New login info: " + username);
                        dialog.dismiss();
                        doLogin();
                    }
            });

            break;
        default:
            throw new RuntimeException("Unknown dialog type: " + id);
        }
        return dialog;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "screen orientation changed");
        super.onConfigurationChanged(newConfig);
    }


    /* Event handlers */

    /** Called when the login button is pressed. */
    public void logIn(View v) {
        switchState(STATE_LOGGING_IN);
    }


    /* Helpers */

    /** Set NEWSTATE as the current state and show the appropriate View. */
    void switchState(int newState) {
        switch (state) {
        default:
            loginButton.setEnabled(false);
            break;
        }

        View v = loginStates.get(newState);
        menuBar.removeView(v);
        menuBar.addView(v);

        state = newState;
        Log.v(TAG, "Now in state " + state);

        switch (state) {
        case STATE_LOGGING_IN:
            showDialog(DIALOG_LOGIN);
            break;
        case STATE_NOT_LOGGED_IN:
            loginButton.setEnabled(true);
            break;
        }
    }

    /** Map the states to their corresponding views. */
    void populateLoginStates() {
        loginStates.clear();
        loginStates.put(STATE_NOT_LOGGED_IN, findViewById(R.id.not_logged_in_view));
        loginStates.put(STATE_LOGGING_IN, findViewById(R.id.logging_in_view));
        loginStates.put(STATE_LOGGED_IN, findViewById(R.id.logged_in_view));
    }

    /** Perform the remote login and switch to login state if successful.
     *  Cheers for:
     *  <a href="http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient">Executing a HTTP POST Request with HttpClient</a> */
    void doLogin() {
        final HttpPost httpPost = new HttpPost(LOGIN_URL);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("username", username));
        values.add(new BasicNameValuePair("password", password));
        values.add(new BasicNameValuePair("Login", "Login"));
        values.add(new BasicNameValuePair("came_from", "/"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(values));
        } catch (Exception e) {
            handleLoginFailure(e);
        }

        Thread t = new Thread(new Runnable() {
                public void run() {
                    Handler handler = BookTrader.this.loginHandler;
                    try {
                        HttpResponse response = httpClient.execute(httpPost, httpContext);
                        handler.sendMessage(Message.obtain(handler, LOGIN_RESPONSE, response));
                    } catch (Exception e) {
                        handler.sendMessage(Message.obtain(handler, LOGIN_ERROR, e));
                    }
                }
            });
        t.start();
    }

    void handleLoginResponse(HttpResponse response) {
        Log.v(TAG, "login request done with " + response.getStatusLine());
        //Log.v(TAG, "response string: " + responseToString(response));
        CookieStore cookieJar = (CookieStore)httpContext.getAttribute(ClientContext.COOKIE_STORE);
        boolean loggedIn = false;
        for (Cookie c : cookieJar.getCookies()) {
            Log.v(TAG, "cookie; " + c.getName() + ": " + c.getValue());
            if (c.getName().equals("auth_tkt")) {
                loggedIn = true;
            }
        }

        if (loggedIn) {
            Toast.makeText(this, "Logged in (" + response.getStatusLine() + ")", Toast.LENGTH_SHORT).show();
            switchState(STATE_LOGGED_IN);
        } else {
            switchState(STATE_LOGGING_IN);
        }
    }

    void handleLoginFailure(Exception e) {
        Log.v(TAG, "login failed with " + e);
        Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
        switchState(STATE_LOGGING_IN);
    }

    String responseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.getEntity().writeTo(stream);
        return stream.toString();
    }
}
