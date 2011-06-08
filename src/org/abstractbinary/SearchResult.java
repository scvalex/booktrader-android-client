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
    }

    int totalItems;
    List<Book> books = new ArrayList<Book>();
}
