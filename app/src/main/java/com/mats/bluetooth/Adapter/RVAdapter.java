package com.mats.bluetooth.Adapter;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.Dialog.AddingTaskDialogFragment2;
import com.mats.bluetooth.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.ItemViewHolder> {
    private static final String TAG = "RVAdapter";
    private List<String> messageList, userList, idList, numberList, threadList;
    private List<String> itemsPendingRemoval;
    private Database dbHelper;
    private Context context;

    private static final int PENDING_REMOVAL_TIMEOUT = 1000; // 1.5 sec
    private Handler handler = new Handler(); // handler for running delayed runrables
    private HashMap<String, Runnable> pendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be


//    public RVAdapter(List<String> messageList) {
//        this.messageList = messageList;
//        itemsPendingRemoval = new ArrayList<>();
//    }



    public RVAdapter(Cursor mCursor) {
        dbHelper = Database.getInstance(context);
//        this.messageList = messageList;
        itemsPendingRemoval = new ArrayList<>();

        messageList = new ArrayList<>();
        userList = new ArrayList<>();
        numberList = new ArrayList<>();
        idList = new ArrayList<>();
        threadList = new ArrayList<>();
        for(mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            // The Cursor is now set to the right position
            idList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_REMOTE_ID)));
            threadList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_THREAD)));
            numberList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NUMBER)));
            userList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NAME)));
            messageList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_MESSAGE)));
            Log.d(TAG, "RVAdapter: ID " + mCursor.getString(mCursor.getColumnIndex(Database.KEY_REMOTE_ID)));
            Log.d(TAG, "RVAdapter: 1 " + mCursor.getString(1));
            Log.d(TAG, "RVAdapter: 2 " + mCursor.getString(2));
            Log.d(TAG, "RVAdapter: 3 " + mCursor.getString(3));
        }




    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
        context = parent.getContext();
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        final String message = messageList.get(position);
        final String number = numberList.get(position);
        final String thread = threadList.get(position);
        final String user = userList.get(position);
        final String id = idList.get(position);

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
            holder.regularLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setVisibility(View.GONE);
            holder.listNumber.setText(user);
            holder.listNumber.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            holder.listNumber.setSingleLine(true);
            holder.listNumber.setSelected(true);
            holder.listMessage.setText(message);
            holder.listMessage.setEllipsize(TextUtils.TruncateAt.END);
            holder.listMessage.setSingleLine(true);
            holder.listMessage.setSelected(true);

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

    private void undoOpt(String customer) {
        Runnable pendingRemovalRunnable = pendingRunnables.get(customer);
        pendingRunnables.remove(customer);
        if (pendingRemovalRunnable != null)
            handler.removeCallbacks(pendingRemovalRunnable);
        itemsPendingRemoval.remove(customer);
        // this will rebind the row in "normal" state
        notifyItemChanged(messageList.indexOf(customer));
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void pendingRemoval(int position) {

        final String data = messageList.get(position);
        final String id = idList.get(position);
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
            messageList.remove(position);
            notifyItemRemoved(position);
        }
        if(idList.contains(idIn)){
            idList.remove(position);
            dbHelper.markSmsDeleted(idIn);
        }

    }

    public boolean isPendingRemoval(int position) {
        String data = messageList.get(position);
        return itemsPendingRemoval.contains(data);
    }




    public class ItemViewHolder extends RecyclerView.ViewHolder {

        public LinearLayout regularLayout;
        public LinearLayout swipeLayout;
        public TextView listNumber, listMessage;
        public TextView undo;

        public ItemViewHolder(View view) {
            super(view);

            regularLayout = (LinearLayout) view.findViewById(R.id.regularLayout);
            listNumber = (TextView) view.findViewById(R.id.list_number);
            listMessage = (TextView) view.findViewById(R.id.list_message);
            swipeLayout = (LinearLayout) view.findViewById(R.id.swipeLayout);
            undo = (TextView) view.findViewById(R.id.undo);

        }
    }



}