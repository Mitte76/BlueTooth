package com.mats.bluetooth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Base64;
import android.util.Log;

import com.mats.bluetooth.DbHelper.Database;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by mats on 2017-08-26.
 */

public class SlaveService extends Service {

    private Database dbHelper;
    private ResultReceiver mResultReceiver;

    private ArrayList<String> messageArrayList;

    final int handlerState = 0;                        //used to identify handler message
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private static final String TAG = "SlaveService";
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;
    public Boolean isRunning, doDestroy = false;
    private String fullString = "";
    private int mState;
    private boolean stopThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private StringBuilder recDataString = new StringBuilder();
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICE CREATED");
        stopThread = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {

            if ("REGISTER_RECEIVER".equals(intent.getAction())) {
                Log.d("BT SERVICE", "REGISTERING RECEIVER");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                sendResult(2);

            } else if ("UNREGISTER_RECEIVER".equals(intent.getAction())) {
                // Extract the ResultReceiver ID and remove it from the map

                mResultReceiver = null;
            } else if ("SEND_MESSAGE".equals(intent.getAction())) {
                String message = intent.getStringExtra("MESSAGE_TEXT");
                String number = intent.getStringExtra("MESSAGE_NUMBER");
                sendMessage(message, number, null, "SMS");

            } else if ("MARK_READ".equals(intent.getAction())) {
                String id = intent.getStringExtra("MESSAGE_ID");
                String message = intent.getStringExtra("MESSAGE_TEXT");
                String number = intent.getStringExtra("MESSAGE_NUMBER");

                sendMessage(message, number, id, "MARK_READ");

            } else if ("FIRST_START".equals(intent.getAction())) {

                Log.d("BT SERVICE", "SERVICE STARTED");
                ResultReceiver receiver = intent.getParcelableExtra("ResultReceiver");
                mResultReceiver = receiver;
                sendResult(3);
                init();
            }
        } else {
            Log.d(TAG, "onStartCommand: no resultreceiver for some reason");
        }

        return START_STICKY;

//        return super.onStartCommand(intent, flags, startId);
    }

    private void init() {

        Intent notificationIntent = new Intent(this, SmsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "Android O")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("BT Slave")
                .setContentText("Waiting for stuff to do.")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter

        isRunning = true;/*
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                 //if message is what we want
                    String readMessage = (String) msg.obj;      // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);
                    // Do stuff here with your data, like adding it to the database
                }

                if (recDataString.toString().contains("[END12:12:12]")){
                    Log.d(TAG, "handleMessage: Slut: " + recDataString.toString());
                    sortMessage();
                } else {

                }


//                if (recDataString.toString().contains("[END12:12:12]") && smallMessage) {
//                    Log.d(TAG, "handleMessage: " + recDataString);
//                    sortMessage(recDataString.toString());
//                    recDataString.delete(0, recDataString.length());                    //clear all string data
//                    Log.d(TAG, "handleMessage: " + fullMsg.toString());
//
//                } else if (recDataString.toString().contains("[END12:12:12]") && !smallMessage){
//                    sortMessage(fullMsg.toString());
//                    Log.d(TAG, "handleMessage: " + fullMsg.toString());
//
//                } else {
//                    smallMessage = false;
//                    fullMsg.append(recDataString.toString()) ;
//                }

            }
        };
*/
        startListening();
        messageArrayList = new ArrayList<String>();
        dbHelper = Database.getInstance(getApplicationContext());


    }

    private void assembleBTMessage(String message) {

        Log.d(TAG, "assembleBTMessage: Kommer hit" + message);
        boolean test = false;

//        Log.d(TAG, "assembleBTMessage: " + message);
        if (message.contains(Constants.START_STRING) && message.contains(Constants.DELIMITER_STRING)) {
            dbHelper.prepareSms();
            Log.d(TAG, "assembleBTMessage: " + message);

            List<String> stuff = new ArrayList<>(Arrays.asList(message.split(Constants.DELIMITER_STRING)));
            Log.d(TAG, "assembleBTMessage: " + stuff.size());
            int i = 0;
            do {
                Log.d(TAG, "assembleBTMessage: Stuff !!!!!!!!!!!" + stuff.get(i));
                i++;
            } while (stuff.size() > i);

            fullString = stuff.get(stuff.size() - 1);


            Log.d(TAG, "assembleBTMessage: Start String");
        } else if (message.contains(Constants.STOP_STRING)) {
//            fullString = fullString + message;
            Log.d(TAG, "assembleBTMessage: Stop String " + fullString);
            fullString = fullString + message.substring(0, message.indexOf(Constants.DELIMITER_STRING));
            sortMessage(fullString);
            fullString = "";

            dbHelper.deleteSms();
        } else if (message.contains(Constants.DELIMITER_STRING) && !message.contains(Constants.START_STRING)) {
            Log.d(TAG, "assembleBTMessage: Delimiter String ");

            fullString = fullString + message.substring(0, message.indexOf(Constants.DELIMITER_STRING));
            sortMessage(fullString);
            fullString = "";
        }  else {
            Log.d(TAG, "assembleBTMessage: ELSE !!!!");
            fullString = fullString + message;

            if (fullString.substring(fullString.length() - Constants.DELIMITER_STRING.length(), fullString.length()).contains(Constants.DELIMITER_STRING)) {
                sortMessage(fullString);
                fullString = "";
//                Log.d(TAG, "assembleBTMessage: Built Delimiter String");
            } else if (fullString.substring(fullString.length() - Constants.STOP_STRING.length(), fullString.length()).contains(Constants.STOP_STRING)) {
                dbHelper.deleteSms();
                sortMessage(fullString);
                fullString = "";
                 Log.d(TAG, "assembleBTMessage: Empty");
            }



/*            Log.d(TAG, "assembleBTMessage: innan " + fullString.length());
            fullString = fullString + message;
            Log.d(TAG, "assembleBTMessage: efter " + fullString.length());
            if (fullString.substring(fullString.length() - Constants.STOP_STRING.length(),fullString.length()).contains(Constants.STOP_STRING)){
                sortMessage(fullString);
                Log.d(TAG, "assembleBTMessage: post " + fullString.length());
                dbHelper.deleteSms();
                fullString = "";
            } else  if (fullString.substring(fullString.length() - Constants.DELIMITER_STRING.length(),fullString.length()).contains(Constants.DELIMITER_STRING)){
                sortMessage(fullString);
                fullString = "";
            }*/

        }


//        if (test) {
//            int i = 0;
//            do {
//                message = message + messageArrayList.get(i);
//                i++;
//            } while (messageArrayList.size() != i);
//
//            sortMessage(fullString);
//            fullString = "";
//
//            messageArrayList.clear();
//
//        }

    }

    private void sendMessage(String message, String number, String id, String action) {

        if (action == "MARK_READ") {
            mConnectedThread.write("(MRKRED)" + "(NUMBER" + number + "NUMBER)" + "(" + id + "ID)" + "(" + message + "MESSAGE)");

        } else if (action == "SMS") {
            mConnectedThread.write("(SNDSMS)" + "(NUMBER" + number + "NUMBER)" + "(MESSAGE" + message + "MESSAGE)");

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        doDestroy = true;

//        Log.d(TAG, "onDestroy: " + btAdapter.getScanMode());
//        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }


        btAdapter = null;
        Log.d("SERVICE", "onDestroy");

        stopSelf();
    }

    private void sendResult(int number) {
        if (mResultReceiver != null)
            mResultReceiver.send(number, null);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void addToDb(String name, String number, String message, String time) {
        dbHelper.addSMS(name, number, message, time);
    }

    private void sortMessage(String inMessage) {
//        inMessage = inMessage.replaceFirst("\\[END121212\\]", "");
//        Log.d(TAG, "sortMessage: in " + inMessage);
        Bitmap bitmap = null;
//        for (int i = 0; i < messageList.size(); i++) {
////            Log.d("string is",(String) messageList.get(i));
//            message = message + messageList.get(i);
//        }
//        Log.d(TAG, "sortMessage: " + messageList);

//        String message = fullMsg.toString();
//        Log.d(TAG, "sortMessage: full message: " + message);
//        List<String> sms = new ArrayList<>(Arrays.asList(inMessage.split("\\[SMS\\]")));
//        List<String> img = new ArrayList<>(Arrays.asList(inMessage.split("\\[IMG\\]")));
//        sms.remove(0);
//        img.remove(0);
//        if (sms.size() > 0) {
//            dbHelper.prepareSms();
//        }
//        Log.d(TAG, "sortMessage: " + img.size());
        if (inMessage.substring(0, 5).contains(Constants.IMG)) {
//            String image = img.get(0);
//            inMessage = inMessage.replaceFirst(Constants.DELIMITER_STRING, "");
            String image = inMessage.substring(inMessage.indexOf(Constants.IMG) + 5, inMessage.length());

            Log.d(TAG, "sortMessage: Bild medskickad" + image.length());

            try {
//                String tmp = img.get(0);
                byte[] encodeByte = Base64.decode(image, Base64.DEFAULT);
                bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            } catch (Exception e) {
                e.getMessage();
            }

        } else {
            Log.d(TAG, "sortMessage: substring " + inMessage.substring(0, 5) + " " + inMessage.length());
        }
        if (inMessage.substring(0, 5).contains(Constants.SMS)) {
//            Log.d(TAG, "handleMessage: Size" + sms.size());
//            if (j != (sms.size() - 1)) {
//                inMessage = sms.get(j).substring(0, sms.get(j).length() - 2);
//            } else {
//                inMessage = sms.get(j).substring(0, sms.get(j).length() - 1);
//            }
//            Log.d(TAG, "sortMessage: FullMessage " + inMessage);
            String number = inMessage.substring(inMessage.indexOf("(NUMBER") + 7, inMessage.indexOf("NUMBER)"));
//            Log.d(TAG, "sortMessage: Number " + number);
            String name = inMessage.substring(inMessage.indexOf("(USER") + 5, inMessage.indexOf("USER)"));
//            Log.d(TAG, "sortMessage: Name " + name);
            String date = inMessage.substring(inMessage.indexOf("(DATE") + 5, inMessage.indexOf("DATE)"));
//            Log.d(TAG, "sortMessage: Date " + date);
            String id = inMessage.substring(inMessage.indexOf("(ID") + 3, inMessage.indexOf("ID)"));
//            Log.d(TAG, "sortMessage: Id " + id);
            String message = inMessage.substring(inMessage.indexOf("(MESSAGE") + 8, inMessage.indexOf("MESSAGE)"));
//            Log.d(TAG, "sortMessage: Message " + message);


////            String number = message.substring(2, message.indexOf("|)"));
////            Log.d(TAG, "sortMessage: Number: " + number);
//            message = message.replaceFirst("\\(\\|(.*)\\|\\)", "");
//
//
////            String name = message.substring(1, message.indexOf("user)"));
//            //                Log.d(TAG, "sortMessage: Name: " + name);
//            message = message.replaceFirst("\\(USER.*USER\\)", "");
//
//            String time = message.substring(1, message.indexOf("date)"));
//            message = message.replaceFirst("\\(DATE.*DATE\\)", "");
//
//            String id = message.substring(1, message.indexOf("id)"));
//            message = message.replaceFirst("\\(ID.*ID\\)", "");
//

//            Log.d(TAG, "sortMessage: id: " + id);


//            message = message.replaceFirst("\\(.*\\)", "");
//                Log.d(TAG, "sortMessage: Message: " + message);
            addToDb(name, number, message, date);


//                String number = message.substring(2, message.indexOf("|)"));
////                Log.d(TAG, "sortMessage: Number: " + number);
//                message = message.replaceFirst("\\(\\|(.*)\\|\\)", "");
//                String name = message.substring(1, message.indexOf(")"));
////                Log.d(TAG, "sortMessage: Name: " + name);
//                message = message.replaceFirst("\\(.*\\)\\(", "\\(");
//                String time = message.substring(1, message.indexOf(")"));
////                Log.d(TAG, "sortMessage: time: " + time);
//                message = message.replaceFirst("\\(.*\\)", "");
////                Log.d(TAG, "sortMessage: Message: " + message);
//                addToDb(name, number, message, time);


        }
        Intent intent = new Intent(getApplicationContext(), SmsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction("REFRESH");

        if (bitmap != null) {
            ByteArrayOutputStream _bs = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, _bs);
            intent.putExtra("byteArray", _bs.toByteArray());
        }

//
//        img.clear();
//        sms.clear();

        startActivity(intent);

        // Lägg till intent till slaveactivity igen.
        // Kolla om appen är igång annars starta.
    }

    private void startListening() {

        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }
        stopThread = false;
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
            Log.d(TAG, "onStartCommand: STARTED");
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();


    }


/*    // New Class for Connecting Thread
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
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                mConnectedThread.write("x");
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
                    stopSelf();
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
    }*/


    public static Handler myHandler = new Handler();
    boolean test = false;

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
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            final byte[] buffer = new byte[Constants.BUFFERSIZE];
            int byteNo;
//            int byteNo2 = 0;
            // Keep looping to listen for received messages





            while (!stopThread) {
                try {




                    byteNo = mmInStream.read(buffer);
//                    if (byteNo != -1) {
//                        while (byteNo2 <= Constants.BUFFERSIZE - 1000) {
//                            byteNo2 = byteNo2 + byteNo;
//
//                        }
//                        String readMessage = new String(buffer, 0, byteNo2);
//                        Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage.length());
//                        // Send the obtained bytes to the UI Activity via handler
//                        assembleBTMessage(readMessage);
//                        byteNo2 = 0;
//
//                    }

                    Runnable myRunnable = null;
                    if (byteNo != -1) {
                        //ensure DATAMAXSIZE Byte is read.
                        int byteNo2 = byteNo;
                        int bufferSize = 30000;
                        while (byteNo2 != bufferSize) {


//                        while (byteNo2 != bufferSize) {
                            bufferSize = bufferSize - byteNo2;
                            Log.d(TAG, "run: Buffer " + buffer + " Byte " + byteNo + " BufferSize " + bufferSize);

                            byteNo2 = mmInStream.read(buffer, byteNo, bufferSize);


                            byteNo = byteNo + byteNo2;
                            final int byteNo3 = byteNo;
                            Log.d(TAG, "run: ");
                            Log.d(TAG, "run: Byte2 " + byteNo2 + " Byte " + byteNo + " BufferSize " + bufferSize);
                            myHandler.removeCallbacksAndMessages(null);

                            myRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    String readMessage = new String(buffer, 0, byteNo3);
                                    assembleBTMessage(readMessage);

                                    Log.d(TAG, "run: Handler körts");
                                }
                            };
                            myHandler.postDelayed(myRunnable, 700);

                        }

                    }
//                    myHandler.removeCallbacksAndMessages(null);

                    String readMessage = new String(buffer, 0, byteNo);
//                    Log.d(TAG, "run: " + readMessage);
                    assembleBTMessage(readMessage);


                    //read bytes from input buffer

//                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    if (!doDestroy) {
                        stopThread = true;
                        startListening();
                    }
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
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
                stopSelf();
            }
        }
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private final String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("MasterService", BTMODULEUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;

        }

        public void run() {
//            Log.d(TAG, "Socket Type: " + mSocketType +
//                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (SlaveService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
//            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
//            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


}
