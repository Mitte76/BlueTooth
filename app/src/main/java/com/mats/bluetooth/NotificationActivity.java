package com.mats.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.Adapter.NotificationAdapter;
import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.Helper.SwipeUtil;

import java.util.ArrayList;


public class NotificationActivity extends AppCompatActivity{
    private Database dbHelper;
    private TextView toolbarText;
    private static final String TAG = "NotificationActivity";
    private ImageView toolbarStatusImg;

    private RecyclerView mRecyclerView;
    private ArrayList<String> mMessageNumberArray;
    private ArrayList<String> mMessageUserArray;

    private BluetoothAdapter mBluetoothAdapter = null;
//    private SharedPreferences sharedpreferences;

    private static final int REQUEST_ENABLE_BT = 3;
    //    public static final String MYPREFERENCES = "BtPrefs";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_activity);
        dbHelper = Database.getInstance(getApplicationContext());
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbarText = findViewById(R.id.batteryText);
        toolbarStatusImg = findViewById(R.id.toolbarServiceStatus);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), SlaveService.class);
            service.setAction("REGISTER_RECEIVER");
            service.putExtra("ResultReceiver", mResultReceiver);
            service.putExtra("ResultReceiver_ID", hashCode());
            getApplicationContext().startService(service);

            Log.d(TAG, "onCreate: Running");
        } else {
            toolbarStatusImg.setImageResource(R.drawable.red_status);
            Log.d(TAG, "onCreate: NOT Running");
        }

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

    }

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
                    toolbarStatusImg.setImageResource(R.drawable.orange_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_CONNECTED:
                    toolbarStatusImg.setImageResource(R.drawable.green_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (toolbarText != null) {
                toolbarText.setText(String.valueOf(level) + "%");
            }
        }
    };

    private void redrawScreen() {

        NotificationAdapter notificationAdapter = new NotificationAdapter(dbHelper.getNotifications());
        if (mRecyclerView == null) {
            mRecyclerView = findViewById(R.id.recyclerView);
            mRecyclerView.setAdapter(notificationAdapter);
        } else {
            mRecyclerView.setAdapter(notificationAdapter);
        }


        setSwipeForRecyclerView();


    }

    private void setSwipeForRecyclerView() {

        SwipeUtil swipeHelper = new SwipeUtil(0, ItemTouchHelper.LEFT, this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int swipedPosition = viewHolder.getAdapterPosition();
                NotificationAdapter adapter = (NotificationAdapter) mRecyclerView.getAdapter();
                adapter.pendingRemoval(swipedPosition);
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                NotificationAdapter adapter = (NotificationAdapter) mRecyclerView.getAdapter();
                if (adapter.isPendingRemoval(position)) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };

        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(swipeHelper);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);


        //set swipe label
        swipeHelper.setLeftSwipeLable("");
        //set swipe background-Color
        swipeHelper.setLeftcolorCode(ContextCompat.getColor(this, R.color.red));

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
        unregisterReceiver(mBatInfoReceiver);

    }


    @Override
    public void onResume(){
        super.onResume();
        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), SlaveService.class);
            service.setAction("REGISTER_RECEIVER");
            service.putExtra("ResultReceiver", mResultReceiver);
            service.putExtra("ResultReceiver_ID", hashCode());
            getApplicationContext().startService(service);

            Log.d(TAG, "onCreate: Running");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), SlaveService.class);
            service.setAction("REGISTER_RECEIVER");
            service.putExtra("ResultReceiver", mResultReceiver);
            service.putExtra("ResultReceiver_ID", hashCode());
            getApplicationContext().startService(service);

            Log.d(TAG, "onCreate: Running");
        }
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
        getMenuInflater().inflate(R.menu.notification, menu);
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
        mRecyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        redrawScreen();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        redrawScreen();
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
}
