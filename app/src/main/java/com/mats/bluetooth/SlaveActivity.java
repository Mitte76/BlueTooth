package com.mats.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.DbHelper.Database;


public class SlaveActivity extends AppCompatActivity {
    private Database dbHelper;
    private TextView toolbarText, messagesTxt;
    private ImageView toolbarStatusImg;
    private static final String TAG = "SmsActivity";

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slave_main_window);
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarText = findViewById(R.id.batteryText);
        toolbarStatusImg = findViewById(R.id.toolbarServiceStatus);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_running, Toast.LENGTH_LONG).show();
            this.finish();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                init();
                Log.d(TAG, "onCreate: INIT");
            }
        }

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


    }

    private void redrawScreen() {

    }


    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (toolbarText != null) {
                toolbarText.setText(String.valueOf(level) + "%");
            }
        }
    };

    private ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            switch (resultCode) {
                case Constants.REFRESH:
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_NONE:
                    toolbarStatusImg.setImageResource(R.drawable.red_status);
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_CONNECTING:
                    toolbarStatusImg.setImageResource(R.drawable.orange_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_LISTEN:
                    toolbarStatusImg.setImageResource(R.drawable.blue_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_CONNECTED:
                    toolbarStatusImg.setImageResource(R.drawable.green_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;

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
        unregisterReceiver(mBatInfoReceiver);

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onStart() {
        messagesTxt.setText(String.valueOf(dbHelper.getSMS().getCount()));

        init();
        Log.d(TAG, "onStart: DEN HAR STARTAT IGEN");
        super.onStart();
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
/*            case R.id.undelete: {
                dbHelper.markSmsUnDeleted();
                Cursor log = dbHelper.getSMSLog();
                Log.d(TAG, "onOptionsItemSelected: " + log.getCount());
                redrawScreen(null);
                return true;
            }*/
        }
        return false;
    }

    private void init() {
        if (!isMyServiceRunning()){
            toolbarStatusImg.setImageResource(R.drawable.red_status);
        }
        dbHelper = Database.getInstance(getApplicationContext());
        ImageView messagesImg = findViewById(R.id.slave_main_messageImageView);
        messagesTxt = findViewById(R.id.slave_main_messageTextView);

        messagesTxt.setText(String.valueOf(dbHelper.getSMS().getCount()));
        if (dbHelper.getFirstThreadMsg() != null){
            messagesImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    slaveIntent();
                }
            });
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ("REFRESH".equals(intent.getAction())) {
            init();
            Log.d(TAG, "onCreate: REFRESH");
        } else {
            Log.d(TAG, "init: WTF!" + getIntent().getAction());
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_enabled,
                            Toast.LENGTH_SHORT).show();
                    init();

                } else {
                    Toast.makeText(this, R.string.bt_not_enabled,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
        }
    }

    private void slaveIntent(){
        Intent slaveIntent = new Intent(this, SmsActivity.class);
        startActivity(slaveIntent);
    }
}
