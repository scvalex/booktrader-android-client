package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;


public class UserDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Misc constants */
    static final int DIALOG_ABOUT_USER = 0;

    /* Background downloading */
    Handler requestHandler;

    /* Internal gubbins */
    Person user;
    String username;
    TextView usernameLabel;
    ImageView avatarView;
    Button aboutUserButton;

    GridView ownedTable;
    BooksAdapter ownedAdapter;
    GridView wantedTable;
    BooksAdapter wantedAdapter;


    /* Activity lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);

        username = getIntent().getData().getPathSegments().get(0);

        usernameLabel = (TextView)findViewById(R.id.user_username_label);
        usernameLabel.setText(username);

        avatarView = (ImageView)findViewById(R.id.user_avatar_view);

        aboutUserButton = (Button)findViewById(R.id.about_user_button);

        ownedAdapter = new BooksAdapter(this);
        ownedTable = (GridView)findViewById(R.id.owned_table);
        ownedTable.setAdapter(ownedAdapter);
        ownedTable.setOnItemClickListener
            (new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        bookSelected(ownedTable, position);
                    }
                });

        wantedAdapter = new BooksAdapter(this);
        wantedTable = (GridView)findViewById(R.id.wanted_table);
        wantedTable.setAdapter(wantedAdapter);
        wantedTable.setOnItemClickListener
            (new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        bookSelected(wantedTable, position);
                    }
                });

        TabHost host = (TabHost)findViewById(android.R.id.tabhost);
        host.setup();

        TabHost.TabSpec spec =
            host.newTabSpec("owned")
            .setIndicator(getResources().getString(R.string.owns),
                          getResources().getDrawable(R.drawable.owns))
            .setContent(R.id.owned_table);
        host.addTab(spec);

        spec =
            host.newTabSpec("wanted")
            .setIndicator(getResources().getString(R.string.wants),
                          getResources().getDrawable(R.drawable.wants))
            .setContent(R.id.wanted_table);
        host.addTab(spec);

        requestHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    BookTraderAPI.PersonResult r;
                    DownloadCache.DownloadResult rd;
                    switch (msg.what) {
                    case ObjectCache.OBJECT_GET_STARTED:
                        // whoosh
                        break;
                    case ObjectCache.PERSON_GOT:
                        r = (BookTraderAPI.PersonResult)msg.obj;
                        handlePersonGot(r.username, (Person)r.result);
                        break;
                    case ObjectCache.OBJECT_GET_FAILED:
                        handlePersonGetFailed((Exception)msg.obj);
                        break;
                    case DownloadCache.DOWNLOAD_DONE:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadDone(rd.url, (Drawable)rd.result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        rd = (DownloadCache.DownloadResult)msg.obj;
                        handleDownloadError(rd.url, (Exception)rd.result);
                        break;
                    default:
                        throw new RuntimeException("unknown message type" +
                                                   msg.what);
                    }
                }
            };
    }

    @Override
    public void onStart() {
        super.onStart();

        ObjectCache.getInstance().getPersonDetails(username, requestHandler);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_ABOUT_USER:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(user.username + " (" + user.location + ")")
                .setMessage(user.about)
                .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            break;
        default:
            throw new RuntimeException("Unknown dialog type: " + id);
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (hasAbout())
            menu.findItem(R.id.show_user_menu).setEnabled(true);
        if (myself())
            menu.findItem(R.id.send_message_menu).setEnabled(false);
        return true;
    }


    /* Callbacks */

    /** Called when clicking on a user's ``more'' link. */
    public void moreUser(View v) {
        showDialog(DIALOG_ABOUT_USER);
    }

    /** Called when clicking a user's ``send message'' button. */
    public void sendMessage(View v) {
        //whoosh
    }

    /** Called when clicking on a book in one of the tables. */
    void bookSelected(View table, int position) {
        Book book;
        if (table == ownedTable)
            book = (Book)ownedAdapter.getItem(position);
        else if (table == wantedTable)
            book = (Book)wantedAdapter.getItem(position);
        else
            throw new RuntimeException("wtf?  Unknown widget.");
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
        case R.id.goto_home_menu:
            finish();
            return true;
        case R.id.show_user_menu:
            moreUser(null);
            return true;
        case R.id.send_message_menu:
            sendMessage(null);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }


    /* Handlers */

    void handlePersonGot(String username, Person person) {
        Person oldUser = user;
        user = person;
        user.getAvatar(requestHandler);

        usernameLabel.setText(user.username);
        if (hasAbout())
            aboutUserButton.setEnabled(true);
        if (myself())
            findViewById(R.id.send_message_button).setEnabled(false);

        boolean same = false;

        // Owned list
        if (oldUser != null && oldUser.owned.size() == user.owned.size()) {
            same = true;
            for (int i = 0; i < user.owned.size(); ++i)
                if (!(oldUser.owned.get(i).identifier.equals
                      (user.owned.get(i).identifier))) {
                    same = false;
                    break;
                }
        }
        if (!same) {
            Log.v(TAG, "Person got; displaying " + user.owned.size() +
                  " books...");
            ownedAdapter.displaySearchResult
                (new FixedSearchResult(user.owned));
        }

        // Wanted list
        if (oldUser != null && oldUser.wanted.size() == user.wanted.size()) {
            same = true;
            for (int i = 0; i < user.wanted.size(); ++i)
                if (!(oldUser.wanted.get(i).identifier.equals
                      (user.wanted.get(i).identifier))) {
                    same = false;
                    break;
                }
        }
        if (!same) {
            Log.v(TAG, "Person got; displaying " + user.wanted.size() +
                  " books...");
            wantedAdapter.displaySearchResult
                (new FixedSearchResult(user.wanted));
        }
    }

    void handlePersonGetFailed(Exception exception) {
        Log.v(TAG, "failed to get: " + exception);
        Toast.makeText(this, "trouble bubble", Toast.LENGTH_SHORT).show();
        // whoosh
    }

    void handleDownloadDone(String url, Drawable image) {
        user.avatar = image;
        avatarView.setImageDrawable(user.avatar);
    }

    void handleDownloadError(String url, Exception e) {
        Log.v(TAG, "failed to download " + url + " because " + e);
        //whoosh
    }


    /* Utilities */

    /** Returns true if there is enough about info to display */
    boolean hasAbout() {
        return (user != null && user.about.length() > 0 &&
                user.location.length() > 0);
    }

    boolean myself() {
        BookTraderAPI api = BookTraderAPI.getInstance();
        return (user != null && api.loggedIn &&
                user.username.equals(api.currentUser));
    }
}
