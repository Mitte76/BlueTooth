/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mats.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.listeners.SmsListener.Listener;

import java.util.ArrayList;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class MasterFragment extends Fragment implements Listener {

    private static final String TAG = "MasterFragment";
    private String SLAVE_MAC;
    // Intent request codes
    private static final int SELECT_SLAVE_DEVICE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private Button mOffButton, mOnButton, mSlaveOffButton, mSlaveOnButton;
    private TextView infoText;
    private final int GET_SMS_PERMISSION = 1;
    private final int GET_CONTACT_PERMISSION = 2;
    private final int GET_LOCATION_PERMISSION = 3;
    private final int GET_ALL_PERMISSION = 4;
    private final String[] PERMISSIONS = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_COARSE_LOCATION};


    private String SMS_PERMISSION = Manifest.permission.READ_SMS;
    private String CONTACT_PERMISSION = Manifest.permission.READ_CONTACTS;
    //    private int CONTACT_PERMISSION = 0;
    private int LOCATION_PERMISSION = 0;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mChatService = null;

    /**
     * Listener for new sms
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            checkPermission(getContext(), PERMISSIONS);
        }

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (mChatService != null) {
//            mChatService.stop();
//        }

    }

    @Override
    public void onResume() {
        super.onResume();
//        checkPermission();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//        if (mChatService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mChatService.getState() == BluetoothService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                mChatService.start();
//            }
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.master_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mOffButton = (Button) view.findViewById(R.id.button_off);
        mOnButton = (Button) view.findViewById(R.id.button_on);
        mSlaveOnButton = (Button) view.findViewById(R.id.button_s_on);
        mSlaveOffButton = (Button) view.findViewById(R.id.button_s_off);
        infoText = (TextView) view.findViewById(R.id.info);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
        }

        infoText.setText(R.string.select_bt_msg);

        mOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();

//
//                mChatService.stop();
//                mBluetoothAdapter.cancelDiscovery();

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (SLAVE_MAC != null) {
                        Intent service = new Intent(getActivity(), MasterService.class);
                        service.putExtra("version", 0);
                        service.putExtra("mac_address", SLAVE_MAC);
                        getActivity().startService(service);
                        infoText.setText(R.string.service_started);
                    } else {
                        Toast.makeText(getActivity(), R.string.select_bt_first, Toast.LENGTH_SHORT).show();

                    }
                }


            }
        });

        mOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();

                getActivity().stopService(new Intent(getActivity(), MasterService.class));
                infoText.setText(R.string.service_stopped);

            }
        });

        mSlaveOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();

                getActivity().stopService(new Intent(getActivity(), MasterService.class));
                infoText.setText(R.string.service_stopped);

            }
        });



        mSlaveOnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();

//
//                mChatService.stop();
//                mBluetoothAdapter.cancelDiscovery();

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (SLAVE_MAC != null) {
                        Intent service = new Intent(getActivity(), MasterService.class);
                        service.putExtra("version", 1);
                        service.putExtra("mac_address", SLAVE_MAC);
                        getActivity().startService(service);
                        infoText.setText(R.string.service_started);
                    } else {
                        Toast.makeText(getActivity(), R.string.select_bt_first, Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });

    }


    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    //    /**
//     * Sends a message.
//     *
//     * @param message A string of text to send.
//     */
    private void sendMessage(/*String message*/) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();

            return;
        }

//        if(!checkPermission(getContext(), Manifest.permission.SEND_SMS)){
//            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, GET_SMS_PERMISSION);
//        }
        if (!checkPermission(getActivity(), SMS_PERMISSION)) {
            getPermission(getContext(), GET_SMS_PERMISSION, SMS_PERMISSION);
        } else /*(checkPermission(getActivity(),Manifest.permission.READ_SMS))*/ {
            Cursor cursor = getActivity().getContentResolver().query(Uri.parse("content://sms/inbox?simple=true"), null, "read = 0", null, null);
            Log.d(TAG, "sendMessage: ");

            if (cursor != null && cursor.moveToFirst()) { // must check the result to prevent exception
                ArrayList<String> mArrayList = new ArrayList<>();
                do {
                    if (cursor.getInt(7) == 0) {
//                        Log.d(TAG, "sendMessage: " + cursor.getCount());
                        String user1 = getContactName(cursor.getString(2), getContext());
//                        Log.d(TAG, "sendMessage: " + user1);
                        String user = getContactName(user1, getContext());
                        mArrayList.add("[SMS]" + "(|" + cursor.getString(2) + "|)" + "(" + user /*cursor.getString(2)*/ + ") " + cursor.getString(12));
                    } else {
                        Log.d(TAG, "sendMessage: Redan läst: " + cursor.getString(7));
                    }
//                    cursor.moveToNext();


                } while /*(i < 10)*/(cursor.moveToNext());
                cursor.close();

                if (mArrayList.size() > 0) {
                    // Get the message bytes and tell the BluetoothService to write
//                            byte[] send = msgData.getBytes();
                    byte[] send = mArrayList.toString().getBytes();
                    mChatService.write(send);

                    // Reset out string buffer to zero and clear the edit text field
                    mOutStringBuffer.setLength(0);
//                    mOutEditText.setText(mOutStringBuffer);
                }

            } else {
                // empty box, no SMS
                Log.d(TAG, "sendMessage: No sms");
            }

        }
//        else {
//            Log.d(TAG, "sendMessage: No permission to read sms");
//        }

    }


//    }


    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle(resId);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle(subTitle);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mChatService != null) {
            mChatService.stop();
        }
//        if (mHandler != null) {
//            mHandler.removeCallbacksAndMessages(null);
//            Log.d(TAG, "onDestroyView: ");
//        }

    }
//
//    /**
//     * The Handler that gets information back from the BluetoothService
//     */
//    private final Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            FragmentActivity activity = getActivity();
//            switch (msg.what) {
//                case Constants.MESSAGE_STATE_CHANGE:
//                    switch (msg.arg1) {
//                        case BluetoothService.STATE_CONNECTED:
//                            setStatus(getString(R.string.title_connected_to_slave, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
//                            break;
//                        case BluetoothService.STATE_CONNECTING:
//                            setStatus(R.string.title_connecting);
//                            break;
//                        case BluetoothService.STATE_LISTEN:
//                        case BluetoothService.STATE_NONE:
//                            setStatus(R.string.title_not_connected);
//                            break;
//                    }
//                    break;
//                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
////                    mConversationArrayAdapter.add("Me:  " + writeMessage);
//                    break;
//                case Constants.MESSAGE_READ:
//                    byte[] readBuf = (byte[]) msg.obj;
//                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    Toast.makeText(activity, readMessage,
//                            Toast.LENGTH_SHORT).show();
//                    sendSMS(readMessage);
//                    break;
//                case Constants.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
//                    break;
//                case Constants.MESSAGE_TOAST:
//                    if (null != activity) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                    break;
//            }
//        }
//    };

    private void sendSMS(String message) {

        if (checkPermission(getActivity(), Manifest.permission.SEND_SMS)) {

            String number = message.substring(message.indexOf("(|") + 2, message.indexOf("|)"));
            String test = message.replaceAll("\\(\\|.*\\|\\)", "");

            Log.d(TAG, "sendSMS: Skickar till!" + number);
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, test, null, null);
        } else {
            checkPermission(getContext(), Manifest.permission.SEND_SMS);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_SLAVE_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    SLAVE_MAC = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    infoText.setText(R.string.start_service_available);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
//                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    infoText.setText(R.string.select_bt_msg);
//                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
//                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }
//
//    /**
//     * Establish connection with other device
//     *
//     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
//     * @param secure Socket Security type - Secure (true) , Insecure (false)
//     */
//    private void connectDevice(Intent data, boolean secure) {
//        // Get the device MAC address
//        String address = data.getExtras()
//                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//        // Get the BluetoothDevice object
//        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        // Attempt to connect to the device
//        mChatService.connect(device, secure);
//    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.bluetooth_chat, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_slave: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, SELECT_SLAVE_DEVICE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.permission: {
                // Fix permissions
                if (!checkPermission(getContext(), PERMISSIONS)) {
                    getPermission(getContext(), GET_ALL_PERMISSION, PERMISSIONS);
//                    ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, GET_ALL_PERMISSION);
                }

                return true;
            }
        }
        return false;
    }

    private String getContactName(final String phoneNumber, Context context) {


        String out = phoneNumber;
        if (!checkPermission(getActivity(), CONTACT_PERMISSION)) {
            getPermission(getContext(), GET_CONTACT_PERMISSION, CONTACT_PERMISSION);
            out = phoneNumber;

        } else {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

            String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

            String contactName = "";
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contactName = cursor.getString(0);
                }
                cursor.close();
//                    Log.d(TAG, "getContactName:" + contactName + ".");
                if (contactName == "") {
                    out = phoneNumber;
                } else {
                    out = contactName;
                }
            }
        }

//        else {
//            checkPermission(getContext(), Manifest.permission.READ_CONTACTS);
//
//            out = phoneNumber;
//        }
        return out;
    }


    private void getPermission(Context context, int whatPermission, String... permissions) {


        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkPermission: " + permission);

                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),permission)) {
                    /* do nothing*/
                }
                requestPermissions(
                        new String[]{permission}, whatPermission);
//                return false;
            }
        }
    }


    private boolean checkPermission(Context context, String... permissions) {
//        Activity activity = (Activity) context;
        Log.d(TAG, "checkPermission: ovanför");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "checkPermission: " + permission);

                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            permission)) {
                        /* do nothing*/
                    }
                    return false;
                }
            }
        }
        return true;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case GET_ALL_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                        SMS_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  SMS");
                    }
                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                        CONTACT_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  CONTACT");
                    }
                    if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        LOCATION_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  LOCATION");
                    }
                }

//                else {
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (SMS)" + grantResults.length);
//                    Toast.makeText(getActivity(), "You need access to SMS to run this app",
//                            Toast.LENGTH_LONG).show();
//                    getActivity().finish();
//
//                }
                return;
            }

            case GET_SMS_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                        SMS_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  SMS");
                    }
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (SMS)");
                    Toast.makeText(getActivity(), "You need access to SMS to run this app",
                            Toast.LENGTH_LONG).show();
                    getActivity().finish();

                }
                return;
            }

            case GET_CONTACT_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                        CONTACT_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  CONTACT");
                    }
                }
//                else {
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (CONTACT)");
//                    Toast.makeText(getActivity(), "You will not be able to see the name of the sender" +
//                                    ", only the number if you deny this permission",
//                            Toast.LENGTH_LONG).show();
//
//                }
                return;
            }

            case GET_LOCATION_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        LOCATION_PERMISSION = 1;
                        Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!  CONTACT");
                    }
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (CONTACT)");
                    Toast.makeText(getActivity(), "You will not be able to scan for new bluetooth devices" +
                                    " if you do not grant location permission",
                            Toast.LENGTH_LONG).show();

                }
            }
//
//            case GET_CONTACTS_PERMISSION: {
//
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    CONTACT_PERMISSION = 1;
//
//                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!");
//
//                } else {
////                    CONTACT_PERMISSION = 0;
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (Contact)");
//                }
//            }
//
//            case GET_COARSE_LOCATION_PERMISSION: {
//
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    LOCATION_PERMISSION = 1;
//
//                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!");
//
//                } else {
////                    CONTACT_PERMISSION = 0;
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (Contact)");
//                }
//            }
//
//
//            case GET_SMS_RECEIVE_PERMISSION: {
//
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    RECEIVE_SMS_PERMISSION = 1;
//
//                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!");
//
//                } else {
////                    CONTACT_PERMISSION = 0;
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (Contact)");
//                }
//            }
//            // other 'case' lines to check for other
//            // permissions this app might request
//            case GET_SMS_SEND_PERMISSION: {
//
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    SEND_SMS_PERMISSION = 1;
//
//                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!");
//
//                } else {
////                    CONTACT_PERMISSION = 0;
//                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (Contact)");
//                }
//            }
//


        }
    }


    @Override
    public void onTextReceived(String text) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessage();
            }
        }, 1000);
    }
}
