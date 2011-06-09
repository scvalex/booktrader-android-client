package org.abstractbinary.booktrader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class BookTraderOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "booktrader";
    private static final int DATABASE_VERSION = 1;
    private static final String CACHE_TABLE_NAME = "cache";
    private static final String KEY_URL = "url";
    private static final String KEY_VALUE = "value";
    private static final String CACHE_TABLE_CREATE =
        "CREATE TABLE " + CACHE_TABLE_NAME + " (" +
        KEY_URL + " TEXT, " +
        KEY_VALUE + " BLOB);";

    public BookTraderOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CACHE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE " + CACHE_TABLE_NAME + ";");
        onCreate(db);
    }
}
