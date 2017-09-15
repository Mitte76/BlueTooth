package com.mats.bluetooth.Adapter;


import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ItemViewHolder> {
    private static final String TAG = "SmsThreadsAdapter";
    private List<String> addressList, messageList, subjectList;
    private List<Long> idList, itemsPendingRemoval;
    private Database dbHelper;
    private Context context;
    private static final int PENDING_REMOVAL_TIMEOUT = 1000; // 1.5 sec
    private Handler handler = new Handler(); // handler for running delayed runrables
    private HashMap<Long, Runnable> pendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be

//    public SmsThreadsAdapter(List<String> idList) {
//        this.idList = idList;
//        itemsPendingRemoval = new ArrayList<>();
//    }


    public NotificationAdapter(Cursor mCursor) {
        dbHelper = Database.getInstance(context);
        itemsPendingRemoval = new ArrayList<>();
        idList = new ArrayList<>();
        addressList = new ArrayList<>();
        messageList = new ArrayList<>();
        subjectList = new ArrayList<>();

        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            // The Cursor is now set to the right position
            Bitmap bitmap = null;

            idList.add(mCursor.getLong(mCursor.getColumnIndex(Database.KEY_ID)));
            addressList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NOTIFICATION_ADDRESS)));
            messageList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NOTIFICATION_MESSAGE)));
            subjectList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NOTIFICATION_SUBJECT)));


        }


    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_row_item, parent, false);
        context = parent.getContext();
        return new ItemViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        String tmp;
        final Long id = idList.get(position);
        final String message = messageList.get(position);
        final String subject = subjectList.get(position);
        final String address = addressList.get(position);

        if (itemsPendingRemoval.contains(id)) {
            holder.regularLayout.setVisibility(View.GONE);
            holder.swipeLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    undoOpt(id);
                }
            });
        } else {
            holder.regularLayout.setVisibility(View.VISIBLE);
            holder.swipeLayout.setVisibility(View.GONE);
            holder.notiAddress.setText(address);
            holder.notiSubject.setText(subject);
            holder.notiMessage.setText(message);
            Log.d(TAG, "onBindViewHolder: " + message);
//            holder.notiSubject.setEllipsize(TextUtils.TruncateAt.END);
//            holder.notiSubject.setSingleLine(true);
//            holder.notiSubject.setSelected(true);

        }
    }

    private void undoOpt(Long id) {
        Runnable pendingRemovalRunnable = pendingRunnables.get(id);
        pendingRunnables.remove(id);
        if (pendingRemovalRunnable != null)
            handler.removeCallbacks(pendingRemovalRunnable);
        itemsPendingRemoval.remove(id);
        // this will rebind the row in "normal" state
        notifyItemChanged(idList.indexOf(id));
    }


    @Override
    public int getItemCount() {
        return idList.size();
    }

    public void pendingRemoval(int position) {

        final Long id = idList.get(position);
        final String message = messageList.get(position);
        if (!itemsPendingRemoval.contains(id)) {
            itemsPendingRemoval.add(id);
            // this will redraw row in "undo" state
            notifyItemChanged(position);
            // let's create, store and post a runnable to remove the data
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(idList.indexOf(id));
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(id, pendingRemovalRunnable);
        }
    }

    private void remove(int position) {
        Long id = idList.get(position);
        if (itemsPendingRemoval.contains(id)) {
            itemsPendingRemoval.remove(id);
        }
        if (idList.contains(id)) {
            idList.remove(position);
            messageList.remove(position);
            addressList.remove(position);
            subjectList.remove(position);
            dbHelper.deleteNotification(id);
            notifyItemRemoved(position);
        }


    }

    public boolean isPendingRemoval(int position) {
        Long id = idList.get(position);
        return itemsPendingRemoval.contains(id);
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {

        public RelativeLayout regularLayout;
        public LinearLayout swipeLayout;
        public TextView notiAddress, notiSubject, notiMessage;
        public TextView undo;
        public ImageView mmsImageviewLeft, mmsImageviewRight;

        public ItemViewHolder(View view) {
            super(view);

            regularLayout = (RelativeLayout) view.findViewById(R.id.regularLayout);
            notiAddress = (TextView) view.findViewById(R.id.noti_list_address);
            notiSubject = (TextView) view.findViewById(R.id.noti_list_subject);
            notiMessage = (TextView) view.findViewById(R.id.noti_list_message);
            swipeLayout = (LinearLayout) view.findViewById(R.id.swipeLayout);
            undo = (TextView) view.findViewById(R.id.undo);
            mmsImageviewLeft = view.findViewById(R.id.mmsImageViewLeft);
            mmsImageviewRight = view.findViewById(R.id.mmsImageViewRight);
        }
    }


}