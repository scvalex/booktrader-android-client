package org.abstractbinary.booktrader;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    public static class Book {
        String title;
        String subtitle;
        String publisher;
        List<String> authors;
        String thumbnailSource;
        String smallThumbnailSource;
        Drawable image;

        /** Note: call by name; be careful */
        public Book(String title, String subtitle, String publisher,
                    List<String> authors, String thumbnailSource,
                    String smallThumbnailSource) {
            this.title = title;
            this.subtitle = subtitle;
            this.publisher = publisher;
            this.authors = authors;
            this.thumbnailSource = thumbnailSource;
            this.smallThumbnailSource = smallThumbnailSource;
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
    }

    /** Used when we scroll past the last result in the search view. */
    static final Book FILLER_BOOK = new Book("", "", "",
                                             new ArrayList<String>(),
                                             "", "");

    int totalItems;
    List<Book> books = new ArrayList<Book>();
}
