package com.mats.bluetooth.DbHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;


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
    public static final String KEY_READ = "read";
    public static final String KEY_THREAD = "thread";
    public static final String KEY_REMOTE_ID = "remote_id";
    public static final String KEY_TIME = "time";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_DIRECTION = "direction";
    public static final String KEY_DELETED_LOCAL = "deleted_local";
    public static final String KEY_DELETED_EXTERNAL = "deleted_external";

    //Common
    public static final String KEY_ID = "_id";

    private static final String CREATE_SMS_TABLE = "CREATE TABLE " + SMS_TABLE + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NUMBER + " TEXT,"
            + KEY_NAME + " TEXT," + KEY_MESSAGE + " TEXT," + KEY_TIME + " TEXT unique,"
            + KEY_REMOTE_ID + " TEXT unique," + KEY_READ + " TEXT, "
            + KEY_THREAD + " TEXT, " + KEY_DELETED_LOCAL + " TEXT, "
            + KEY_IMAGE + " TEXT, " + KEY_DIRECTION + " TEXT, "
            + KEY_DELETED_EXTERNAL + " TEXT " + ")";


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
        String selectQuery = "SELECT  * FROM " + SMS_TABLE + " WHERE " + KEY_DELETED_LOCAL + " != 1 AND " + KEY_READ + " == 0 ORDER BY " + KEY_TIME + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
//        cursor.moveToFirst();
        Log.d(TAG, "getSMS: " + cursor.getCount());
        return cursor;
    }

    public Cursor getSMS2(String id) {
        SQLiteDatabase db = this.getWritableDatabase();

        String select = String.format("SELECT * FROM %s WHERE %s = '%s' ORDER BY %s ASC;",
                SMS_TABLE, KEY_THREAD, id, KEY_TIME);
        Cursor c = db.rawQuery(select, null);

        return c;
    }

    public Cursor getFirstThreadMsg() {
        SQLiteDatabase db = this.getWritableDatabase();
        String select1 = String.format("SELECT %s, %s, max(%s) from %s WHERE %s = 0 AND %s == 1 GROUP BY %s;",
                KEY_THREAD, KEY_DIRECTION, KEY_TIME, SMS_TABLE, KEY_READ, KEY_DIRECTION, KEY_THREAD);
        Cursor cursor = db.rawQuery(select1, null);
        if (cursor.moveToFirst()) {
            String[] thread = new String[cursor.getCount() - 1];
            String[] time = new String[cursor.getCount() - 1];
            String[] direction = new String[cursor.getCount() - 1];
            int i = 0;
            while (cursor.moveToNext()) {
                thread[i] = cursor.getString(0);
                direction[i] = cursor.getString(1);
                time[i] = cursor.getString(2);
                i++;
            }
            cursor.close();
            String allTime = TextUtils.join(", ", time);
            String allThread = TextUtils.join(", ", thread);
            String allDirection = TextUtils.join(", ", direction);
            Log.d(TAG, "getFirstThreadMsg: " + allTime);
            String select = String.format("SELECT * FROM %s WHERE %s IN (%s) AND %s IN (%s) AND %s == 1 ORDER BY %s DESC;",
                    SMS_TABLE, KEY_TIME, allTime, KEY_THREAD, allThread, KEY_DIRECTION, KEY_TIME);
            return db.rawQuery(select, null);

        } else return null;
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

    public void addSMS(String name, String number, String message, String time, String remoteId, String read, String thread, String direction) {
        Long test = null;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_NUMBER, number);
        values.put(KEY_MESSAGE, message);
        values.put(KEY_TIME, time);
        values.put(KEY_READ, read);
        values.put(KEY_THREAD, thread);
        values.put(KEY_DELETED_LOCAL, 0);
        values.put(KEY_DELETED_EXTERNAL, 0);
        values.put(KEY_REMOTE_ID, remoteId);
        values.put(KEY_DIRECTION, direction);
        try {
            test = db.insertWithOnConflict(SMS_TABLE, null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
        } catch (SQLException e) {

            if (e instanceof SQLiteConstraintException) {
                Log.d(TAG, "addSMS: japp" + test);

                try {
                    db.execSQL(String.format("UPDATE %s SET %s = 0 WHERE %s = %s;",
                            SMS_TABLE, KEY_DELETED_EXTERNAL, KEY_TIME, time));

                } catch (SQLException f) {
                }
            }
        }
    }

    public void addSMS(String name, String number, String message, String time, String remoteId, String read, String thread, String direction, String image) {
        Long test = null;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_NUMBER, number);
        values.put(KEY_MESSAGE, message);
        values.put(KEY_TIME, time);
        values.put(KEY_READ, read);
        values.put(KEY_THREAD, thread);
        values.put(KEY_DELETED_LOCAL, 0);
        values.put(KEY_DELETED_EXTERNAL, 0);
        values.put(KEY_REMOTE_ID, remoteId);
        values.put(KEY_DIRECTION, direction);
        values.put(KEY_IMAGE, image);
        try {
            test = db.insertWithOnConflict(SMS_TABLE, null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
        } catch (SQLException e) {

            if (e instanceof SQLiteConstraintException) {
//                Log.d(TAG, "addSMS: japp" + test);

                try {
                    db.execSQL(String.format("UPDATE %s SET %s = 0 WHERE %s = %s;",
                            SMS_TABLE, KEY_DELETED_EXTERNAL, KEY_TIME, time));

                } catch (SQLException f) {
//                    Log.d(TAG, "addSMS 2: " + f);

                }


            }

        }
    }


    public Cursor getOneSMS(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String select = String.format("SELECT * FROM %s WHERE %s = '%s';",
                SMS_TABLE, KEY_REMOTE_ID, id);
        Cursor c = db.rawQuery(select, null);

        return c;
    }

    public void deleteSms() {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d(TAG, "deleteSms: Deleting");
        db.delete(SMS_TABLE, KEY_DELETED_EXTERNAL + " = 1", null);

    }

    public void markSmsDeleted(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("UPDATE %s SET %s = 1 WHERE %s = '%s';",
                SMS_TABLE, KEY_DELETED_LOCAL, KEY_REMOTE_ID, id));


    }

    public void markSmsUnDeleted() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(String.format("UPDATE %s SET %s = 0;",
                SMS_TABLE, KEY_DELETED_LOCAL));


    }

}


