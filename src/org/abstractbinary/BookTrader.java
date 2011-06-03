package org.abstractbinary;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.HttpResponse;

public class BookTrader extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Login-state stuff */
    static final int STATE_NOT_LOGGED_IN = 0;
    static final int STATE_LOGGING_IN = 1;
    static final int STATE_LOGGED_IN = 2;
    int state;
    Map<Integer, View> loginStates = new HashMap<Integer, View>();

    /* Remote API */
    BookTraderAPI api;
    Handler requestHandler;

    /* Dialogs */
    static final int DIALOG_LOGIN = 0;
    static final int DIALOG_PERPETUUM = 1;
    ProgressDialog perpetuumDialog;

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

        requestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case BookTraderAPI.LOGIN_RESPONSE:
                    BookTrader.this.handleLoginResponse((HttpResponse)msg.obj);
                    break;
                case BookTraderAPI.LOGIN_ERROR:
                    BookTrader.this.handleLoginFailure((Exception)msg.obj);
                    break;
                case BookTraderAPI.LOGIN_START:
                    showDialog(DIALOG_PERPETUUM);
                    break;
                }
            }
        };

        api = new BookTraderAPI(requestHandler);

        Log.v(TAG, "BookTrader running...");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOGIN:
            dialog = new Dialog(this);

            dialog.setContentView(R.layout.login_dialog);
            dialog.setTitle(getResources().getText(R.string.log_in));

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
                        api.doLogin(username, password);
                    }
            });

            break;
        case DIALOG_PERPETUUM:
            perpetuumDialog = new ProgressDialog(this);
            dialog = perpetuumDialog;
            perpetuumDialog.setMessage(getResources().getText(R.string.logging_in));

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

    void handleLoginResponse(HttpResponse response) {
        Log.v(TAG, "login request done with " + response.getStatusLine());
        perpetuumDialog.dismiss();
        CookieStore cookieJar = (CookieStore)api.getHttpContext().getAttribute(ClientContext.COOKIE_STORE);
        boolean loggedIn = false;
        for (Cookie c : cookieJar.getCookies()) {
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
        perpetuumDialog.dismiss();
        Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
        switchState(STATE_LOGGING_IN);
    }

    String responseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.getEntity().writeTo(stream);
        return stream.toString();
    }
}
