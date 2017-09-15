package com.mats.bluetooth.Adapter;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.Dialog.AddingTaskDialogFragment2;
import com.mats.bluetooth.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SmsThreadsAdapter extends RecyclerView.Adapter<SmsThreadsAdapter.ItemViewHolder> {
    private static final String TAG = "SmsThreadsAdapter";
    private List<String> messageList, userList, idList, numberList, threadList, readlist;
    private List<String> itemsPendingRemoval;
    private Database dbHelper;
    private Context context;

    private static final int PENDING_REMOVAL_TIMEOUT = 1000; // 1.5 sec
    private Handler handler = new Handler(); // handler for running delayed runrables
    private HashMap<String, Runnable> pendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be


//    public SmsThreadsAdapter(List<String> messageList) {
//        this.messageList = messageList;
//        itemsPendingRemoval = new ArrayList<>();
//    }


    public SmsThreadsAdapter(Cursor mCursor) {
        dbHelper = Database.getInstance(context);
//        this.messageList = messageList;
        itemsPendingRemoval = new ArrayList<>();


        readlist = new ArrayList<>();
        messageList = new ArrayList<>();
        userList = new ArrayList<>();
        numberList = new ArrayList<>();
        idList = new ArrayList<>();
        threadList = new ArrayList<>();
        if (mCursor != null) {
            for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
                // The Cursor is now set to the right position
                readlist.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_READ)));
                idList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_REMOTE_ID)));
                threadList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_THREAD)));
                numberList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NUMBER)));
                userList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NAME)));
                messageList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_MESSAGE)));
                Log.d(TAG, "SmsThreadsAdapter: ID " + mCursor.getString(mCursor.getColumnIndex(Database.KEY_REMOTE_ID)));
                Log.d(TAG, "SmsThreadsAdapter: 1 " + mCursor.getString(1));
                Log.d(TAG, "SmsThreadsAdapter: 2 " + mCursor.getString(2));
                Log.d(TAG, "SmsThreadsAdapter: 3 " + mCursor.getString(3));
            }
        }

    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.smsthread_row_item, parent, false);
        context = parent.getContext();
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        final String message = messageList.get(position);
        final String number = numberList.get(position);
        final String thread = threadList.get(position);
        final String user = userList.get(position) + ": ";
        final String id = idList.get(position);
        final String read = readlist.get(position);

        if (itemsPendingRemoval.contains(message)) {
            holder.regularLayout.setVisibility(View.GONE);
            holder.swipeLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    undoOpt(message);
                }
            });
        } else {
            ForegroundColorSpan userColor = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.number_bg));
            SpannableString userOut = new SpannableString(user);
            SpannableString messageOut = new SpannableString(message);
            holder.listCount.setText(dbHelper.threadCount(thread));
            holder.regularLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setVisibility(View.GONE);
//            userOut.setSpan(userColor, 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


//            holder.notiSubject.setText(message);
//            holder.notiSubject.setEllipsize(TextUtils.TruncateAt.END);
//            holder.notiSubject.setSingleLine(true);
//            holder.notiSubject.setSelected(true);
//            SpannableString s = new SpannableString(user + message);

//            messageOut.setSpan(bgNumber, 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

//            SpannableString spannableString = new SpannableString(user + message);
//            BackgroundColorSpan bgMessage = new BackgroundColorSpan(ContextCompat.getColor(context,R.color.unread_msg_text_bg));
//            s.setSpan(new android.text.style.LeadingMarginSpan.Standard(user.length(), 0), 0, 1, 0);
            if (Objects.equals(read, "0")) {
//                ForegroundColorSpan bgMessage = new ForegroundColorSpan(ContextCompat.getColor(context,R.color.unread_msg_text_bg));
//                s.setSpan(bgMessage, user.length() , s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
                messageOut.setSpan(bold, 0, messageOut.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                userOut.setSpan(bold, 0, userOut.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                ForegroundColorSpan bgNumber = new ForegroundColorSpan(ContextCompat.getColor(context,R.color.number_bg_bold));
//                s.setSpan(bgNumber, 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

//                holder.notiSubject.setBackgroundColor(Color.LTGRAY);
            } else {

                Log.d(TAG, "onBindViewHolder: " + read);
            }
            holder.listMessageLeft.setText(messageOut);
            holder.listMessageLeft.setEllipsize(TextUtils.TruncateAt.END);
            holder.listMessageLeft.setSingleLine(true);
            holder.listMessageLeft.setSelected(true);
            holder.listNumber.setText(userOut);
            holder.listNumber.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            holder.listNumber.setSingleLine(true);
            holder.listNumber.setSelected(true);
//            holder.notiSubject.setText(s);

            holder.regularLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

//                    if (number.substring(0, 1).equals("+")) {
                    android.support.v4.app.DialogFragment addingTaskDialogFragment2 = new AddingTaskDialogFragment2();
                    Bundle args = new Bundle();
                    args.putString("id", id);
                    args.putString("user", user);
                    args.putString("number", number);
                    args.putString("message", message);
                    args.putString("thread", thread);
                    addingTaskDialogFragment2.setArguments(args);
                    addingTaskDialogFragment2.show(((AppCompatActivity) context).getSupportFragmentManager(), "AddingTaskDialogFragment");
//                    } else {
//                        Toast.makeText(context, R.string.no_reply, Toast.LENGTH_SHORT).show();
//                    }


                }
            });
        }
    }

    private void undoOpt(String id) {
        Runnable pendingRemovalRunnable = pendingRunnables.get(id);
        pendingRunnables.remove(id);
        if (pendingRemovalRunnable != null)
            handler.removeCallbacks(pendingRemovalRunnable);
        itemsPendingRemoval.remove(id);
        // this will rebind the row in "normal" state
        notifyItemChanged(messageList.indexOf(id));
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void pendingRemoval(int position) {

        final String data = messageList.get(position);
        final String id = threadList.get(position);
        if (!itemsPendingRemoval.contains(data)) {
            itemsPendingRemoval.add(data);
            // this will redraw row in "undo" state
            notifyItemChanged(position);
            // let's create, store and post a runnable to remove the data
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(messageList.indexOf(data), id);
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(data, pendingRemovalRunnable);
        }
    }

    private void remove(int position, String idIn) {
        String data = messageList.get(position);
        if (itemsPendingRemoval.contains(data)) {
            itemsPendingRemoval.remove(data);
        }
        if (messageList.contains(data)) {
            dbHelper.markSmsDeleted(idIn);
            messageList.remove(position);
            numberList.remove(position);
            threadList.remove(position);
            readlist.remove(position);
            userList.remove(position);
            idList.remove(position);
            notifyItemRemoved(position);
        }


        if (idList.contains(idIn)) {

        }

    }

    public boolean isPendingRemoval(int position) {
        String data = messageList.get(position);
        return itemsPendingRemoval.contains(data);
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {

        public LinearLayout regularLayout;
        public LinearLayout swipeLayout;
        public TextView listNumber, listCount, listMessageLeft;
        public TextView undo;

        public ItemViewHolder(View view) {
            super(view);

            regularLayout = (LinearLayout) view.findViewById(R.id.regularLayout);
            listNumber = (TextView) view.findViewById(R.id.list_number);
            listCount = (TextView) view.findViewById(R.id.list_count);
            listMessageLeft = (TextView) view.findViewById(R.id.list_message);
            swipeLayout = (LinearLayout) view.findViewById(R.id.swipeLayout);
            undo = (TextView) view.findViewById(R.id.undo);

        }
    }


}