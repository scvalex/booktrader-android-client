package org.abstractbinary;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
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
    static final int STATE_LOGGING_OUT = 3;
    int state;
    Map<Integer, View> loginStates = new HashMap<Integer, View>();

    /* Remote API */
    BookTraderAPI api;
    Handler requestHandler;
    boolean loggedIn;

    /* Dialogs */
    static final int DIALOG_LOGIN = 0;
    static final int DIALOG_PERPETUUM = 1;
    ProgressDialog perpetuumDialog;

    /* Often used widgets */
    FrameLayout menuBar;
    Button loginButton;
    Button logoutButton;
    TextView usernameLabel;
    EditText searchField;
    Button searchButton;

    /* Internal gubbins */
    String username, password;
    String username_try, password_try;


    /* Application life-cycle */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        menuBar = (FrameLayout)findViewById(R.id.menu_bar);
        loginButton = (Button)findViewById(R.id.login_button);
        logoutButton = (Button)findViewById(R.id.logout_button);
        usernameLabel = (TextView)findViewById(R.id.user_label);

        searchField = (EditText)findViewById(R.id.search_field);
        searchButton = (Button)findViewById(R.id.search_button);
        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId,
                                              KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        searchButton.performClick();
                        return true;
                    }
                    return false;
                }
            });

        populateLoginStates();
        state = STATE_NOT_LOGGED_IN;

        requestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case BookTraderAPI.LOGIN_RESPONSE:
                    handleLoginResponse((HttpResponse)msg.obj);
                    break;
                case BookTraderAPI.LOGIN_ERROR:
                    handleLoginFailure((Exception)msg.obj);
                    break;
                case BookTraderAPI.LOGIN_START:
                    showDialog(DIALOG_PERPETUUM);
                    break;
                case BookTraderAPI.LOGOUT_START:
                    showDialog(DIALOG_PERPETUUM);
                    break;
                case BookTraderAPI.LOGOUT_FINISHED:
                    handleLogoutFinished();
                    break;
                case BookTraderAPI.LOGOUT_ERROR:
                    handleLogoutError((Exception)msg.obj);
                    break;
                case BookTraderAPI.SEARCH_START:
                    showDialog(DIALOG_PERPETUUM);
                    break;
                case BookTraderAPI.SEARCH_FINISHED:
                    handleSearchResult((HttpResponse)msg.obj);
                    break;
                case BookTraderAPI.SEARCH_FAILED:
                    handleSearchFailed((Exception)msg.obj);
                    break;
                }
            }
        };
        api = new BookTraderAPI(requestHandler);

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        username = settings.getString("username", null);
        password = settings.getString("password", null);

        switchState(STATE_NOT_LOGGED_IN);

        Log.v(TAG, "BookTrader running...");
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!loggedIn && username != null && password != null) {
            switchState(STATE_NOT_LOGGED_IN);
            username_try = username;
            password_try = password;
            api.doLogin(username_try, password_try);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePreferences();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOGIN:
            dialog = new Dialog(this);

            dialog.setContentView(R.layout.login_dialog);
            dialog.setTitle(getResources().getText(R.string.log_in));

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        switchState(STATE_NOT_LOGGED_IN);
                    }
            });
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
                        username_try = usernameField.getText().toString();
                        password_try = passwordField.getText().toString();
                        Log.v(TAG, "New login info: " + username_try);
                        dialog.dismiss();
                        api.doLogin(username_try, password_try);
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
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "screen orientation changed");
    }


    /* Event handlers */

    /** Called when the login button is pressed. */
    public void logIn(View v) {
        switchState(STATE_LOGGING_IN);
    }

    /** Called when the login button is pressed. */
    public void logOut(View v) {
        switchState(STATE_LOGGING_OUT);
    }

    /** Called when the search button is pressed. */
    public void search(View v) {
        String query = searchField.getText().toString();
        if (query.length() > 0)
            api.doSearch(query);
    }


    /* Helpers */

    /** Set NEWSTATE as the current state and show the appropriate View. */
    void switchState(int newState) {
        loginStates.get(state).setVisibility(View.INVISIBLE);
        loginStates.get(newState).setVisibility(View.VISIBLE);

        state = newState;
        Log.v(TAG, "Now in state " + state);

        switch (state) {
        case STATE_LOGGING_IN:
            showDialog(DIALOG_LOGIN);
            break;
        case STATE_LOGGING_OUT:
            api.doLogout();
            break;
        }
    }

    /** Map the states to their corresponding views. */
    void populateLoginStates() {
        loginStates.clear();
        loginStates.put(STATE_NOT_LOGGED_IN, findViewById(R.id.not_logged_in_view));
        loginStates.put(STATE_LOGGING_IN, findViewById(R.id.not_logged_in_view));
        loginStates.put(STATE_LOGGED_IN, findViewById(R.id.logged_in_view));
        loginStates.put(STATE_LOGGING_OUT, findViewById(R.id.logged_in_view));
    }

    /** Called when the API signals that login finished without error. */
    void handleLoginResponse(HttpResponse response) {
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        CookieStore cookieJar = (CookieStore)api.getHttpContext().getAttribute(ClientContext.COOKIE_STORE);
        loggedIn = false;
        for (Cookie c : cookieJar.getCookies()) {
            if (c.getName().equals("auth_tkt")) {
                loggedIn = true;
            }
        }

        if (loggedIn) {
            Toast.makeText(this, "Let's get literate!", Toast.LENGTH_SHORT).show();
            username = username_try;
            password = password_try;
            usernameLabel.setText(username);
            savePreferences();
            switchState(STATE_LOGGED_IN);
        } else {
            switchState(STATE_LOGGING_IN);
        }
    }

    /** Called when the API signals that an error occured during login. */
    void handleLoginFailure(Exception e) {
        Log.v(TAG, "login failed with " + e);
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
        switchState(STATE_LOGGING_IN);
    }

    /** Called when the API signals logout finished. */
    void handleLogoutFinished() {
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        clearPrivateData();
        Toast.makeText(this, "Bye bye", Toast.LENGTH_LONG).show();
        switchState(STATE_NOT_LOGGED_IN);
    }

    /** Called when the API signals that an error occured during logout. */
    void handleLogoutError(Exception e) {
        Log.v(TAG, "logout failed with " + e);
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Logout failed :(", Toast.LENGTH_LONG).show();
        clearPrivateData();
    }

    void handleSearchResult(HttpResponse response) {
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Searched!", Toast.LENGTH_SHORT).show();
    }

    void handleSearchFailed(Exception e) {
        Log.v(TAG, "search failed with " + e);
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
    }

    /** Clears internal stores of private data.  Used when logging out. */
    void clearPrivateData() {
        username = null;
        password = null;
        api = new BookTraderAPI(requestHandler);
        getPreferences(Context.MODE_PRIVATE).edit().clear().commit();
    }

    void savePreferences() {
        Log.v(TAG, "Saving preferences...");
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (username != null)
            editor.putString("username", username);
        if (password != null)
            editor.putString("password", password);
        editor.commit();
    }

    /** Return the String body of a HttpResponse. */
    String responseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.getEntity().writeTo(stream);
        return stream.toString();
    }
}
