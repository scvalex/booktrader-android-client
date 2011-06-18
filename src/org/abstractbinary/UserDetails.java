package org.abstractbinary.booktrader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


class UserDetails extends Activity {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal gubbins */
    String username;
    TextView usernameLabel;
    ImageView avatarView;

    /* Activity lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);

        username = getIntent().getData().getPathSegments().get(0);

        usernameLabel = (TextView)findViewById(R.id.user_username_label);
        avatarView = (ImageView)findViewById(R.id.user_avatar_view);
    }
}
