package com.mats.bluetooth.listeners;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Objects;

/**
 * Created by mats on 2017-09-14.
 */

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private static NotificationListenerInterface mNotify;

    public interface NotificationListenerInterface {
        void onNotificationReceived(String address, String subject, String message);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    public void setListener(NotificationListenerInterface mNotificationListenerInterface) {
        mNotify = mNotificationListenerInterface;
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Implement what you want here
//        if (mNotify != null) {
//            mNotify.onNotificationReceived("Posted " + sbn.toString());
//        }
        Bundle extras = sbn.getNotification().extras;

        Log.d(TAG, "onNotificationPosted: " + extras.toString());
        if (mNotify != null) {

            if (Objects.equals(sbn.getPackageName(), "com.google.android.gm")) {
                String subject = extras.getCharSequence("android.text").toString();
                String message = extras.getCharSequence("android.bigText").toString().replaceFirst(extras.getCharSequence("android.text").toString(),"").replace("\n", "").replace("\r", "");
                mNotify.onNotificationReceived(extras.getString("android.title"), subject , message);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Implement what you want here
//        Log.d(TAG, "onNotificationPosted: Removed NotificationListener");
//        Bundle extras = sbn.getNotification().extras;
//        Log.d(TAG, "onNotificationPosted: " + extras.toString());
        Bundle extras = sbn.getNotification().extras;
        if (mNotify != null) {

//            if (Objects.equals(sbn.getPackageName(), "com.google.android.gm")) {
//                String subject = extras.getCharSequence("android.text").toString();
//                String message = extras.getCharSequence("android.bigText").toString().replaceFirst(extras.getCharSequence("android.text").toString(),"").replace("\n", "").replace("\r", "");
//                mNotify.onNotificationReceived("Nytt mail från: " + extras.getString("android.title")
//                        + " Ämne: " + extras.getCharSequence("android.text").toString() + " Meddelande: " + extras.getCharSequence("android.bigText").toString());
//            }
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        Log.d(TAG, "onNotificationPosted: 1 NotificationListener");

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationRemoved(sbn, rankingMap);
        Log.d(TAG, "onNotificationPosted: 2 NotificationListener");

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        super.onNotificationRemoved(sbn, rankingMap, reason);
        Log.d(TAG, "onNotificationPosted: 3 NotificationListener");

    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "onNotificationPosted: 4 NotificationListener");

    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "onNotificationPosted: 5 NotificationListener");

    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        super.onNotificationRankingUpdate(rankingMap);
        Log.d(TAG, "onNotificationPosted: 6 NotificationListener");

    }

    @Override
    public void onListenerHintsChanged(int hints) {
        super.onListenerHintsChanged(hints);
        Log.d(TAG, "onNotificationPosted: 7 NotificationListener");

    }

    @Override
    public void onNotificationChannelModified(String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
        super.onNotificationChannelModified(pkg, user, channel, modificationType);
        Log.d(TAG, "onNotificationPosted: 8 NotificationListener");

    }

    @Override
    public void onNotificationChannelGroupModified(String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType);
        Log.d(TAG, "onNotificationPosted: 9 NotificationListener");

    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
        super.onInterruptionFilterChanged(interruptionFilter);
        Log.d(TAG, "onNotificationPosted: 10otificationListener");

    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        Log.d(TAG, "onNotificationPosted: 11 NotificationListener");

        return super.getActiveNotifications();
    }

    @Override
    public StatusBarNotification[] getActiveNotifications(String[] keys) {
        Log.d(TAG, "onNotificationPosted: 12 NotificationListener");

        return super.getActiveNotifications(keys);
    }

    @Override
    public RankingMap getCurrentRanking() {
        Log.d(TAG, "onNotificationPosted: 13 NotificationListener");

        return super.getCurrentRanking();
    }
}
