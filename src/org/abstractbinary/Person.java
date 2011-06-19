package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


class Person {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Internal state */
    String username = "";
    String avatarSource = "";
    String location = "Somewhere Far Away";
    String about = "Proud to be a BookTrader!";
    Drawable avatar;
    String jsonString;
    List<Book> owned = new ArrayList<Book>();
    List<Book> wanted = new ArrayList<Book>();


    /* Public API */

    private Person() {
    }

    public Person(String username, JSONObject json)
        throws JSONException
    {
        this.username = username;
        this.avatarSource = json.getString("gravatar");
        this.location = json.getString("location");
        this.about = json.getString("about");
        this.jsonString = json.toString();
        ObjectCache oc = ObjectCache.getInstance();
        JSONArray jsonBooks = json.getJSONArray("owned");
        for (int i = 0; i < jsonBooks.length(); ++i) {
            JSONObject jsonBook = jsonBooks.getJSONObject(i);
            Book book = new Book(jsonBook);
            oc.insertBook(book);
            owned.add(book);
        }
        jsonBooks = json.getJSONArray("want");
        for (int i = 0; i < jsonBooks.length(); ++i) {
            JSONObject jsonBook = jsonBooks.getJSONObject(i);
            Book book = new Book(jsonBook);
            oc.insertBook(book);
            wanted.add(book);
        }
    }

    public void getAvatar(Handler handler) {
        DownloadCache.getInstance().getDrawable(avatarSource, handler);
    }
}
