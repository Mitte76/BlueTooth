package com.mats.bluetooth.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.mats.bluetooth.R;

/**
 * Created by mats on 2017-08-22.
 */


/**
 * A broadcast receiver who listens for incoming SMS
 */

public class SmsListener extends BroadcastReceiver {
    public interface Listener {
        void onTextReceived();
    }

    private static final String TAG = "SmsBroadcastReceiver";
    private static Listener mListener;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            mListener.onTextReceived();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }


}