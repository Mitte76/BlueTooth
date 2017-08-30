package com.mats.bluetooth.DbHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class Database extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "myMessages";

    //SMS
    private static final String SMS_TABLE = "sms_table";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_NAME = "name";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_READ = "read";
    private static final String KEY_TIME = "time";

    //Common
    public static final String KEY_ID = "_id";

    private static final String CREATE_SMS_TABLE = "CREATE TABLE " + SMS_TABLE + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NUMBER + " TEXT,"
            + KEY_NAME + " TEXT," + KEY_MESSAGE + " TEXT," + KEY_TIME + " TEXT unique,"
            + KEY_READ + " TEXT" + ")";
//            + KEY_NAME + " TEXT unique," + KEY_MESSAGE + " TEXT," + KEY_READ + " TEXT" + ")";


    private Database(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static Database mInstance = null;
    private final String TAG = Database.class.getName();
    private static final int debug = 0;

    public static Database getInstance(Context ctx) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new Database(ctx.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SMS_TABLE);
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CREATE_SMS_TABLE);
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    public void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }


    public Cursor getSMS() {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  * FROM " + SMS_TABLE;
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        return cursor;
    }


    public void addSMS(String name, String number, String message, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_NUMBER, number);
        values.put(KEY_MESSAGE, message);
        values.put(KEY_TIME, time);
        values.put(KEY_READ, 0);
        db.insertWithOnConflict(SMS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);

//        return db.insert(SMS_TABLE, null, values);
    }


}


