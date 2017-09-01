package com.mats.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.Dialog.AddingTaskDialogFragment2;

import java.util.ArrayList;


/**
 * Created by mats on 2017-08-28.
 */

public class SlaveActivity extends AppCompatActivity implements AddingTaskDialogFragment2.ReplyMessageListener {
    private Database dbHelper;

    private static final String TAG = "SlaveActivity";
    private Toolbar toolbar;
    private Button mOffButton, mOnButton, mSlaveOffButton, mSlaveOnButton;
    private ListView mListview;
    private ArrayAdapter<String> mMessageArrayAdapter;
    private ArrayList<String> mMessageNumberArray;
    private ArrayList<String> mMessageUserArray;

    private BluetoothAdapter mBluetoothAdapter = null;
    private SharedPreferences sharedpreferences;

    private static final int REQUEST_ENABLE_BT = 3;
    public static final String MYPREFERENCES = "BtPrefs";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slave_activity);
        dbHelper = Database.getInstance(getApplicationContext());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sharedpreferences = getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);

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
            Cursor cursor = dbHelper.getSMS();
            if (cursor != null) {
                cursor.moveToFirst();
                do {

                    mMessageUserArray.add(cursor.getString(2));
                    mMessageNumberArray.add(cursor.getString(1));
                    mMessageArrayAdapter.add(cursor.getString(2) + ": " + cursor.getString(3));
                    Log.d(TAG, "sendMessage: " + cursor.getString(1) + " " + cursor.getString(2) + " "
                            + cursor.getString(3) + " " + cursor.getString(4));
                } while (cursor.moveToNext());
                mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String value = (String) mMessageArrayAdapter.getItem(position);

                        if (mMessageNumberArray.get(position).substring(0, 1).equals("+")) {
                            android.support.v4.app.DialogFragment addingTaskDialogFragment2 = new AddingTaskDialogFragment2();
                            Bundle args = new Bundle();

                            args.putString("user", mMessageUserArray.get(position));

                            args.putString("number", mMessageNumberArray.get(position));
                            args.putString("message", mMessageArrayAdapter.getItem(position));
//                    args.putInt("btnId", btnAdd.getId());
                            addingTaskDialogFragment2.setArguments(args);
                            addingTaskDialogFragment2.show(getSupportFragmentManager(), "AddingTaskDialogFragment");
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.no_reply, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
            }
        }
    };


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
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }

        }
        return false;
    }

    private void init() {
        mOnButton = (Button) findViewById(R.id.slave_start_btn);
        mOffButton = (Button) findViewById(R.id.slave_stop_btn);
        mSlaveOnButton = (Button) findViewById(R.id.button_s_on);
        mSlaveOffButton = (Button) findViewById(R.id.button_s_off);
        mListview = findViewById(R.id.in);
        mMessageArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mMessageNumberArray = new ArrayList<>();
        mMessageUserArray = new ArrayList<>();
        mListview.setAdapter(mMessageArrayAdapter);

        mOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


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
            }
        });

        mOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isMyServiceRunning()) {
                    // Send a message using content of the edit text widget
                    getApplicationContext().stopService(new Intent(getApplicationContext(), SlaveService.class));
                    Toast.makeText(getApplicationContext(), R.string.service_stopped, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.service_not_running, Toast.LENGTH_SHORT).show();

                }
            }
        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_enabled,
                            Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
        }
    }


    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    @Override
    public void onReply(String number, String text) {

        Log.d(TAG, "onReply: " + number + ": " + text);
    }
}
