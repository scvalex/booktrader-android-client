package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BookDetails extends Activity {
    TextView bookTitleLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_details);

        bookTitleLabel = (TextView)findViewById(R.id.book_title_label);

        String bookIdentifier =
            getIntent().getData().getPathSegments().get(0);

        bookTitleLabel.setText(bookIdentifier);
    }
}
