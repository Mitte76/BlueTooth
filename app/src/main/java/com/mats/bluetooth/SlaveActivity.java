package com.mats.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.Dialog.AddingTaskDialogFragment2;

import java.util.ArrayList;

import static com.mats.bluetooth.DbHelper.Database.KEY_MESSAGE;
import static com.mats.bluetooth.DbHelper.Database.KEY_NAME;


public class SlaveActivity extends AppCompatActivity implements AddingTaskDialogFragment2.ReplyMessageListener {
    private Database dbHelper;
    private TextView toolbarText;
    private static final String TAG = "SlaveActivity";
    private ListView mListView;
    private ArrayAdapter<String> mMessageArrayAdapter;
    private ArrayList<String> mMessageNumberArray;
    private ArrayList<String> mMessageUserArray;

    private BluetoothAdapter mBluetoothAdapter = null;
//    private SharedPreferences sharedpreferences;

    private static final int REQUEST_ENABLE_BT = 3;
    //    public static final String MYPREFERENCES = "BtPrefs";
    public static final int REFRESH = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slave_activity);
        dbHelper = Database.getInstance(getApplicationContext());
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbarText = findViewById(R.id.batteryText);
        setSupportActionBar(toolbar);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

//        sharedpreferences = getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);

        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), SlaveService.class);
            service.setAction("REGISTER_RECEIVER");
            service.putExtra("ResultReceiver", mResultReceiver);
            service.putExtra("ResultReceiver_ID", hashCode());
            getApplicationContext().startService(service);

            Log.d(TAG, "onCreate: Running");
        } else {
            Log.d(TAG, "onCreate: NOT Running");
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                init();
            }
        }

//        if (savedInstanceState == null) {
//
//        }
    }

    private ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            switch (resultCode) {
                case REFRESH:
                    Log.d(TAG, "onReceiveResult: ");
                    redrawScreen();
                    break;
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//            Log.d(TAG, "onReceive: " + level);
            if (toolbarText != null) {
                toolbarText.setText(String.valueOf(level) + "%");
            }
        }
    };

    private void redrawScreen() {

        Cursor cursor = dbHelper.getSMS();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.list_reply_message, cursor,
                new String[]{KEY_NAME, KEY_MESSAGE}, new int[]{R.id.list_number, R.id.list_message}, 0);
        mListView.setAdapter(adapter);
        Log.d(TAG, "redrawScreen: Innan");
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                Log.d(TAG, "redrawScreen: " + cursor.getCount());
                mMessageUserArray.add(cursor.getString(2));
                mMessageNumberArray.add(cursor.getString(1));
                mMessageArrayAdapter.add(cursor.getString(2) + ": " + cursor.getString(3));
//                Log.d(TAG, "sendMessage: " + cursor.getString(1) + " " + cursor.getString(2) + " "
//                        + cursor.getString(3) + " " + cursor.getString(4));
            } while (cursor.moveToNext());
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mMessageNumberArray.get(position).substring(0, 1).equals("+")) {
                        android.support.v4.app.DialogFragment addingTaskDialogFragment2 = new AddingTaskDialogFragment2();
                        Bundle args = new Bundle();
                        args.putString("user", mMessageUserArray.get(position));
                        args.putString("number", mMessageNumberArray.get(position));
                        args.putString("message", mMessageArrayAdapter.getItem(position));
                        addingTaskDialogFragment2.setArguments(args);
                        addingTaskDialogFragment2.show(getSupportFragmentManager(), "AddingTaskDialogFragment");
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.no_reply, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMyServiceRunning()) {
            Intent i = new Intent(this, SlaveService.class);
            i.setAction("UNREGISTER_RECEIVER");
            i.putExtra("ResultReceiver_ID", hashCode());
            startService(i);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.mats.bluetooth.SlaveService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.slave, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.start_service: {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (!isMyServiceRunning()) {
                        Intent service = new Intent(getApplicationContext(), SlaveService.class);
                        service.setAction("FIRST_START");
                        service.putExtra("ResultReceiver", mResultReceiver);
                        service.putExtra("ResultReceiver_ID", hashCode());
                        getApplicationContext().startService(service);
                        Toast.makeText(getApplicationContext(), R.string.service_started, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.service_already_running, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            case R.id.stop_service: {
                if (isMyServiceRunning()) {
                    getApplicationContext().stopService(new Intent(getApplicationContext(), SlaveService.class));
                    Toast.makeText(getApplicationContext(), R.string.service_stopped, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.service_not_running, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        }
        return false;
    }

    private void init() {

        mListView = findViewById(R.id.in);
        mMessageArrayAdapter = new ArrayAdapter<>(this, R.layout.message);
        mMessageNumberArray = new ArrayList<>();
        mMessageUserArray = new ArrayList<>();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_enabled,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
        }
    }

    @Override
    public void onReply(String number, String text) {
        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), SlaveService.class);
            service.setAction("SEND_MESSAGE");
            service.putExtra("MESSAGE_TEXT", text);
            service.putExtra("MESSAGE_NUMBER", number);
            getApplicationContext().startService(service);
            Log.d(TAG, "onCreate: Running");
        }
        Log.d(TAG, "onReply: " + number + ": " + text);
    }


}
