package com.mats.bluetooth;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import com.mats.bluetooth.DbHelper.Database;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SlaveService extends Service {
    private Database dbHelper;

    private ResultReceiver mResultReceiver;

    private BluetoothAdapter btAdapter = null;
    private static final String TAG = "SlaveService";
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;
    public Boolean doDestroy = false;
    private String fullString = "";
    private int mState;
    private boolean stopThread;
    private static final UUID BTMODULEUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int turns;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICE CREATED");
        stopThread = false;
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        dbHelper = Database.getInstance(getApplicationContext());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {

            if ("REGISTER_RECEIVER".equals(intent.getAction())) {
                mResultReceiver = intent.getParcelableExtra("ResultReceiver");
                sendResult(mState);
                Log.d("BT SERVICE", "REGISTERING RECEIVER");

            } else if ("UNREGISTER_RECEIVER".equals(intent.getAction())) {
                mResultReceiver = null;
            }else if ("SEND_MESSAGE".equals(intent.getAction())) {
                String message = intent.getStringExtra("MESSAGE_TEXT");
                String number = intent.getStringExtra("MESSAGE_NUMBER");
                sendMessage(message, number, null, "SMS");

            } else if ("MARK_READ".equals(intent.getAction())) {
                String id = intent.getStringExtra("MESSAGE_ID");
                String message = intent.getStringExtra("MESSAGE_TEXT");
                String number = intent.getStringExtra("MESSAGE_NUMBER");
                sendMessage(message, number, id, "MARK_READ");

            } else if ("FIRST_START".equals(intent.getAction())) {
                mState = STATE_NONE;
                mResultReceiver = intent.getParcelableExtra("ResultReceiver");
                sendResult(mState);
                Log.d("BT SERVICE", "SERVICE STARTED");
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

        /*isRunning = true;
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


    }
    private void sendResult(int number) {
        if (mResultReceiver != null)
            mResultReceiver.send(number, null);
    }

    private void assembleBTMessage(String message) {

        fullString = fullString + message;

        if (fullString.contains(Constants.START_STRING) && fullString.contains(Constants.STOP_STRING)) {
            dbHelper.prepareSms();
            fullString = fullString.replace(Constants.START_STRING, "");
            List<String> stuff = new ArrayList<>(Arrays.asList(fullString.split(Constants.DELIMITER_STRING)));
            if (stuff.size() > 0) {
                int i = 0;
                Log.d(TAG, "assembleBTMessage: Antal mottagna = " + stuff.size());

                do {
                    sortMessage(stuff.get(i));
                    Log.d(TAG, "assembleBTMessage: Början på paketen = " + stuff.get(i));

                    i++;
                } while (i < stuff.size());
            }
            fullString = "";
            dbHelper.deleteSms();
        }


/*


        turns++;
        boolean test = false;

        if (message.contains(Constants.START_STRING)) {
            dbHelper.prepareSms();
            fullString = "";
            message = message.substring(Constants.START_STRING.length(), message.length()); //Tar bort startSträng
            Log.d(TAG, "assembleBTMessage: Innehåller startSträng" + " turns " + turns);
            if (message.contains(Constants.STOP_STRING)) {
                Log.d(TAG, "assembleBTMessage: Innehåller stopSträng" + " turns " + turns);
                //Det här görs om Strängen är komplett
                List<String> stuff = new ArrayList<>(Arrays.asList(message.split(Constants.DELIMITER_STRING)));
                if (stuff.size() > 0) {
                    int i = 0;
                    do {
                        sortMessage(stuff.get(i));
                        i++;
                    } while (stuff.size() > i);
                }
                dbHelper.deleteSms();

            } else if (message.contains(Constants.DELIMITER_STRING)) {
                Log.d(TAG, "assembleBTMessage: Innehåller delimiterSträng" + " turns " + turns);
                //Det här görs om vissa delar finns med
                List<String> stuff = new ArrayList<>(Arrays.asList(message.split(Constants.DELIMITER_STRING)));
                int pkgToSend = stuff.size() - 1; // - 1 för att jag ska göra "get" på en array som börjar på 0
                String lastitem = stuff.get(pkgToSend);
                if (lastitem.substring((lastitem.length() - Constants.ITEM_STOP.length()), lastitem.length()).equals(Constants.ITEM_STOP)) {
                    // Kolla om sista delen är komplett.
                    Log.d(TAG, "assembleBTMessage: Start + Delimiter + Itemstop " + lastitem.substring((lastitem.length() - Constants.ITEM_STOP.length()), lastitem.length()));
                } else {
                    pkgToSend = pkgToSend - 1;
                    //Om inte sista delen är komplett lägg in i fullstring och vänta på mer data.
                    Log.d(TAG, "assembleBTMessage: Start + Delimiter inte Itemstop " + lastitem.substring((lastitem.length() - Constants.ITEM_STOP.length()), lastitem.length()));
                    fullString = stuff.get(stuff.size() - 1);
                }
                Log.d(TAG, "assembleBTMessage: Start + Delimiter. Antal paket: " + stuff.size() + " Antal kompletta: " + (pkgToSend + 1));
                int i = 0;
                do {
                    sortMessage(stuff.get(i));
                    i++;
                    Log.d(TAG, "assembleBTMessage: " + i);
                } while (i <= pkgToSend);
            } else {
                Log.d(TAG, "assembleBTMessage: Start men inget komplett " + " turns " + turns);
                //Det här görs om inga kompletta delar finns med
                fullString = message;
            }


        } else if (message.contains(Constants.STOP_STRING)) {
            // Det här görs om meddelandet har delats upp på flera buffers och sista paketet har kommit.
//            Log.d(TAG, "assembleBTMessage: vad innehåller fullstring? " + fullString.substring(fullString.length() - 400, fullString.length()));
//            Log.d(TAG, "assembleBTMessage: vad innehåller message? " + message.substring(0, 600));
            fullString = fullString + message.substring(0, message.indexOf(Constants.STOP_STRING));
//            Log.d(TAG, "assembleBTMessage: vad innehåller message slutet? " + message.substring(message.length() - 200, message.length()));

            List<String> stuff = new ArrayList<>(Arrays.asList(fullString.split(Constants.DELIMITER_STRING)));
//            Log.d(TAG, "assembleBTMessage: Innehåller stopstring. Antal paket: " + stuff.size() + " turns " + turns);
            if (stuff.size() > 0) {
                int i = 0;
                do {
                    sortMessage(stuff.get(i));
//                    Log.d(TAG, "assembleBTMessage: Innehåller stopstring. Slutet på paketen = " + stuff.get(i).substring(stuff.get(i).length() - 40, stuff.get(i).length()));
//                    Log.d(TAG, "assembleBTMessage: Innehåller stopstring. Början på paketen = " + stuff.get(i).substring(0, 40));

                    i++;
                } while (i < stuff.size());
            }
            fullString = "";
            dbHelper.deleteSms();

        } else if (fullString.contains(Constants.DELIMITER_STRING)) {
            test = true;

            List<String> stuff = new ArrayList<>(Arrays.asList(fullString.split(Constants.DELIMITER_STRING)));
            int pkgToSend = stuff.size() - 1; // - 1 för att jag ska göra "get" på array som börjar på 0

            String lastItem = stuff.get(pkgToSend);
            Log.d(TAG, "assembleBTMessage: Delimiter sista paket längd" + lastItem.length());
            if (lastItem.length() >= Constants.ITEM_STOP.length()) {
                if (lastItem.substring((lastItem.length() - Constants.ITEM_STOP.length()), lastItem.length()).equals(Constants.ITEM_STOP)) {
                    // Kolla om sista delen är komplett.
                    Log.d(TAG, "assembleBTMessage: Verkar funkar Item Stop (IF) Fullstring " + lastItem.substring((lastItem.length() - Constants.ITEM_STOP.length()), lastItem.length()) + " turns " + turns);
                    fullString = "";
                } else {
                    pkgToSend = pkgToSend - 1;
                    //Om inte sista delen är komplett lägg in i fullstring och vänta på mer data.
//                    Log.d(TAG, "assembleBTMessage: fullstring + Delimiter Tot antal paket: " + stuff.size() + " kompletta paket: " + (pkgToSend + 1) + " Sista tecknen i sista paketet: " + lastItem.substring((lastItem.length() - Constants.ITEM_STOP.length()), lastItem.length()) + " turns " + turns);
//                    Log.d(TAG, "assembleBTMessage: fullstring + Delimiter Tot antal paket: " + stuff.size() + " kompletta paket: " + (pkgToSend + 1) + " Första tecknen i sista paketet: " + lastItem.substring(0, 140));
                    fullString = stuff.get(stuff.size() - 1);
//                    Log.d(TAG, "assembleBTMessage: stuff sista Början: " + stuff.get(stuff.size() - 1).substring(0, 200));
//                    Log.d(TAG, "assembleBTMessage: stuff sista Slutet: " + stuff.get(stuff.size() - 1).substring(stuff.get(stuff.size() - 1).length() - 200, stuff.get(stuff.size() - 1).length()));

                }
            } else {
                Log.d(TAG, "assembleBTMessage: " + lastItem.length());
                pkgToSend = pkgToSend - 1;
                fullString = stuff.get(stuff.size() - 1);
            }
            int i = 0;
            do {
                sortMessage(stuff.get(i));
//                Log.d(TAG, "assembleBTMessage: gånger " + i);
//                Log.d(TAG, "assembleBTMessage: Fullstring Delimiter första 50 " + stuff.get(i).substring(0, 100) + " Turn " + turns);
//                Log.d(TAG, "assembleBTMessage: Fullstring Delimiter Storlek " + stuff.size());
//                Log.d(TAG, "assembleBTMessage: Fullstring Delimiter Sista 25 " + stuff.get(i).substring(stuff.get(i).length() - 25, stuff.get(i).length()));
                i++;
            } while (i < pkgToSend);


        } else {
            fullString = fullString + message;
            Log.d(TAG, "assembleBTMessage: ELSE !!!! Meddelande med bara rå data " + " turns " + turns);
        }
*/


    }

    private void sendMessage(String message, String number, String id, String action) {

        if (Objects.equals(action, "MARK_READ")) {
            mConnectedThread.write("(MRKRED)" + "(NUMBER" + number + "NUMBER)" + "(" + id + "ID)" + "(" + message + "MESSAGE)");

        } else if (Objects.equals(action, "SMS")) {
            mConnectedThread.write("(SNDSMS)" + "(NUMBER" + number + "NUMBER)" + "(MESSAGE" + message + "MESSAGE)");

        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mState = STATE_NONE;
        sendResult(mState);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sortMessage(String inMessage) {

        if ((inMessage.substring(0, 5).contains(Constants.MMS) || (inMessage.substring(0, 5).contains(Constants.SMS)))) {
            String type = "SMS";
            if (inMessage.substring(0, 5).contains(Constants.MMS)) {
                type = "MMS";
            }
            Log.d(TAG, "sortMessage: " + inMessage);
            String number = inMessage.substring(inMessage.indexOf(Constants.NUMBER_START) + Constants.NUMBER_START.length(), inMessage.indexOf(Constants.NUMBER_STOP));
//            Log.d(TAG, "sortMessage: Number " + number);
            String name = inMessage.substring(inMessage.indexOf(Constants.CONTACT_START) + Constants.CONTACT_START.length(), inMessage.indexOf(Constants.CONTACT_STOP));
//            Log.d(TAG, "sortMessage: Name " + name);
            String date = inMessage.substring(inMessage.indexOf(Constants.DATE_START) + Constants.DATE_START.length(), inMessage.indexOf(Constants.DATE_STOP));
//            Log.d(TAG, "sortMessage: Date " + date);
            String id = type + inMessage.substring(inMessage.indexOf(Constants.ID_START) + Constants.ID_START.length(), inMessage.indexOf(Constants.ID_STOP));
//            Log.d(TAG, "sortMessage: Id " + id);
            String message = inMessage.substring(inMessage.indexOf(Constants.MESSAGE_START) + Constants.MESSAGE_START.length(), inMessage.indexOf(Constants.MESSAGE_STOP));
//            Log.d(TAG, "sortMessage: Message " + message);
            String read = inMessage.substring(inMessage.indexOf(Constants.READ_START) + Constants.READ_START.length(), inMessage.indexOf(Constants.READ_STOP));

            String thread = inMessage.substring(inMessage.indexOf(Constants.THREAD_START) + Constants.THREAD_START.length(), inMessage.indexOf(Constants.THREAD_STOP));

            String direction = inMessage.substring(inMessage.indexOf(Constants.DIRECTION_START) + Constants.DIRECTION_START.length(), inMessage.indexOf(Constants.DIRECTION_STOP));


            if (inMessage.contains(Constants.IMAGE_STOP)) {
                Log.d(TAG, "sortMessage: " + inMessage.substring(inMessage.indexOf(Constants.IMAGE_STOP) - 50, inMessage.indexOf(Constants.IMAGE_STOP)));
                String image = inMessage.substring(inMessage.indexOf(Constants.IMAGE_START) + Constants.IMAGE_START.length(), inMessage.indexOf(Constants.IMAGE_STOP));
                Log.d(TAG, "sortMessage: Bild medskickad" + image.length());
                Log.d(TAG, "sortMessage: ID " + id);
                dbHelper.addSMS(name, number, message, date, id, read, thread, direction, image);
            } else {
                dbHelper.addSMS(name, number, message, date, id, read, thread, direction);

            }

            Intent intent = new Intent(getApplicationContext(), SlaveActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setAction("REFRESH");
            startActivity(intent);
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






    private void startListening() {

            sendResult(mState);

        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread = null;
            Log.d(TAG, "startListening: mConnectedThread Closed ");
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

    int totalBytes;
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
//                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            sendResult(mState);
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

/*

                    if (byteNo != -1) {
                        //ensure DATAMAXSIZE Byte is read.
                        int byteNo2 = byteNo;
                        int bufferSize = Constants.BUFFERSIZE;
                        while (byteNo2 != bufferSize) {


//                        while (byteNo2 != bufferSize) {
                            bufferSize = bufferSize - byteNo2;
//                            Log.d(TAG, "run: Buffer " + buffer + " Byte " + byteNo + " BufferSize " + bufferSize);

                            byteNo2 = mmInStream.read(buffer, byteNo, bufferSize);
                            if(byteNo2 == -1){
                                break;
                            }

                            byteNo = byteNo + byteNo2;
//                            Log.d(TAG, "run: " + byteNo);
                            final int byteNo3 = byteNo;
//                            Log.d(TAG, "run: ");
//                            Log.d(TAG, "run: Byte2 " + byteNo2 + " Byte " + byteNo + " BufferSize " + bufferSize);
    */
/*                        myHandler.removeCallbacksAndMessages(null);

                            myRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    String readMessage = new String(buffer, 0, byteNo3);
                                    assembleBTMessage(readMessage);

                                    Log.d(TAG, "run: Handler körts");
                                }
                            };
                            myHandler.postDelayed(myRunnable, 100);*//*


                        }

                    }
//                    myHandler.removeCallbacksAndMessages(null);
*/

                    String readMessage = new String(buffer, 0, byteNo);
//                    Log.d(TAG, "run: " + readMessage);
                    assembleBTMessage(readMessage);
                    totalBytes = totalBytes + readMessage.length();

                    Log.d(TAG, "run: " + totalBytes);
                    //read bytes from input buffer

//                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    if (!doDestroy) {
                        mState = STATE_NONE;
                        sendResult(mState);
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
//                stopSelf();
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
//                stopSelf();
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

            Log.d(TAG, "AcceptThread: " + btAdapter.toString());

            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("SlaveService", BTMODULEUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
            sendResult(mState);

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
