package com.mats.bluetooth.Model;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mats on 2017-09-10.
 */

public class Msg {
    private String id;
    private String t_id;
    private String date;
    private String dispDate;
    private String addr;
    private String contact;
    private String direction;
    private String body;
    private Bitmap img;
    private String type;

    public String getDirection() {
        return direction;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    private String read;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private boolean bImg;

    public Msg(String ID) {
        id = ID;
        body = "";
    }

    public void setDate(String d) {
        date = d;
        dispDate = msToDate(date);
    }
    public void setThread(String d) { t_id = d; }

    public void setAddr(String a) {
        addr = a;
    }
    public void setContact(String c) {
        if (c==null) {
            contact = "Unknown";
        } else {
            contact = c;
        }
    }
    public void setDirection(String d) {
        direction = d;

    }
    public void setBody(String b) {
        body = b;
    }
    public void setImg(Bitmap bm) {
        img = bm;
        if (bm != null)
            bImg = true;
        else
            bImg = false;
    }

    public String getDate() {
        return date;
    }
    public String getDispDate() {
        return dispDate;
    }
    public String getThread() { return t_id; }
    public String getID() { return id; }
    public String getBody() { return body; }
    public String getAddress() { return addr; }
    public String getContact() { return contact; }
    public Bitmap getImg() { return img; }
    public boolean hasImg() { return bImg; }

    public String toString() {

        String s = id + ". " + dispDate + " - " + direction + " " + contact + " " + addr + ": "  + body;
        if (bImg)
            s = s + "\nData: " + img;
        return s;
    }

    public String msToDate(String mss) {
        long seconds = Long.parseLong(mss);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE yyyy.MM.d HH:mm", Locale.getDefault());
        String dateString;
        if (type.equals("SMS")){
//            Log.d("rrr", "msToDate: " + type);
            dateString = formatter.format(new Date(seconds));
        } else {
//            Log.d("rrr", "msToDate: " + type);
            dateString = formatter.format(new Date(seconds * 1000L));
        }
//        Log.d("rrr", "msToDate: " + seconds);


//        Log.d("rrr", "msToDate: " + dateString);
        long time = Long.parseLong(mss);

        long sec = ( time / 1000 ) % 60;
        time = time / 60000;

        long min = time % 60;
        time = time / 60;

        long hour = time % 24 - 5;
        time = time / 24;

        long day = time % 365;
        time = time / 365;

        long yr = time + 1970;

        day = day - ( time / 4 );
        long mo = getMonth(day);
        day = getDay(day);

        mss = String.valueOf(yr) + "/" + String.valueOf(mo) + "/" + String.valueOf(day) + " " + String.valueOf(hour) + ":" + String.valueOf(min) + ":" + String.valueOf(sec);

        return dateString;
    }
    public long getMonth(long day) {
        long[] calendar = {31,28,31,30,31,30,31,31,30,31,30,31};
        for(int i = 0; i < 12; i++) {
            if(day < calendar[i]) {
                return i + 1;
            } else {
                day = day - calendar[i];
            }
        }
        return 1;
    }
    public long getDay(long day) {
        long[] calendar = {31,28,31,30,31,30,31,31,30,31,30,31};
        for(int i = 0; i < 12; i++) {
            if(day < calendar[i]) {
                return day;
            } else {
                day = day - calendar[i];
            }
        }
        return day;
    }



}