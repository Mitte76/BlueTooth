package com.mats.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.listeners.NotificationListener;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by mats on 2017-08-28.
 */

public class MasterActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MasterActivity";
    private Toolbar toolbar;
    private TextView mToolbarCountText;
    private Button mOffButton, mOnButton;
    private MasterService mService;
    private boolean mBound = false;
    private ImageView toolbarStatusImg;
    private CountDownTimer timer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private SharedPreferences sharedpreferences;

    private final String[] PERMISSIONS = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_COARSE_LOCATION};
    private final String[] SMS_PERMISSION = {Manifest.permission.RECEIVE_SMS};
    private static final int REQUEST_ENABLE_BT = 3;
    public static final String MYPREFERENCES = "BtPrefs";
    private static final int SELECT_SLAVE_DEVICE = 1;
    private String SLAVE_MAC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        ComponentName cn = new ComponentName(this, NotificationListener.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());
        Log.d(TAG, "onCreate: Notification running = " + enabled);
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !enabled) {
            if (n.isNotificationPolicyAccessGranted()) {

            } else {
                // Ask the user to grant access
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            }
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.master_activity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbarStatusImg = findViewById(R.id.toolbarServiceStatus);
        mToolbarCountText = findViewById(R.id.toolbarCountdownText);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sharedpreferences = getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);


        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldWeAsk("first")) {
                    Log.d(TAG, "onCreate: first");
                    testPermission(PERMISSIONS);
                    markAsAsked("first");
                } else {
                    testPermission(SMS_PERMISSION);
                    Log.d(TAG, "onCreate: not first");
                }
            }

            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult

        }


        if (savedInstanceState == null) {

        }
//        if (mService.isRunning){
//            Log.d(TAG, "onClick: Running");
//        }else{
//            Log.d(TAG, "onClick: NOT Running");
//        }

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
                    mToolbarCountText.setText("");
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_CONNECTING:
                    toolbarStatusImg.setImageResource(R.drawable.orange_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_CONNECTED:
                    toolbarStatusImg.setImageResource(R.drawable.green_status);

                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.STATE_WAITING:
                    toolbarStatusImg.setImageResource(R.drawable.blue_status);
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.TIMER_10:
                    toolbarStatusImg.setImageResource(R.drawable.blue_status);
                    startTimer(10);
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.TIMER_60:
                    toolbarStatusImg.setImageResource(R.drawable.blue_status);
                    startTimer(60);
                    Log.d(TAG, "onReceiveResult: ");
                    break;
                case Constants.TIMER_CANCEL:
                    toolbarStatusImg.setImageResource(R.drawable.blue_status);
                    if (timer != null){
                        timer.cancel();
                    }
                    Log.d(TAG, "onReceiveResult: ");
                    break;

            }
        }
    };

    private void startTimer(int time) {

        timer = new CountDownTimer((time + 2) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mToolbarCountText.setText(String.valueOf((millisUntilFinished / 1000) - 2));

                if (((millisUntilFinished / 1000) - 1) == 0){
                    mToolbarCountText.setText("");
                    timer.cancel();
                }

            }

            public void onFinish() {
            }

        };
        timer.start();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.mats.bluetooth.MasterService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isMyServiceRunning()) {
            Intent i = new Intent(this, MasterService.class);
            i.setAction("UNREGISTER_RECEIVER");
            i.putExtra("ResultReceiver_ID", hashCode());
            startService(i);
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (mBound) {
//            unbindService(mConnection);
//            mBound = false;
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.master, menu);
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
                    if (SLAVE_MAC != null && !isMyServiceRunning()) {
                        Intent service = new Intent(getApplicationContext(), MasterService.class);
                        service.setAction("FIRST_START");
                        service.putExtra("ResultReceiver", mResultReceiver);
                        service.putExtra("ResultReceiver_ID", hashCode());
                        service.putExtra("mac_address", SLAVE_MAC);
                        getApplicationContext().startService(service);
                        Toast.makeText(getApplicationContext(), R.string.service_started, Toast.LENGTH_SHORT).show();

                    } else {
                        if (isMyServiceRunning()) {
                            Toast.makeText(getApplicationContext(), R.string.service_already_running, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.select_bt_first, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                return true;
            }
            case R.id.stop_service: {
                if (isMyServiceRunning()) {
                    getApplicationContext().stopService(new Intent(getApplicationContext(), MasterService.class));
                    Toast.makeText(getApplicationContext(), R.string.service_stopped, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.service_not_running, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.permission: {
                testPermission(PERMISSIONS);
                return true;
            }
            case R.id.select_slave: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, SELECT_SLAVE_DEVICE);
                return true;
            }


        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void init() {
        mOffButton = (Button) findViewById(R.id.button_off);
        mOnButton = (Button) findViewById(R.id.button_on);

        if (isMyServiceRunning()) {
            Intent service = new Intent(getApplicationContext(), MasterService.class);
            service.setAction("REGISTER_RECEIVER");
            service.putExtra("ResultReceiver", mResultReceiver);
            service.putExtra("ResultReceiver_ID", hashCode());
            getApplicationContext().startService(service);

            Log.d(TAG, "onCreate: Running");
        } else {
            toolbarStatusImg.setImageResource(R.drawable.red_status);

            Log.d(TAG, "onCreate: NOT Running");

        }

        mOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (SLAVE_MAC != null && !isMyServiceRunning()) {
                        Intent service = new Intent(getApplicationContext(), MasterService.class);
                        service.setAction("FIRST_START");
                        service.putExtra("ResultReceiver", mResultReceiver);
                        service.putExtra("ResultReceiver_ID", hashCode());
                        service.putExtra("mac_address", SLAVE_MAC);
                        getApplicationContext().startService(service);
                        Toast.makeText(getApplicationContext(), R.string.service_started, Toast.LENGTH_SHORT).show();

                    } else {
                        if (isMyServiceRunning()) {
                            Toast.makeText(getApplicationContext(), R.string.service_already_running, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.select_bt_first, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        mOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                if (isMyServiceRunning()) {
                    getApplicationContext().stopService(new Intent(getApplicationContext(), MasterService.class));
                    Toast.makeText(getApplicationContext(), R.string.service_stopped, Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), R.string.service_not_running, Toast.LENGTH_SHORT).show();

                }

            }
        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {


            case AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE:
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, R.string.returned_from_app_settings_to_activity_SMS_not_granted, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case SELECT_SLAVE_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    SLAVE_MAC = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                }
                break;
        }
    }


    private void testPermission(String... permissions) {

        if (EasyPermissions.hasPermissions(this, permissions)) {
            init();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.permission_explained),
                    1, permissions);
        }


    }


    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
//        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.

        if (perms.contains(Manifest.permission.RECEIVE_SMS)) {
            new AppSettingsDialog.Builder(this).build().show();
        }

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {

        if (list.contains(Manifest.permission.RECEIVE_SMS)) {
            init();
        }

    }

    private boolean shouldWeAsk(String permission) {

        return (sharedpreferences.getBoolean(permission, true));

    }


    private void markAsAsked(String permission) {

        sharedpreferences.edit().putBoolean(permission, false).apply();

    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


}
