package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class BookDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Commonly used widgets */
    TextView bookTitleLabel;

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
                        //whoosh
                        break;
                    case BookTraderAPI.DETAILS_GOT:
                        handleDetailsGot((SearchResult.Book)msg.obj);
                        break;
                    case BookTraderAPI.DETAILS_ERROR:
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


    /* Event handlers */

    void handleDetailsGot(SearchResult.Book book) {
        this.book = book;
        Toast.makeText(BookDetails.this, "details got", Toast.LENGTH_SHORT).show();
        bookTitleLabel.setText(book.title);
    }
}
