package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Book {
    /* Debugging */
    static final String TAG = "BookTrader";

    /** Used when we scroll past the last result in the search view. */
    static final Book FILLER_BOOK = new Book();

    /* Record fields */
    String identifier = "";
    String title = "";
    String subtitle = "";
    String publisher = "";
    List<String> authors = new ArrayList<String>();
    String thumbnailSource = "";
    String smallThumbnailSource = "";
    String description = "";
    Drawable image;
    List<String> owners = new ArrayList<String>();
    List<String> coveters = new ArrayList<String>();
    String jsonString = "";

    private Book() {
    }

    /** JsonBook should hold the book entries at top-level. */
    public Book(JSONObject jsonBook) throws JSONException {
        JSONArray jsonAuthors = jsonBook.getJSONArray("authors");
        authors = new ArrayList<String>();
        for (int j = 0; j < jsonAuthors.length(); ++j)
            authors.add(jsonAuthors.getString(j));
        this.identifier = jsonBook.getString("identifier");
        this.title = jsonBook.getString("title");
        this.subtitle = jsonBook.getString("subtitle");
        this.publisher = jsonBook.getString("publisher");
        this.description = jsonBook.getString("description");
        this.thumbnailSource = jsonBook.getString("thumbnail");
        this.smallThumbnailSource = jsonBook.getString("smallThumbnail");
        this.jsonString = jsonBook.toString();

        JSONArray jsonOwners = jsonBook.getJSONArray("owners");
        for (int j = 0; j < jsonOwners.length(); ++j)
            owners.add(jsonOwners.getString(j));

        JSONArray jsonCoveters = jsonBook.getJSONArray("coveters");
        for (int j = 0; j < jsonCoveters.length(); ++j)
            coveters.add(jsonCoveters.getString(j));
    }

    /** Get the best known cover image.  Return null if does not exist. */
    public String getBestCoverSource() {
        String url = thumbnailSource;
        if (url == null || url.length() == 0)
            url = smallThumbnailSource;
        if (url != null && url.length() > 0)
            return url;
        return null;
    }

    /** Request the background in the background */
    public void getCover(Handler downloadHandler) {
        DownloadCache.getInstance().getDrawable(getBestCoverSource(),
                                                downloadHandler);
    }
}
