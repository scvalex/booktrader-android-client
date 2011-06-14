package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


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
    Handler requestHandler;

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
    GridView bookTable;

    /* Internal gubbins */
    String username, password;
    String username_try, password_try;
    String lastSearch;
    BookAdapter bookAdapter;


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

        bookAdapter = new BookAdapter(this);
        bookTable = (GridView)findViewById(R.id.book_table);
        bookTable.setAdapter(bookAdapter);
        bookTable.setOnItemClickListener
            (new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        bookSelected(position);
                    }
                });

        populateLoginStates();
        state = STATE_NOT_LOGGED_IN;

        requestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case BookTraderAPI.LOGIN_DONE:
                    handleLoginDone();
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
                    handleSearchResult((SearchResult)msg.obj);
                    break;
                case BookTraderAPI.SEARCH_FAILED:
                    handleSearchFailed((Exception)msg.obj);
                    break;
                }
            }
        };

        BookTraderOpenHelper dbHelper = new BookTraderOpenHelper(this);
        DownloadCache.getInstance().setDbHelper(dbHelper);
        BookCache.getInstance().setDbHelper(dbHelper);

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        username = settings.getString("username", null);
        password = settings.getString("password", null);
        lastSearch = settings.getString("lastSearch", "Godel");
        if (lastSearch != null)
            BookTraderAPI.getInstance().doSearch(lastSearch, requestHandler);

        switchState(STATE_NOT_LOGGED_IN);

        Log.v(TAG, "BookTrader running...");
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!BookTraderAPI.getInstance().loggedIn &&
            username != null && password != null)
        {
            BookTraderAPI.reset();
            switchState(STATE_NOT_LOGGED_IN);
            username_try = username;
            password_try = password;
            BookTraderAPI.getInstance().doLogin(username_try, password_try,
                                                requestHandler);
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
                        BookTraderAPI.getInstance().doLogin(username_try, password_try, requestHandler);
                    }
            });

            break;
        case DIALOG_PERPETUUM:
            perpetuumDialog = new ProgressDialog(this);
            dialog = perpetuumDialog;
            perpetuumDialog.setMessage(getResources().getText(R.string.loading));

            break;
        default:
            throw new RuntimeException("Unknown dialog type: " + id);
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
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
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        if (query.length() > 0) {
            lastSearch = query;
            BookTraderAPI.getInstance().doSearch(query, requestHandler);
            savePreferences();
        }
    }

    /** Called when a book somewhere is selected. */
    public void bookSelected(int position) {
        Book book = (Book)bookAdapter.getItem(position);
        if (book == Book.FILLER_BOOK)
            return;
        startActivity(new Intent
                      (Intent.ACTION_VIEW,
                       Uri.withAppendedPath(Uri.EMPTY, book.identifier),
                       this, BookDetails.class));
    }

    /** Called when a menu item is selected. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear_cache_menu:
            DownloadCache.getInstance().clear();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.about_menu:
            Toast.makeText(this, "fööt fööt fööt", Toast.LENGTH_SHORT).show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
            BookTraderAPI.getInstance().doLogout(requestHandler);
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
    void handleLoginDone() {
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();

        Toast.makeText(this, "Let's get literate!", Toast.LENGTH_SHORT).show();
        username = username_try;
        password = password_try;
        usernameLabel.setText(username);
        savePreferences();
        switchState(STATE_LOGGED_IN);
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

    void handleSearchResult(SearchResult result) {
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Log.v(TAG, "Found " + result.totalItems + " books!");
        Toast.makeText(this, "Found " + result.totalItems +
                       " books!", Toast.LENGTH_SHORT).show();
        bookAdapter.displaySearchResult(result);
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
        lastSearch = null;
        BookTraderAPI.reset();
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
        if (lastSearch != null)
            editor.putString("lastSearch", lastSearch);
        editor.commit();
    }
}
