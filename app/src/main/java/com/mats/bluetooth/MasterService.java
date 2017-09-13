package com.mats.bluetooth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
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

import com.mats.bluetooth.Model.Msg;
import com.mats.bluetooth.listeners.SmsListener;
import com.mats.bluetooth.listeners.SmsListener.Listener;

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


public class MasterService extends Service implements Listener {

    //    private final IBinder mBinder = new LocalBinder();
    private ArrayList<Msg> messages;
    private Constants mConstants;
    final int handlerState = 0;                        //used to identify handler message
    private Handler bluetoothIn;
    private int mState = Constants.STATE_NONE;
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

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {

            if ("REGISTER_RECEIVER".equals(intent.getAction())) {
                Log.d("BT SERVICE", "REGISTERING RECEIVER");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                sendResult(mState);

            } else if ("UNREGISTER_RECEIVER".equals(intent.getAction())) {

                mResultReceiver = null;
            } else if ("FIRST_START".equals(intent.getAction())) {

                Log.d("BT SERVICE", "SERVICE STARTED");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                MAC_ADDRESS = intent.getExtras().getString("mac_address");
                sendResult(mState);
                init();
            }




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
        messages = new ArrayList<>();
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
            }, 2000);
        }
    }

/*
    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }


    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static InputStream bitmapToInputStream(Bitmap bitmap) {
        int size = bitmap.getHeight() * bitmap.getRowBytes();
        Log.d(TAG, "bitmapToInputStream: " + size);
        ByteBuffer buffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(buffer);
        return new ByteArrayInputStream(buffer.array());
    }
*/


    private void sendMessage() {
        scanMMS();
        scanSMS();

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
                        Log.d(TAG, "sendMessage: ID MMS " + msg.getID());

                    }
                    if (msg.hasImg()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Bitmap bm = msg.getImg();
//                        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.tiger);

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
                    Log.d(TAG, "sendMessage: " + j);
                }
                j++;

            } while (messages.size() > j);
            if (!stopThread) {
                mConnectedThread.write(Constants.STOP_STRING);
            }
        }

    }

///////////////////////////////////////////////////////////////////////////////////////////////////


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


                msg.setType("MMS");
                msg.setThread(c.getString(c.getColumnIndex("thread_id")));
                msg.setDate(c.getString(c.getColumnIndex("date")));
                msg.setRead(c.getString(c.getColumnIndex("read")));
                Log.d(TAG, "scanMMS: Read? " + msg.getRead());
                msg.setContact(getContactName(getMmsAddr(msg.getID())));
                msg.setAddr(getMmsAddr(msg.getID()));
                parseMMS(msg);
//                System.out.println(msg.toString());
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
        String sel = new String("msg_id=" + id);
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
//        Log.d(TAG, "getMmsAddr: " + name);
        return name;
    }


    public void scanSMS() {
        System.out.println("==============================ScanSMS()==============================");
        //Initialize Box
        Uri uri = Uri.parse("content://sms");
        String[] proj = {"*"};
        ContentResolver cr = getContentResolver();

        Cursor c = cr.query(uri, proj, "read = 0", null, null);

        ArrayList<String> threads = new ArrayList<>();

        if (c.moveToFirst()) {

            Log.d(TAG, "scanSMS: " + Arrays.toString(c.getColumnNames()));
            do {
                System.out.println("--------------------SMS------------------");
                if (!threads.contains(c.getString(c.getColumnIndex("thread_id")))) {
                    threads.add(c.getString(c.getColumnIndex("thread_id")));
                } else {
                    Log.d(TAG, "scanSMS: ThreadId fanns redan i arrayen ");
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

            } while (c.moveToNext());


            int j = 0;

            do {
                Cursor thread = cr.query(uri, proj, "thread_id = " + threads.get(j), null, null);
                if (thread.moveToFirst()) {
                    int k = 0;
                    do {
                        boolean finnsEj = false;
                        Msg msg = new Msg(thread.getString(thread.getColumnIndex("_id")));
                        msg.setType("SMS");
                        msg.setDate(thread.getString(thread.getColumnIndex("date")));
                        msg.setThread(thread.getString(thread.getColumnIndex("thread_id")));
                        msg.setAddr(thread.getString(thread.getColumnIndex("Address")));
                        msg.setBody(thread.getString(thread.getColumnIndex("body")));
                        msg.setDirection(thread.getString(thread.getColumnIndex("type")));
                        msg.setContact(getContactName(thread.getString(c.getColumnIndex("Address"))));
                        msg.setRead(getContactName(thread.getString(c.getColumnIndex("read"))));
//                        Log.d(TAG, "scanSMS: Read? " + msg.getRead());

                        for (int l = 0; l < messages.size(); l++) {
                            if (msg.equals(messages.get(l))) {
                                Log.d(TAG, "scanSMS: Meddelandet fanns redan");
                                break;
                            } else {
                                finnsEj = true;

                                Log.d(TAG, "scanSMS: Meddelandet tillagt i messages Array");
                                Log.d(TAG, "scanSMS: I storlek " + j + " " + k + " " + messages.size());
                            }

                        }
                        if (finnsEj){
                            messages.add(msg);
                        }

                        k++;


                    } while (thread.moveToNext() && k < 10);
                }


                j++;
                thread.close();

            } while (j < threads.size());


        }
        c.close();

/*



        Cursor c = cr.query(uri, proj, "thread ", null, null);




        if (c.moveToFirst()) {
            do {
                String[] col = c.getColumnNames();
                String str = "";
                for (int i = 0; i < col.length; i++) {
                    str = str + col[i] + ": " + c.getString(i) + ", ";
                }
                //System.out.println(str);

                System.out.println("--------------------SMS------------------");

                Msg msg = new Msg(c.getString(c.getColumnIndex("_id")));
                msg.setType("SMS");
                msg.setDate(c.getString(c.getColumnIndex("date")));
                msg.setAddr(c.getString(c.getColumnIndex("Address")));
                msg.setBody(c.getString(c.getColumnIndex("body")));
                msg.setDirection(c.getString(c.getColumnIndex("type")));
                msg.setContact(getContactName(c.getString(c.getColumnIndex("Address"))));
//                System.out.println(msg);
                messages.add(msg);

            } while (c.moveToNext());
        }
        c.close();


*/


    }


///////////////////////////////////////////////////////////////////////////////////////////////////


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
            if (Objects.equals(contactName, "")) {
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
        Log.d(TAG, "sendSMS: Skickar till!" + number);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);

    }

    private void doRetry() {
        mState = Constants.STATE_NONE;
        sendResult(mState);
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }
        if (timesRetried <= 3 && !doDestroy && !isHandlerRunning) {
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


        } else if (timesRetried > 3 && !doDestroy && !isHandlerRunning) {

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
//        }
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
//                sendMessage();
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
                    isRunning = false;
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
                isRunning = false;
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
            timesRetried = 0;
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
                    Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
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
                        Log.d(TAG, "run3: not retry");
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
