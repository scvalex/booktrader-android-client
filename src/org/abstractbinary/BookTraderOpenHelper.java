package org.abstractbinary.booktrader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


class BookTraderOpenHelper extends SQLiteOpenHelper {
    /* Debugging */
    static final String TAG = "BookTrader";

    /* Static values */
    private static final String DATABASE_NAME = "booktrader";
    private static final int DATABASE_VERSION = 1;
    private static final String CACHE_TABLE_NAME = "cache";
    private static final String KEY_URL = "url";
    private static final String KEY_VALUE = "value";
    private static final String CACHE_TABLE_CREATE =
        "CREATE TABLE " + CACHE_TABLE_NAME + " (" +
        KEY_URL + " TEXT, " +
        KEY_VALUE + " BLOB);";

    /* SQLiteOpenHelper API */

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


    /* Public API */

    /** Insert into cache. */
    public void cacheInsert(String key, byte[] value) {
        ContentValues values = new ContentValues();
        values.put(KEY_URL, key);
        values.put(KEY_VALUE, value);
        getWritableDatabase().insertOrThrow(CACHE_TABLE_NAME, null, values);
    }

    /** Get a value from cache or null */
    public byte[] cacheQuery(String key) {
        Cursor c = getReadableDatabase().query(CACHE_TABLE_NAME, new String[]{KEY_VALUE}, KEY_URL + "='" + key + "'", null, null, null, null, null);
        try {
            if (!c.moveToFirst())
                return null;
            return c.getBlob(0);
        } finally {
            c.close();
        }
    }
}
