package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import java.util.List;


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
    Button inboxButton;

    /* Barcodes, etc. */
    static final String ZXING_URL = "com.google.zxing.client.android.SCAN";
    static final int BARCODE_SCAN_ACTIVITY = 0;

    /* Internal gubbins */
    String username, password;
    String username_try, password_try;
    String lastSearch;
    BooksAdapter bookAdapter;
    boolean autoLogin;
    boolean imFeelingLucky;


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
        inboxButton = (Button)findViewById(R.id.inbox_button);
        usernameLabel = (TextView)findViewById(R.id.user_label);
        usernameLabel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    BookTraderAPI api = BookTraderAPI.getInstance();
                    if (!api.loggedIn)
                        return;
                    startActivity(new Intent
                      (Intent.ACTION_VIEW,
                       Uri.withAppendedPath(Uri.EMPTY, api.currentUser),
                       BookTrader.this, UserDetails.class));
                }
            });

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

        bookAdapter = new BooksAdapter(this);
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

        if (isIntentAvailable(ZXING_URL)) {
            Log.v(TAG, "ZXing present and accounted for!");
            ((Button)findViewById(R.id.barcode_button)).setEnabled(true);
        }

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
                case ObjectCache.OBJECT_GET_STARTED:
                    // whoosh!
                    break;
                case ObjectCache.MESSAGES_GOT:
                    handleMessagesGot((Messages)msg.obj);
                    break;
                case ObjectCache.OBJECT_GET_FAILED:
                    Log.v(TAG, "blast it: " + (Exception)msg.obj);
                    break;
                default:
                    throw new RuntimeException("trader got unknown msg: " +
                                               msg.what);
                }
            }
        };

        BookTraderOpenHelper dbHelper = new BookTraderOpenHelper(this);
        DownloadCache.getInstance().setDbHelper(dbHelper);
        ObjectCache.getInstance().setDbHelper(dbHelper);

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
            autoLogin = true;
            loginButton.setEnabled(false);
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
                        autoLogin = false;
                        loginButton.setEnabled(false);
                        BookTraderAPI.getInstance().doLogin(username_try, password_try, requestHandler);
                    }
                });
            Button cancel =
                (Button)dialog.findViewById(R.id.login_cancel_button);
            cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        autoLogin = true;
                        switchState(STATE_NOT_LOGGED_IN);
                        dialog.dismiss();
                    }
                });

            break;
        case DIALOG_PERPETUUM:
            perpetuumDialog = new ProgressDialog(this);
            dialog = perpetuumDialog;
            perpetuumDialog.setMessage
                (getResources().getText(R.string.loading));
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (BookTraderAPI.getInstance().loggedIn) {
            menu.findItem(R.id.login_menu).setVisible(false);
            menu.findItem(R.id.goto_messages_menu).setEnabled(true);
            menu.findItem(R.id.logout_menu).setVisible(true);
        } else {
            menu.findItem(R.id.login_menu).setVisible(true);
            menu.findItem(R.id.goto_messages_menu).setEnabled(false);
            menu.findItem(R.id.logout_menu).setVisible(false);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent intent) {
        switch (requestCode) {
        case BARCODE_SCAN_ACTIVITY:
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                Toast.makeText(this, "Got " + contents + " (" + format + ")",
                               Toast.LENGTH_SHORT).show();

                imFeelingLucky = true;
                search(contents);
            } else if (resultCode == RESULT_CANCELED) {
                // whoosh
            }
            break;
        default:
            throw new RuntimeException("got result for unknown acitvity");
        }
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
        imFeelingLucky = false;
        search(searchField.getText().toString());
    }

    public void search(String query) {
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
        case R.id.goto_messages_menu:
            showMessages(null);
            return true;
        case R.id.scan_barcode_menu:
            scanBarcode(null);
            return true;
        case R.id.login_menu:
            logIn(null);
            return true;
        case R.id.logout_menu:
            logOut(null);
            return true;
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

    /** Called when the barcode scan button is pressed. */
    public void scanBarcode(View v) {
        Intent zxingIntent = new Intent(ZXING_URL);
        //zxingIntent.setPackage("com.google.zxing.client.android");
        zxingIntent.putExtra("SCAN_MODE", "PRODUCT_MODE");
        startActivityForResult(zxingIntent, BARCODE_SCAN_ACTIVITY);
    }

    /** Called when the inbox button is pressed */
    public void showMessages(View v) {
        startActivity(new Intent
                      (Intent.ACTION_VIEW, Uri.EMPTY, this, Inbox.class));
    }


    /* Helpers */

    /** Set NEWSTATE as the current state and show the appropriate View. */
    void switchState(int newState) {
        loginStates.get(state).setVisibility(View.INVISIBLE);
        loginStates.get(newState).setVisibility(View.VISIBLE);

        state = newState;
        Log.v(TAG, "Now in state " + state);

        loginButton.setEnabled(false);
        switch (state) {
        case STATE_NOT_LOGGED_IN:
            loginButton.setEnabled(true);
            break;
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
        ObjectCache.getInstance().getAllMessages(requestHandler);
    }

    /** Called when the API signals that an error occured during login. */
    void handleLoginFailure(Exception e) {
        Log.v(TAG, "login failed with " + e);
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
        loginButton.setEnabled(true);
        if (!autoLogin)
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
        if (imFeelingLucky && result.totalItems == 1) {
            startActivity(new Intent
                          (Intent.ACTION_VIEW,
                           Uri.withAppendedPath(Uri.EMPTY,
                                                result.get(0).identifier),
                           this, BookDetails.class));
        }
    }

    void handleSearchFailed(Exception e) {
        Log.v(TAG, "search failed with " + e);
        if (perpetuumDialog != null)
            perpetuumDialog.dismiss();
        Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
    }

    void handleMessagesGot(Messages m) {
        inboxButton.setCompoundDrawablesWithIntrinsicBounds
            (R.drawable.ic_menu_friendslist, 0, 0, 0);
        if (m.unread.size() > 0) {
            inboxButton.setCompoundDrawablesWithIntrinsicBounds
                (R.drawable.ic_menu_notifications, 0, 0, 0);
        }

        Toast.makeText(this, "" + m.unread.size() + " unread messages",
                       Toast.LENGTH_SHORT).show();
    }

    /** Returns true if an intent is available. */
    boolean isIntentAvailable(String url) {
        Intent intent = new Intent(url);
        List<ResolveInfo> list =
            getPackageManager().queryIntentActivities
            (intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
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
