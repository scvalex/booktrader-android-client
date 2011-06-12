package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.app.ProgressDialog;


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
    SearchResult.Book book;
    Handler detailsHandler;

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
                    switch (msg.what) {
                    case BookTraderAPI.DETAILS_START:
                        showDialog(DIALOG_LOADING);
                        break;
                    case BookTraderAPI.DETAILS_GOT:
                        handleDetailsGot((SearchResult.Book)msg.obj);
                        break;
                    case BookTraderAPI.DETAILS_ERROR:
                        if (loadingDialog != null)
                            loadingDialog.dismiss();
                        Toast.makeText(BookDetails.this, "error getting book", Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Failed to get book details: " +
                              (Exception)msg.obj);
                        break;
                    default:
                        throw new RuntimeException("unknown message type: " +
                                                   msg.what);
                    }
                }
            };

        BookTraderAPI.getInstance().doGetBookDetails(bookIdentifier,
                                                     detailsHandler);

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

    void handleDetailsGot(SearchResult.Book book) {
        if (loadingDialog != null)
            loadingDialog.dismiss();

        this.book = book;

        bookTitleLabel.setText(book.title);
        ((TextView)findViewById(R.id.book_subtitle_label)).setText
            (book.subtitle);
        StringBuilder authors = new StringBuilder();
        for (int i = 0; i < book.authors.size(); i++) {
            authors.append(book.authors.get(i));
            if (i < book.authors.size() - 2)
                authors.append(", ");
            if (i == book.authors.size() - 2)
                authors.append(", and ");
        }
        ((TextView)findViewById(R.id.book_authors_label)).setText
            (authors.toString());
    }
}
