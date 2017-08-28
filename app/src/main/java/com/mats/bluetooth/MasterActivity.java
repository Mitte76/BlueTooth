package com.mats.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by mats on 2017-08-28.
 */

public class MasterActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MasterActivity";
    private Toolbar toolbar;
    private Button mOffButton, mOnButton, mSlaveOffButton, mSlaveOnButton;
    private TextView infoText;

    private MasterService mService;
    private boolean mBound = false;



    private BluetoothAdapter mBluetoothAdapter = null;
    SharedPreferences sharedpreferences;

    private final String[] PERMISSIONS = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_COARSE_LOCATION};
    private final String[] SMS_PERMISSION = {Manifest.permission.RECEIVE_SMS};
    private static final int REQUEST_ENABLE_BT = 3;
    public static final String MYPREFERENCES = "BtPrefs";
    private static final int SELECT_SLAVE_DEVICE = 1;
    private String SLAVE_MAC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.master_fragment);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sharedpreferences = getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (shouldWeAsk("first")) {
                    Log.d(TAG, "onCreate: first");
                    testPermission(PERMISSIONS);
                    markAsAsked("first");
                } else {
                    testPermission(SMS_PERMISSION);
                    Log.d(TAG, "onCreate: not first");
                }
            }
            Intent intent = new Intent(this, MasterService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }


        if (savedInstanceState == null) {

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MasterService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MasterService.LocalBinder binder = (MasterService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_slave: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, SELECT_SLAVE_DEVICE);
                return true;
            }
//            case R.id.insecure_connect_scan: {
//                // Launch the DeviceListActivity to see devices and do scan
//                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
//                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
//                return true;
//            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.permission: {
                testPermission(PERMISSIONS);
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
        mSlaveOnButton = (Button) findViewById(R.id.button_s_on);
        mSlaveOffButton = (Button) findViewById(R.id.button_s_off);
        infoText = (TextView) findViewById(R.id.info);


        mOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

//
//                mChatService.stop();
//                mBluetoothAdapter.cancelDiscovery();

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (SLAVE_MAC != null) {
                        Intent service = new Intent(getApplicationContext(), MasterService.class);
                        service.putExtra("version", 0);
                        service.putExtra("mac_address", SLAVE_MAC);
                        getApplicationContext().startService(service);
                        infoText.setText(R.string.service_started);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.select_bt_first, Toast.LENGTH_SHORT).show();

                    }
                }


            }
        });

        mOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                getApplicationContext().stopService(new Intent(getApplicationContext(), MasterService.class));
                infoText.setText(R.string.service_stopped);

            }
        });

        mSlaveOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                Log.d(TAG, "onClick: " + mService.getMac());

//                getApplicationContext().stopService(new Intent(getApplicationContext(), MasterService.class));
//                infoText.setText(R.string.service_stopped);

            }
        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    infoText.setText(R.string.select_bt_msg);
                    init();
                } else {
                    // User did not enable Bluetooth or an error occurred
//                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            case AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE:
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
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
                    infoText.setText(R.string.start_service_available);
                }
                break;
        }
    }


    private void testPermission(String... permissions) {

        if (EasyPermissions.hasPermissions(this, permissions)) {
            // Already have permission, do the thing
            // ...
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
