package com.mats.bluetooth;

/**
 * Created by mats on 2017-09-07.
 */

public class Constants {

    public static final String START_STRING = "//START121212//";
    public static final String DELIMITER_STRING = "//DELI121212//";
    public static final String STOP_STRING = "//STOP121212//";

//    public static final String SUB_START = "|STOP121212|";
//    public static final String SUB_STOP = "|STOP121212|";

    public static final String SMS = "[SMS]";
    public static final String MMS = "[MMS]";
    public static final String NOTIFICATION = "[NOT]";
    public static final String ITEM_STOP = "[STOP]";
    public static final String NUMBER_START = "//NUMBER";
    public static final String NUMBER_STOP = "NUMBER//";
    public static final String CONTACT_START = "//CONTACT";
    public static final String CONTACT_STOP = "CONTACT//";
    public static final String DATE_START = "//DATE";
    public static final String DATE_STOP = "DATE//";
    public static final String ID_START = "//IDIN";
    public static final String ID_STOP = "IDIN//";
    public static final String MESSAGE_START = "//MESSAGE";
    public static final String MESSAGE_STOP = "MESSAGE//";
    public static final String IMAGE_START = "//IMAGE";
    public static final String IMAGE_STOP = "IMAGE//";
    public static final String READ_START = "//READ";
    public static final String READ_STOP = "READ//";
    public static final String THREAD_START = "//THRD";
    public static final String THREAD_STOP = "THRD//";
    public static final String DIRECTION_START = "//DIR";
    public static final String DIRECTION_STOP = "DIR//";
    public static final String NOTIFICATION_START = "//NOTI";
    public static final String NOTIFICATION_STOP = "NOTI//";
    public static final String ADDRESS_START = "//ADDR";
    public static final String ADDRESS_STOP = "ADDR//";
    public static final String SUBJECT_START = "//SUBJ";
    public static final String SUBJECT_STOP = "SUBJ//";



    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_WAITING = 2;
    public static final int STATE_CONNECTING = 3; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 4;
    public static final int REFRESH = 5;
    public static final int TIMER_10 = 10;
    public static final int TIMER_60 = 60;
    public static final int TIMER_CANCEL = 666;


//
//    public static final String USER = "(STOP121212)";
//    public static final String ID = "(STOP121212)";
//    public static final String TIME = "(STOP121212)";
//    public static final String MESSAGE = "(STOP121212)";

//    public static final int BUFFERSIZE = 8192;
    public static final int BUFFERSIZE = 32768;




}
