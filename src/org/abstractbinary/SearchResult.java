package org.abstractbinary.booktrader;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    public static class Book {
        String title;
        String subtitle;
        String publisher;
        List<String> authors;

        /** Note: call by name; be careful */
        public Book(String title, String subtitle, String publisher,
                    List<String> authors) {
            this.title = title;
            this.subtitle = subtitle;
            this.publisher = publisher;
            this.authors = authors;
        }
    }

    int totalItems;
    List<Book> books = new ArrayList<Book>();
}
