package com.mats.bluetooth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.ResultReceiver;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import com.mats.bluetooth.listeners.SmsListener;
import com.mats.bluetooth.listeners.SmsListener.Listener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


public class MasterService extends Service implements Listener {

//    private final IBinder mBinder = new LocalBinder();

    private Constants mConstants;
    final int handlerState = 0;                        //used to identify handler message
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private SmsListener mSmsListener;
    private static final String TAG = "MasterService";
    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;
    //    private AcceptThread mSecureAcceptThread;
    private Handler mRetryHandler;
    public Boolean isRunning = false;
    private ResultReceiver mResultReceiver;
    private boolean doDestroy = false;
    private int timesRetried = 0;
    private boolean stopThread;
    private boolean isHandlerRunning = false;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    //    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
//    private static final String MAC_ADDRESS = "DC:66:72:B7:BB:48";
    private static String MAC_ADDRESS;
    private StringBuilder recDataString = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICE CREATED");
        stopThread = false;
        mConstants = new Constants();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {

            if ("REGISTER_RECEIVER".equals(intent.getAction())) {
                Log.d("BT SERVICE", "REGISTERING RECEIVER");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                sendResult(2);

            } else if ("UNREGISTER_RECEIVER".equals(intent.getAction())) {

                mResultReceiver = null;
            } else if ("FIRST_START".equals(intent.getAction())) {

                Log.d("BT SERVICE", "SERVICE STARTED");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                MAC_ADDRESS = intent.getExtras().getString("mac_address");






/*

                BroadcastReceiver intentReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        String name;
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            name = device.getName();
                            Log.v(TAG, "Device=" + device.getName());
                        } else {
                            name = "None";
                        }

                        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {


//                            if (!isRunning2) {
//                                Log.v(TAG, "connected: but still running " + device);
//
//                            } else {
                            if (device.equals(MAC_ADDRESS)) {
                                Log.d(TAG, "onReceive: MATCHAR");
//                                checkBTState();
                            }
                            Log.v(TAG, "connected: " + device);
//                            }
                        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                            Log.v(TAG, "disconnected: " + device);
                        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                            Log.v(TAG, "found:" + device);
                        } else if (btAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                            Log.v(TAG, "Discovery started");
                        } else if (btAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                            Log.v(TAG, "Discovery finished");
                        }
                    }
                };

                IntentFilter intentFilter;

                intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(intentReceiver, intentFilter);
*/

                init();


            }


        }
        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    private void init() {

        Intent notificationIntent = new Intent(this, MasterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "Android O")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle("BT Master")
                .setContentText("Waiting for stuff to do.")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter

        isHandlerRunning = false;
        isRunning = true;
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                 //if message is what we want
                    String readMessage = (String) msg.obj;      // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);          // `enter code here`
                    Log.d("RECORDED", recDataString.toString());
                    String message = recDataString.toString();
                    doStuff(message);
                    // Do stuff here with your data, like adding it to the database
                }
                recDataString.delete(0, recDataString.length());                    //clear all string data
            }
        };

        mSmsListener = new SmsListener();
        mSmsListener.setListener(this);
        mRetryHandler = new Handler(Looper.getMainLooper());
        checkBTState();


    }

    private void sendResult(int number) {
        if (mResultReceiver != null)
            mResultReceiver.send(number, null);
    }

    private void doStuff(String inMessage) {
        Log.d(TAG, "doStuff: " + inMessage);
        if (inMessage.substring(0, 8).equals("(GETCON)")) {

        } else if (inMessage.substring(0, 8).equals("(SNDSMS)")) {
            inMessage = inMessage.replaceAll("\\(SNDSMS\\)", "(");

            String number = inMessage.substring(inMessage.indexOf("(NUMBER") + 7, inMessage.indexOf("NUMBER)"));
            String message = inMessage.substring(inMessage.indexOf("(MESSAGE") + 8, inMessage.indexOf("MESSAGE)"));

            message = message.replaceFirst("\\(NUMBER.*NUMBER\\)", "");
//            message = message.replaceAll("\\(SNDSMS\\)", "");
            sendSMS(message, number);
        }

 /*       else if (inMessage.substring(0, 8).equals("(MRKRED)")) {

            inMessage = inMessage.replaceAll("\\(MRKRED\\)", "");
            String number = inMessage.substring(inMessage.indexOf("(") + 1, inMessage.indexOf("NUMBER)"));
            inMessage = inMessage.replaceAll("\\(.*NUMBER\\)", "");
            String id = inMessage.substring(inMessage.indexOf("(") + 1, inMessage.indexOf("ID)"));
            inMessage = inMessage.replaceAll("\\(.*ID\\)", "");

            String message = inMessage.substring(inMessage.indexOf("(") + 1, inMessage.indexOf("MESSAGE)"));
            markMessageRead(number, message, id);

            Log.d(TAG, "doStuff: Kommer hit nummer " + number + " meddelande " + message + " id " + id);

        }*/
    }


    //Funkar bara om appen är registrerad som default SMS app

/*    private void markMessageRead(String number, String body, String id) {

        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, "read = 0", null, null);
        cursor.moveToFirst();
        try{
            int i = 1;
            while (cursor.moveToNext()) {
                Log.d(TAG, "markMessageRead: count " + i);
                i++;
                if ((cursor.getString(cursor.getColumnIndex("address")).equals(number)) && (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                    Log.d(TAG, "markMessageRead: 1 ");
                    if (cursor.getString(cursor.getColumnIndex("body")).startsWith(body)) {
                        Log.d(TAG, "markMessageRead: 2 ");
//                        if (cursor.getString(cursor.getColumnIndex("_id")).startsWith(id)) {
                            Log.d(TAG, "markMessageRead: 3 ");
                            String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                            ContentValues values = new ContentValues();
                            values.put("read", true);
                            getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                            return;
//                         }else {
//                            Log.d(TAG, "markMessageRead: else 3 id " + cursor.getColumnIndex("_id"));
//                        }
                    }else {
                        Log.d(TAG, "markMessageRead: else 2 bdy " + cursor.getColumnIndex("body"));
                    }
                }
            }
            cursor.close();
        }catch(Exception e)
        {
            Log.e("Mark Read", "Error in Read: "+e.toString());
        }
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        doDestroy = true;
//        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        isRunning = false;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }
        btAdapter = null;
        mSmsListener = null;
        Log.d("SERVICE", "onDestroy");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    private void startListening(){
//
//        if (mSecureAcceptThread == null) {
//            mSecureAcceptThread = new AcceptThread(true);
//            mSecureAcceptThread.start();
//            Log.d(TAG, "onStartCommand: STARTED");
//        }
//
//    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }
        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
            stopSelf();
        } else {
            if (btAdapter.isEnabled()) {
                Log.d("DEBUG BT", "BT ENABLED! BT ADDRESS : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                try {
                    BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                    Log.d("DEBUG BT", "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + MAC_ADDRESS);
                    mConnectingThread = new ConnectingThread(device);
                    mConnectingThread.start();
                } catch (IllegalArgumentException e) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : " + e.toString());
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE");
                    stopSelf();
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    @Override
    public void onTextReceived() {
        Log.d(TAG, "onTextReceived: ");
        if (mConnectedThread != null || mConnectingThread != null) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMessage();
                }
            }, 1000);
        }
    }


    private void sendMessage() {

        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox?simple=true"), null, "read = 0", null, null);
        Log.d(TAG, "sendMessage: ");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.tiger);
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
//        Log.d(TAG, "sendMessage: tempSize = " + temp.length());
        temp = Constants.IMG + temp + Constants.ITEM_STOP + Constants.DELIMITER_STRING;
//        temp = "[IMG]" + temp;

        if (cursor != null && cursor.moveToFirst()) { // must check the result to prevent exception
            int i = 0;
//            Log.d(TAG, "sendMessage: " + cursor.getString(0) + " " + cursor.getString(4) + " " + cursor.getString(5));
            ArrayList<String> mArrayList = new ArrayList<String>();
//            Log.d(TAG, "sendMessage: " + Arrays.toString(cursor.getColumnNames()));
            mConnectedThread.write(Constants.START_STRING);

            do {
                if (cursor.getInt(7) == 0) {
                    String user1 = getContactName(cursor.getString(2));
                    String user = getContactName(user1);
                    mArrayList.add(Constants.SMS + "(NUMBER" + cursor.getString(2) + "NUMBER)" + "(USER" + user + "USER)"
                            + "(DATE" + cursor.getString(4) + "DATE)"
                            + "(ID" + cursor.getString(0) + "ID)" + "(MESSAGE" + cursor.getString(12) + "MESSAGE)");
                    mConnectedThread.write("[SMS]" + "(NUMBER" + cursor.getString(2) + "NUMBER)" + "(USER" + user + "USER)"
                            + "(DATE" + cursor.getString(4) + "DATE)"
                            + "(ID" + cursor.getString(0) + "ID)" + "(MESSAGE" + cursor.getString(12) + "MESSAGE)" + Constants.ITEM_STOP
                            + Constants.DELIMITER_STRING);

                } else {
                    Log.d(TAG, "sendMessage: Redan läst: " + cursor.getString(7));
                }
                i++;
//                    cursor.moveToNext();


            } while /*(i < 10)*/(cursor.moveToNext());
            cursor.close();
            mConnectedThread.write(temp);
            if (mArrayList.size() > 0) {
                // Get the message bytes and tell the BluetoothService to write
//                            byte[] send = msgData.getBytes();




  /*              String send = mArrayList.toString();
                send = send + "[END121212]";
                mConnectedThread.write(send);
                Log.d(TAG, "sendMessage: Message sent" + send.length());
*/


//                mConnectedThread.write(send);

                // Reset out string buffer to zero and clear the edit text field
//                mConnectedThread.setLength(0);
//                    mOutEditText.setText(mOutStringBuffer);
            }

        } else {
            // empty box, no SMS
            Log.d(TAG, "sendMessage: No sms");
        }
//        mConnectedThread.closeStreams();
        mConnectedThread.write(Constants.STOP_STRING);

    }


    private String getContactName(final String phoneNumber) {


        String out = phoneNumber;

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName = "";
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0);
//                Log.d(TAG, "getContactName: " + contactName);
            }
            cursor.close();
//                    Log.d(TAG, "getContactName:" + contactName + ".");
            if (contactName == "") {
                out = phoneNumber;
            } else {
                out = contactName;
            }
        }


//        else {
//            checkPermission(getContext(), Manifest.permission.READ_CONTACTS);
//
//            out = phoneNumber;
//        }
        return out;
    }


    private void sendSMS(String message, String number) {
        Log.d(TAG, "sendSMS: " + message);
//        String number = message.substring(message.indexOf("(|") + 2, message.indexOf("|)"));
//        String test = message.replaceAll("\\(\\|.*\\|\\)", "");

        Log.d(TAG, "sendSMS: Skickar till!" + number);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);

    }

    private void doRetry() {
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }

        isHandlerRunning = true;
        if (timesRetried <= 3 && !doDestroy) {
            mRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkBTState();
                    timesRetried++;
                    isHandlerRunning = false;
                }
            }, 10000);
            Log.d(TAG, "doRetry, not Destroy < 3" + timesRetried);

        } else if (timesRetried > 3 && !doDestroy) {

            Log.d(TAG, "doRetry, not Destroy > 3" + timesRetried);
            mRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkBTState();
                    timesRetried++;
                    isHandlerRunning = false;
                }
            }, 60000);
        } else {
            Log.d(TAG, "doRetry, Destroy " + timesRetried);

            stopSelf();
        }
//        }
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            Log.d("DEBUG BT", "MAC ADDRESS : " + MAC_ADDRESS);
            Log.d("DEBUG BT", "BT UUID : " + BTMODULEUUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
                Log.d("DEBUG BT", "SOCKET CREATED : " + temp.toString());
            } catch (IOException e) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN");
            // Establish the Bluetooth socket connection.
            // Cancelling discovery as it may slow down connection
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("DEBUG BT", "BT SOCKET CONNECTED");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED");
//                mConnectedThread.write("x");
                sendMessage();
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
//                    isRunning2 = false;
                    if (!isHandlerRunning) {
                        doRetry();
                    } else {
                        Log.d(TAG, "run1: not retry");
                    }
                } catch (IOException e2) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :" + e2.toString());
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                    //insert code to deal with this
                }
            } catch (IllegalStateException e) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : " + e.toString());
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeSocket() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    // New Class for Connected Thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            Log.d("DEBUG BT", "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
//                isRunning2 = false;
                if (!isHandlerRunning) {
                    doRetry();
                } else {
                    Log.d(TAG, "run2: not retry");
                }
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[Constants.BUFFERSIZE];
            int bytes;
            timesRetried = 0;

            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
//                    isRunning2 = false;

                    if (!isHandlerRunning) {
                        doRetry();
                    } else {
                        Log.d(TAG, "run3: not retry");
                    }
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                Log.d(TAG, "write: " + msgBuffer.length);
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
//                checkBTState();
//                startListening();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
//                startListening();
            }
        }
    }


}
