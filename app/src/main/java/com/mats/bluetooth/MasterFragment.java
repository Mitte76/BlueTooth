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
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class MasterFragment extends Fragment {

    private static final String TAG = "MasterFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private final int GET_SMS_READ_PERMISSION = 1;
    private final int GET_SMS_SEND_PERMISSION = 2;
    private final int GET_CONTACTS_PERMISSION = 3;
    private int READ_SMS_PERMISSION = 0;
    private int SEND_SMS_PERMISSION = 0;
    private int CONTACT_PERMISSION = 0;

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
            checkPermission();
        }

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        checkPermission();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.master_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();

    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
//        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
//                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
//                    String message = textView.getText().toString();
                    sendMessage();
                }
            }
        });


        // Initialize the BluetoothService to perform bluetooth connections
        mChatService = new BluetoothService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");


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

        if (READ_SMS_PERMISSION == 1) {
            Cursor cursor = getActivity().getContentResolver().query(Uri.parse("content://sms/inbox"), null, "read = 0", null, null);
            if (cursor.moveToFirst()) { // must check the result to prevent exception
                int i = 0;
                ArrayList<String> mArrayList = new ArrayList<String>();
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
                    i++;
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

    }


//    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
//    private TextView.OnEditorActionListener mWriteListener
//            = new TextView.OnEditorActionListener() {
//        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//            // If the action is a key-up event on the return key, send the message
//            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                String message = view.getText().toString();
//                sendMessage(/*message*/);
//            }
//            return true;
//        }
//    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to_slave, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(activity, readMessage,
                            Toast.LENGTH_SHORT).show();
                    sendSMS(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void sendSMS(String message){

        if (SEND_SMS_PERMISSION == 1) {

            String number = message.substring(message.indexOf("(|") + 2, message.indexOf("|)"));
            String test = message.replaceAll("\\(\\|.*\\|\\)", "");

            Log.d(TAG, "sendSMS: Skickar till!" + number);
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, test, null, null);
        } else {
            checkPermission();
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
//                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.bluetooth_chat, menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
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
                checkPermission();
                return true;
            }
        }
        return false;
    }

    public String getContactName(final String phoneNumber, Context context) {


            String out = phoneNumber;

            if(CONTACT_PERMISSION == 1){
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
            } else {
                out = phoneNumber;
            }
            return out;
    }

    private void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.READ_SMS)) {
            /* do nothing*/
                }
                requestPermissions(
                        new String[]{Manifest.permission.READ_SMS}, GET_SMS_READ_PERMISSION);
            }
            else {
                READ_SMS_PERMISSION = 1;
            }

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.SEND_SMS)) {
            /* do nothing*/
                }
                requestPermissions(
                        new String[]{Manifest.permission.SEND_SMS}, GET_SMS_SEND_PERMISSION);
            }
            else {
                SEND_SMS_PERMISSION = 1;
            }

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.READ_CONTACTS)) {
            /* do nothing*/
                }
                requestPermissions(
                        new String[]{Manifest.permission.READ_CONTACTS}, GET_CONTACTS_PERMISSION);
            } else {
                CONTACT_PERMISSION = 1;
            }
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {

            case GET_SMS_READ_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    READ_SMS_PERMISSION = 1;
                    checkPermission();
//                    sendMessage();
                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!" + grantResults.length);
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (SMS)" + grantResults.length);
                    Toast.makeText(getActivity(), "You need access to SMS to run this app",
                            Toast.LENGTH_LONG).show();
                    getActivity().finish();
//                    System.exit(0);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case GET_CONTACTS_PERMISSION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    CONTACT_PERMISSION = 1;

                    Log.d(TAG, "onRequestPermissionsResult: permission was granted, yay!");

                } else {
                    CONTACT_PERMISSION = 0;
                    Log.d(TAG, "onRequestPermissionsResult: Permission not granted (Contact)");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
