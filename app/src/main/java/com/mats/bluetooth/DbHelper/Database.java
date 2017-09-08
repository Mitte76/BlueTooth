package com.mats.bluetooth.DbHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class Database extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "myMessages";

    //SMS
    private static final String SMS_TABLE = "sms_table";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_NAME = "name";
    public static final String KEY_MESSAGE = "message";
    private static final String KEY_READ = "read";
    public static final String KEY_REMOTE_ID = "remote_id";
    public static final String KEY_TIME = "time";
    public static final String KEY_DELETED_LOCAL = "deleted_local";
    public static final String KEY_DELETED_EXTERNAL = "deleted_external";

    //Common
    public static final String KEY_ID = "_id";

    private static final String CREATE_SMS_TABLE = "CREATE TABLE " + SMS_TABLE + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NUMBER + " TEXT,"
            + KEY_NAME + " TEXT," + KEY_MESSAGE + " TEXT," + KEY_TIME + " TEXT unique,"
            + KEY_REMOTE_ID + " TEXT unique," + KEY_READ + " TEXT, "
            + KEY_DELETED_LOCAL + " TEXT, " + KEY_DELETED_EXTERNAL + " TEXT " + ")";
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
        String selectQuery = "SELECT  * FROM " + SMS_TABLE + " WHERE " + KEY_DELETED_LOCAL + " != 1 ORDER BY " + KEY_TIME + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        return cursor;
    }
    public Cursor getSMSLog() {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  * FROM " + SMS_TABLE + " ORDER BY " + KEY_TIME + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        return cursor;
    }

    public void prepareSms() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("UPDATE %s SET %s = 1;",
                SMS_TABLE, KEY_DELETED_EXTERNAL));
    }

    public void addSMS(String name, String number, String message, String time) {
        Long test = null;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_NUMBER, number);
        values.put(KEY_MESSAGE, message);
        values.put(KEY_TIME, time);
        values.put(KEY_READ, 0);
        values.put(KEY_DELETED_LOCAL, 0);
        values.put(KEY_DELETED_EXTERNAL, 0);
        try {
            test = db.insertWithOnConflict(SMS_TABLE, null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
        } catch(SQLException e) {

            if (e instanceof SQLiteConstraintException){
//                Log.d(TAG, "addSMS: japp" + test);

                try {
                    db.execSQL(String.format("UPDATE %s SET %s = 0 WHERE %s = %s;",
                            SMS_TABLE, KEY_DELETED_EXTERNAL, KEY_TIME, time));

                } catch(SQLException f) {
//                    Log.d(TAG, "addSMS 2: " + f);

                }


            }

        }
    }

    public void clearSMS() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(SMS_TABLE,null,null);

//        return db.insert(SMS_TABLE, null, values);
    }

    public Cursor getOneSMS(String message, String number){
        SQLiteDatabase db = this.getWritableDatabase();

//        String selectQuery = "SELECT  * FROM " + SMS_TABLE + " WHERE " + KEY_MESSAGE
//                + " = " + message + " AND ";

//        DatabaseUtils.stringForQuery()

        String select = String.format("SELECT * FROM %s WHERE %s = '%s';",
                SMS_TABLE, KEY_NUMBER, number);

/*
        AND %s = '%s', KEY_NUMBER, number
*/
        Cursor c = db.rawQuery(select,null);

        return c;
    }

    public void deleteSms(String id) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(SMS_TABLE, KEY_ID + " = " + id, null);

//        return db.insert(SMS_TABLE, null, values);
    }

    public void deleteSms() {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d(TAG, "deleteSms: Deleting");
        db.delete(SMS_TABLE, KEY_DELETED_EXTERNAL + " = 1" , null);

//        return db.insert(SMS_TABLE, null, values);
    }

    public void markSmsDeleted(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(KEY_DELETED_LOCAL, 0);
//        db.update(SMS_TABLE, values, KEY_ID + " = " + id, null);


        db.execSQL(String.format("UPDATE %s SET %s = 1 WHERE %s = %s;",
                SMS_TABLE, KEY_DELETED_LOCAL, KEY_ID, id));


    }

    public void markSmsUnDeleted() {
        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(KEY_DELETED_LOCAL, 0);
//        db.update(SMS_TABLE, values, KEY_ID + " = " + id, null);


        db.execSQL(String.format("UPDATE %s SET %s = 0;",
                SMS_TABLE, KEY_DELETED_LOCAL));


    }

/*
    public long createList(String list) {
        ContentValues values = new ContentValues();
        values.put(KEY_LIST_NAME, list);
        values.put(DATE_CREATED, currentTime);

        long tag_id = db.insert(LIST_TABLE, null, values);
        values.clear();


        values.put(KEY_LIST_SORTID, getMaxId());
        values.put(DELETED, 0);
        values.put(DATE_SYNCED, 0);
        return currentTime;
    }*/

}


