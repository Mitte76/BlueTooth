package com.mats.bluetooth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.mats.bluetooth.Model.Msg;
import com.mats.bluetooth.listeners.NotificationListener;
import com.mats.bluetooth.listeners.SmsListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;


public class MasterService extends Service implements SmsListener.SmsListenerInterface, NotificationListener.NotificationListenerInterface {

    //    private final IBinder mBinder = new LocalBinder();
    private ArrayList<Msg> messages;
    final int handlerState = 0;                        //used to identify handler message
    private static Handler bluetoothIn;
    private int mState = Constants.STATE_NONE;
    private BluetoothAdapter btAdapter = null;
    private SmsListener mSmsListener;
    private NotificationListener mNotificationListener;
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
    private static final UUID BTMODULEUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static String MAC_ADDRESS;
    private StringBuilder recDataString = new StringBuilder();
    private BroadcastReceiver intentReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICE CREATED");
        stopThread = false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {

            if ("REGISTER_RECEIVER".equals(intent.getAction())) {
                mResultReceiver = intent.getParcelableExtra("ResultReceiver");
                sendResult(mState);

            } else if ("UNREGISTER_RECEIVER".equals(intent.getAction())) {

                mResultReceiver = null;
            } else if ("FIRST_START".equals(intent.getAction())) {

                Log.d("BT SERVICE", "SERVICE STARTED");
                mResultReceiver = intent.getParcelableExtra("ResultReceiver");
                MAC_ADDRESS = intent.getExtras().getString("mac_address");
                sendResult(mState);
                init();
            }


            intentReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                        Log.d(TAG, "onReceive: ccccc :  " + device.getAddress());

                        if (mState < Constants.STATE_CONNECTING) {
                            if (device.getAddress().equals(MAC_ADDRESS)) {
                                Log.d(TAG, "onReceive: MATCHAR");
                                checkBTState();
                            } else {
                                Log.d(TAG, "onReceive: device address: " + device.getAddress());
                            }
                        }
                    } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                            Log.v(TAG, "disconnected: " + device);
                        }

                        /*else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                            Log.v(TAG, "found:" + device);
                        } else if (btAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                            Log.v(TAG, "Discovery started");
                        } else if (btAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                            Log.v(TAG, "Discovery finished");
                        }*/
                }
            };

            IntentFilter intentFilter;

            intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            registerReceiver(intentReceiver, intentFilter);


        }
        return START_STICKY;
    }

    private void init() {

        Intent notificationIntent = new Intent(this, MasterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "ddd")
                .setSmallIcon(R.drawable.icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
                .setContentTitle("BT Master")
                .setContentText("Waiting for stuff to do.")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        isHandlerRunning = false;
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                 //if message is what we want
                    String readMessage = (String) msg.obj;      // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);          // `enter code here`
//                    Log.d("RECORDED", recDataString.toString());
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
        mNotificationListener = new NotificationListener();
        mNotificationListener.setListener(this);

        checkBTState();
        messages = new ArrayList<>();
    }

    private void sendResult(int number) {
        if (mResultReceiver != null)
            mResultReceiver.send(number, null);
    }

    private void doStuff(String inMessage) {
        Log.d(TAG, "doStuff: " + inMessage);
        if (inMessage.substring(0, 8).equals("(SNDSMS)")) {
            inMessage = inMessage.replaceAll("\\(SNDSMS\\)", "(");

            String number = inMessage.substring(inMessage.indexOf("(NUMBER") + 7, inMessage.indexOf("NUMBER)"));
            String message = inMessage.substring(inMessage.indexOf("(MESSAGE") + 8, inMessage.indexOf("MESSAGE)"));

            message = message.replaceFirst("\\(NUMBER.*NUMBER\\)", "");
            sendSMS(message, number);
        }
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
        sendResult(Constants.TIMER_CANCEL);

        sendResult(Constants.STATE_NONE);
        unregisterReceiver(intentReceiver);
        mNotificationListener = null;
        btAdapter = null;
        mSmsListener = null;
        mResultReceiver = null;
        doDestroy = true;
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

        Log.d("SERVICE", "onDestroy");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        if (mState != Constants.STATE_CONNECTED) {
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
            }, 4000);
        }
    }

    private void sendMessage() {
        messages.clear();
        scanSMS();
        scanMMS();

        if (messages.size() > 0) {
            if (!stopThread) {
                mConnectedThread.write(Constants.START_STRING);
            }
            int j = 0;
            do {

                Msg msg = messages.get(j);


                if (msg.getType().equals("SMS")) {
                    if (!stopThread) {

                        mConnectedThread.write(Constants.SMS + Constants.NUMBER_START + msg.getAddress() + Constants.NUMBER_STOP
                                + Constants.CONTACT_START + msg.getContact() + Constants.CONTACT_STOP
                                + Constants.DATE_START + msg.getDate() + Constants.DATE_STOP
                                + Constants.ID_START + msg.getID() + Constants.ID_STOP
                                + Constants.MESSAGE_START + msg.getBody() + Constants.MESSAGE_STOP
                                + Constants.READ_START + msg.getRead() + Constants.READ_STOP
                                + Constants.THREAD_START + msg.getThread() + Constants.THREAD_STOP
                                + Constants.DIRECTION_START + msg.getDirection() + Constants.DIRECTION_STOP
                                + Constants.ITEM_STOP + Constants.DELIMITER_STRING);
                        Log.d(TAG, "sendMessage: ID SMS " + msg.getID());
                    }

                } else if (msg.getType().equals("MMS")) {
                    if (!stopThread) {

                        mConnectedThread.write(Constants.MMS + Constants.NUMBER_START + msg.getAddress() + Constants.NUMBER_STOP
                                + Constants.CONTACT_START + msg.getContact() + Constants.CONTACT_STOP
                                + Constants.DATE_START + msg.getDate() + Constants.DATE_STOP
                                + Constants.ID_START + msg.getID() + Constants.ID_STOP
                                + Constants.MESSAGE_START + msg.getBody() + Constants.MESSAGE_STOP
                                + Constants.THREAD_START + msg.getThread() + Constants.THREAD_STOP
                                + Constants.DIRECTION_START + msg.getDirection() + Constants.DIRECTION_STOP
                                + Constants.READ_START + msg.getRead() + Constants.READ_STOP);
//                        Log.d(TAG, "sendMessage: ID MMS " + msg.getID());

                    }
                    if (msg.hasImg()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Bitmap bm = msg.getImg();

                        int nh = (int) (bm.getHeight() * (400.0 / bm.getWidth()));
                        Bitmap scaled = Bitmap.createScaledBitmap(bm, 400, nh, true);
                        scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        byte[] b = baos.toByteArray();
                        String img = Base64.encodeToString(b, Base64.DEFAULT);
                        if (!stopThread) {
                            mConnectedThread.write(Constants.IMAGE_START + img + Constants.IMAGE_STOP);
                        }
                    }
                    if (!stopThread) {
                        mConnectedThread.write(Constants.ITEM_STOP + Constants.DELIMITER_STRING);
                    }
//                    Log.d(TAG, "sendMessage: " + j);
                }
                j++;

            } while (messages.size() > j);
            if (!stopThread) {
                mConnectedThread.write(Constants.STOP_STRING);
            }
        }

    }


    public void scanMMS() {
        System.out.println("==============================ScanMMS()==============================");
        //Initialize Box
        Uri uri = Uri.parse("content://mms");
        String[] proj = {"*"};
        ContentResolver cr = getContentResolver();

        Cursor c = cr.query(uri, proj, "read = 0", null, null);

        if (c.moveToFirst()) {
            do {
                System.out.println("--------------------MMS------------------");
                Msg msg = new Msg(c.getString(c.getColumnIndex("_id")));
                msg.setDirection("1");
                msg.setType("MMS");
                msg.setThread(c.getString(c.getColumnIndex("thread_id")));
                msg.setDate(c.getString(c.getColumnIndex("date")));
                msg.setRead(c.getString(c.getColumnIndex("read")));
                msg.setContact(getContactName(getMmsAddr(msg.getID())));
                msg.setAddr(getMmsAddr(msg.getID()));
                parseMMS(msg);
                messages.add(msg);
            } while (c.moveToNext());
        }

        c.close();

    }


    public void parseMMS(Msg msg) {
        Uri uri = Uri.parse("content://mms/part");
        String mmsId = "mid = " + msg.getID();
        Cursor c = getContentResolver().query(uri, null, mmsId, null, null);
        while (c.moveToNext()) {

            String pid = c.getString(c.getColumnIndex("_id"));
            String type = c.getString(c.getColumnIndex("ct"));
            if ("text/plain".equals(type)) {
                msg.setBody(msg.getBody() + c.getString(c.getColumnIndex("text")));
            } else if (type.contains("image")) {
                msg.setImg(getMmsImg(pid));
            }


        }
        c.close();
        return;
    }


    public Bitmap getMmsImg(String id) {
        Uri uri = Uri.parse("content://mms/part/" + id);
        InputStream in = null;
        Bitmap bitmap = null;


        try {
            in = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return bitmap;

    }

    public String getMmsAddr(String id) {
        String sel = "msg_id=" + id;
        String uriString = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uri = Uri.parse(uriString);
        Cursor c = getContentResolver().query(uri, null, sel, null, null);
        String name = "";
        String val;
        if (c.moveToFirst()) {
            if (c.moveToFirst()) {
                do {
                    val = c.getString(c.getColumnIndex("address"));
                    if (val != null) {
                        name = val;
                        // Use the first one found if more than one
                        break;
                    }
                } while (c.moveToNext());
            }
        }
        c.close();
        return name;
    }


    public void scanSMS() {
        //Initialize Box
        Uri uri = Uri.parse("content://sms");
        String[] proj = {"*"};
        ContentResolver cr = getContentResolver();

        Cursor c = cr.query(uri, null, "read = 0", null, null);

        ArrayList<String> threads = new ArrayList<>();
        assert c != null;
        c.moveToFirst();

        Log.d(TAG, "scanSMS: " + Arrays.toString(c.getColumnNames()));
        do {
            if (threads.size() > 0 && threads.contains(c.getString(c.getColumnIndex("thread_id")))) {
            } else {
                threads.add(c.getString(c.getColumnIndex("thread_id")));
            }
            Msg msg = new Msg(c.getString(c.getColumnIndex("_id")));
            msg.setType("SMS");
            msg.setDate(c.getString(c.getColumnIndex("date")));
            msg.setThread(c.getString(c.getColumnIndex("thread_id")));
            msg.setAddr(c.getString(c.getColumnIndex("Address")));
            msg.setBody(c.getString(c.getColumnIndex("body")));
            msg.setDirection(c.getString(c.getColumnIndex("type")));
            msg.setContact(getContactName(c.getString(c.getColumnIndex("Address"))));
            msg.setRead(getContactName(c.getString(c.getColumnIndex("read"))));
            messages.add(msg);
        } while (c.moveToNext());
        c.close();

        int j = 0;

        do {
            Cursor thread = cr.query(uri, proj, "thread_id = " + threads.get(j), null, null);
            if (thread.moveToFirst()) {
                int k = 0;
                do {
                    Msg msg = new Msg(thread.getString(thread.getColumnIndex("_id")));
                    msg.setType("SMS");
                    msg.setDate(thread.getString(thread.getColumnIndex("date")));
                    msg.setThread(thread.getString(thread.getColumnIndex("thread_id")));
                    msg.setAddr(thread.getString(thread.getColumnIndex("Address")));
                    msg.setBody(thread.getString(thread.getColumnIndex("body")));
                    msg.setDirection(thread.getString(thread.getColumnIndex("type")));
                    msg.setContact(getContactName(thread.getString(thread.getColumnIndex("Address"))));
                    msg.setRead(getContactName(thread.getString(thread.getColumnIndex("read"))));
                    boolean finnsEj = false;

                    for (int l = 0; l < messages.size(); l++) {
                        finnsEj = true;
                        if (msg.equals(messages.get(l))) {
                            finnsEj = false;
//                            Log.d(TAG, "scanSMS: Meddelandet fanns redan");
                            break;
                        }

                    }

                    if (finnsEj) {
                        messages.add(msg);
//                        Log.d(TAG, "scanSMS: Meddelandet tillagt i messages Array");
                    }
                    k++;


                } while (thread.moveToNext() && k < 10);
            }


            j++;
            thread.close();

        } while (j < threads.size());


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
            }
            cursor.close();
            if (Objects.equals(contactName, "")) {
                out = phoneNumber;
            } else {
                out = contactName;
            }
        }

        return out;
    }


    private void sendSMS(String message, String number) {
        Log.d(TAG, "sendSMS: " + message);
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
        if (timesRetried <= 6 && !doDestroy && !isHandlerRunning) {
            mState = Constants.STATE_WAITING;
            sendResult(mState);
            sendResult(Constants.TIMER_10);


            mRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isRunning) {
                        checkBTState();
                        timesRetried++;
                        isHandlerRunning = false;
                        Log.d(TAG, "run: 10 sec körs");
                    }
                }
            }, 10000);
            Log.d(TAG, "doRetry, not Destroy < 3" + timesRetried);
            isHandlerRunning = true;


        } else if (timesRetried > 6 && !doDestroy && !isHandlerRunning) {
            mState = Constants.STATE_WAITING;
            sendResult(mState);
            sendResult(Constants.TIMER_60);
            Log.d(TAG, "doRetry, not Destroy > 3" + timesRetried);
            mRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isRunning) {
                        checkBTState();
                        timesRetried++;
                        isHandlerRunning = false;
                        Log.d(TAG, "run: 60 sec körs");
                    }
                }
            }, 60000);
            isHandlerRunning = true;

        } else {
            Log.d(TAG, "doRetry, Destroy " + timesRetried);

            stopSelf();
        }
    }

    @Override
    public void onNotificationReceived(String address, String subject, String message) {
        mConnectedThread.write(Constants.START_STRING + Constants.NOTIFICATION + Constants.NOTIFICATION_START
                + Constants.ADDRESS_START + address + Constants.ADDRESS_STOP
                + Constants.SUBJECT_START + subject + Constants.SUBJECT_STOP
                + Constants.MESSAGE_START + message + Constants.MESSAGE_STOP
                + Constants.NOTIFICATION_STOP + Constants.DELIMITER_STRING + Constants.STOP_STRING);
//        Log.d(TAG, "onNotificationReceived: " + address);
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            mState = Constants.STATE_CONNECTING;
            sendResult(mState);

            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
            } catch (IOException e) {
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
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    isRunning = false;
                    if (!isHandlerRunning) {
                        doRetry();
                    } else {
                        Log.d(TAG, "Handler already running");
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
                isRunning = false;
                if (!isHandlerRunning) {
                    doRetry();
                } else {
                    Log.d(TAG, "Handler körs redan");
                }
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mRetryHandler.removeCallbacksAndMessages(null);
            stopThread = false;
            isRunning = true;
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[Constants.BUFFERSIZE];
            int bytes;
            timesRetried = 0;
            mState = Constants.STATE_CONNECTED;
            sendResult(mState);
            sendMessage();
            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    isRunning = false;
                    if (!isHandlerRunning) {
                        stopThread = true;
                        doRetry();
                    } else {
                        Log.d(TAG, "Handler körs redan");
                    }
                    break;
                }
            }
        }

        //write method
        public void write(String input) {

            InputStream stream = new ByteArrayInputStream(input.getBytes());
            byte[] buffer = new byte[8192];
            int len;
            try {
                while ((len = stream.read(buffer)) != -1) {
                    mmOutStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public void write(InputStream input) {
            InputStream inputStream = input;
            byte[] buffer = new byte[8192];
            int len;
            try {
                while ((len = inputStream.read(buffer)) != -1) {
                    mmOutStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
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
