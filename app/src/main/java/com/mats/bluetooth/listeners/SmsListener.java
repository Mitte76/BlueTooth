package com.mats.bluetooth.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

/**
 * Created by mats on 2017-08-22.
 */


/**
 * A broadcast receiver who listens for incoming SMS
 */

public class SmsListener extends BroadcastReceiver {
    public interface SmsListenerInterface {
        void onTextReceived();
    }

    private static final String TAG = "SmsBroadcastReceiver";
    private static SmsListenerInterface mSmsListenerInterface;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            mSmsListenerInterface.onTextReceived();
        }
    }

    public void setListener(SmsListenerInterface smsListenerInterface) {
        mSmsListenerInterface = smsListenerInterface;
    }


}