package org.abstractbinary.booktrader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class BookDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Constants */
    static final int DIALOG_LOADING = 0;

    /* Commonly used widgets */
    TextView bookTitleLabel;
    ProgressDialog loadingDialog;

    /* Internal gubbins */
    String bookIdentifier;
    Book book;
    Handler detailsHandler;
    BookTraderAPI api;
    PeopleList ownerList;
    PeopleList coveterList;

    /* Activity life-cycle */

    /** Called when the activity is first created.  Mhm.  I am not
     * suprised at all by this, hence the informational content of
     * this comment up to this sentence is null.*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_details);

        bookTitleLabel = (TextView)findViewById(R.id.book_title_label);

        bookIdentifier = getIntent().getData().getPathSegments().get(0);

        detailsHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (loadingDialog != null)
                        loadingDialog.dismiss();
                    switch (msg.what) {
                    case BookTraderAPI.DETAILS_START:
                    case ObjectCache.OBJECT_GET_STARTED:
                        showDialog(DIALOG_LOADING);
                        break;
                    case BookTraderAPI.DETAILS_GOT:
                    case ObjectCache.BOOK_GOT:
                        handleDetailsGot((Book)msg.obj);
                        break;
                    case BookTraderAPI.DETAILS_ERROR:
                    case ObjectCache.OBJECT_GET_FAILED:
                        Toast.makeText(BookDetails.this, "double trouble",
                                       Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Failed to get object details: " +
                              (Exception)msg.obj);
                        break;
                    case DownloadCache.DOWNLOAD_DONE:
                        handleCoverDownloadDone
                            ((Drawable)
                             ((DownloadCache.DownloadResult)msg.obj).result);
                        break;
                    case DownloadCache.DOWNLOAD_ERROR:
                        Log.v(TAG, "Failed to get cover: " +
                              (Exception)
                              ((DownloadCache.DownloadResult)msg.obj).result);
                        break;
                    case BookTraderAPI.DETAILS_HAVE:
                        ObjectCache.getInstance().getBookDetails
                            ((String)msg.obj, detailsHandler);
                        break;
                    case BookTraderAPI.DETAILS_WANT:
                        ObjectCache.getInstance().getBookDetails
                            ((String)msg.obj, detailsHandler);
                        break;
                    case BookTraderAPI.DETAILS_REMOVE:
                        ObjectCache.getInstance().getBookDetails
                            ((String)msg.obj, detailsHandler);
                        break;
                    default:
                        throw new RuntimeException("unknown message type: " +
                                                   msg.what);
                    }
                }
            };

        api = BookTraderAPI.getInstance();

        ownerList = new PeopleList((LinearLayout)findViewById(R.id.book_owners_list), this);

        coveterList  = new PeopleList((LinearLayout)findViewById(R.id.book_coveters_list), this);

        ObjectCache.getInstance().getBookDetails(bookIdentifier, detailsHandler);

        bookTitleLabel.setText(bookIdentifier);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
        case DIALOG_LOADING:
            loadingDialog = new ProgressDialog(this);
            dialog = loadingDialog;
            loadingDialog.setMessage(getResources().getText(R.string.loading));
            break;
        default:
            throw new RuntimeException("unknown dialog type + " + id);
        }
        return dialog;
    }


    /* Event handlers */

    /** Called when the book details have been downloaded */
    void handleDetailsGot(Book book) {
        if (loadingDialog != null)
            loadingDialog.dismiss();

        this.book = book;
        book.getCover(detailsHandler);
        findViewById(R.id.owners_container).setVisibility(View.VISIBLE);
        if (book.owners.size() == 0)
            findViewById(R.id.owners_container).setVisibility(View.GONE);
        ownerList.setData(book.owners);
        findViewById(R.id.coveters_container).setVisibility(View.VISIBLE);
        if (book.coveters.size() == 0)
            findViewById(R.id.coveters_container).setVisibility(View.GONE);
        coveterList.setData(book.coveters);

        bookTitleLabel.setText(book.title);
        ((TextView)findViewById(R.id.book_subtitle_label)).setText
            (book.subtitle);
        StringBuilder authors = new StringBuilder();
        for (int i = 0; i < book.authors.size(); i++) {
            authors.append(book.authors.get(i));
            if (i < book.authors.size() - 2)
                authors.append(", ");
            if (i == book.authors.size() - 2)
                authors.append(" and ");
        }
        ((TextView)findViewById(R.id.book_authors_label)).setText
            (authors.toString());
        Button haveButton = (Button)findViewById(R.id.have_button);
        haveButton.setEnabled(true);
        haveButton.setText(getResources().getString(R.string.have));
        Button wantButton = (Button)findViewById(R.id.want_button);
        wantButton.setEnabled(true);
        wantButton.setText(getResources().getString(R.string.want));
        wantButton.setVisibility(View.VISIBLE);
        Button clearButton = (Button)findViewById(R.id.clear_button);
        clearButton.setVisibility(View.GONE);
        if (api.loggedIn) {
            if (book.owners.contains(api.currentUser))
                markHad();
            if (book.coveters.contains(api.currentUser))
                markWanted();
        } else {
            ((Button)findViewById(R.id.want_button)).setEnabled(false);
            ((Button)findViewById(R.id.have_button)).setEnabled(false);
        }
        ((TextView)findViewById(R.id.book_description_text)).setText
            (book.description);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_details, menu);
        return true;
    }


    /* Callbacks */

    /** Called when the cover image has finished downloading */
    void handleCoverDownloadDone(Drawable image) {
        if (image != null)
            ((ImageView)findViewById
             (R.id.cover_view)).setImageDrawable(image);
    }

    /** Called when the user clicks a have button */
    public void have(View v) {
        api.doHave(bookIdentifier, detailsHandler);
    }

    /** Called when the user clicks a have button */
    public void want(View v) {
        api.doWant(bookIdentifier, detailsHandler);
    }

    /** Called when the user clicks the clear button */
    public void clear(View v) {
        api.doRemove(bookIdentifier, detailsHandler);
    }

    /** Called when a menu item is selected. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.goto_home_menu:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }


    /* Utilities */

    void markWanted() {
        Button wantButton = (Button)findViewById(R.id.want_button);
        wantButton.setText(getResources().getString(R.string.already_want));
        wantButton.setEnabled(false);
        findViewById(R.id.clear_button).setVisibility(View.VISIBLE);
    }

    void markHad() {
        Button haveButton = (Button)findViewById(R.id.have_button);
        haveButton.setText(getResources().getString(R.string.already_have));
        haveButton.setEnabled(false);
        findViewById(R.id.want_button).setVisibility(View.GONE);
        findViewById(R.id.clear_button).setVisibility(View.VISIBLE);
    }
}
