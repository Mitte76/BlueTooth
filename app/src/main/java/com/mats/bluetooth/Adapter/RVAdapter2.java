package com.mats.bluetooth.Adapter;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RVAdapter2 extends RecyclerView.Adapter<RVAdapter2.ItemViewHolder> {
    private static final String TAG = "RVAdapter";
    private List<String> messageList, userList, readlist, numberList;
    private List<Bitmap> bitmapList;
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


    public RVAdapter2(Cursor mCursor) {
        dbHelper = Database.getInstance(context);
//        this.messageList = messageList;
        itemsPendingRemoval = new ArrayList<>();
        bitmapList = new ArrayList<>();
        messageList = new ArrayList<>();
        userList = new ArrayList<>();
        numberList = new ArrayList<>();
        readlist = new ArrayList<>();
        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            // The Cursor is now set to the right position
            Bitmap bitmap = null;

            readlist.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_READ)));
            numberList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NUMBER)));
            userList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_NAME)));
            messageList.add(mCursor.getString(mCursor.getColumnIndex(Database.KEY_MESSAGE)));
            String image = mCursor.getString(mCursor.getColumnIndex(Database.KEY_IMAGE));
            if (image != null) {
                try {
                    byte[] encodeByte = Base64.decode(image, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);

                    Log.d(TAG, "onCreateView: Image size " + image.length());

                } catch (Exception e) {
                    e.getMessage();
                }
            }
            bitmapList.add(bitmap);
            Log.d(TAG, "RVAdapter2: Bitmaplist " + bitmapList.size());
            Log.d(TAG, "RVAdapter: ID " + mCursor.getString(mCursor.getColumnIndex(Database.KEY_REMOTE_ID)));
            Log.d(TAG, "RVAdapter: 1 " + mCursor.getString(1));
            Log.d(TAG, "RVAdapter: 2 " + mCursor.getString(2));
            Log.d(TAG, "RVAdapter: 3 " + mCursor.getString(3));
        }


    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item2, parent, false);
        context = parent.getContext();
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        final String message = messageList.get(position);
        final String user = userList.get(position) + ": ";
        final String read = readlist.get(position);
        final Bitmap bitmap = bitmapList.get(position);
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
//            holder.listNumber.setText(user);
//            holder.listNumber.setEllipsize(TextUtils.TruncateAt.MARQUEE);
//            holder.listNumber.setSingleLine(true);
//            holder.listNumber.setSelected(true);
            SpannableString s = new SpannableString(user + message);

            ForegroundColorSpan bgNumber = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.number_bg));
            s.setSpan(bgNumber, 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

//            SpannableString spannableString = new SpannableString(user + message);
//            BackgroundColorSpan bgMessage = new BackgroundColorSpan(ContextCompat.getColor(context,R.color.unread_msg_text_bg));
//            s.setSpan(new android.text.style.LeadingMarginSpan.Standard(user.length(), 0), 0, 1, 0);
            if (Objects.equals(read, "0")) {
//                ForegroundColorSpan bgMessage = new ForegroundColorSpan(ContextCompat.getColor(context,R.color.unread_msg_text_bg));
//                s.setSpan(bgMessage, user.length() , s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
                s.setSpan(bold, 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                ForegroundColorSpan bgNumber = new ForegroundColorSpan(ContextCompat.getColor(context,R.color.number_bg_bold));
//                s.setSpan(bgNumber, 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

//                holder.listMessage.setBackgroundColor(Color.LTGRAY);
            } else {

                Log.d(TAG, "onBindViewHolder: " + read);
            }
            holder.listMessage.setText(s);

            if (bitmap != null) {
                Point size = new Point();
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                display.getSize(size);
                int width = size.x / 7;
                int nh = (int) (bitmap.getHeight() * ((double) width / bitmap.getWidth()));
                holder.mmsImageview.getLayoutParams().width = width;
                holder.mmsImageview.getLayoutParams().height = nh;
                holder.mmsImageview.setImageBitmap(bitmap);
            }

//            holder.listMessage.setEllipsize(TextUtils.TruncateAt.END);
//            holder.listMessage.setSingleLine(true);
//            holder.listMessage.setSelected(true);

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
        final String id = readlist.get(position);
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
        if (readlist.contains(idIn)) {
            readlist.remove(position);
            dbHelper.markSmsDeleted(idIn);
        }

    }

    public boolean isPendingRemoval(int position) {
        String data = messageList.get(position);
        return itemsPendingRemoval.contains(data);
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {

        public RelativeLayout regularLayout;
        public LinearLayout swipeLayout;
        public TextView listNumber, listMessage;
        public TextView undo;
        public ImageView mmsImageview;

        public ItemViewHolder(View view) {
            super(view);

            regularLayout = (RelativeLayout) view.findViewById(R.id.regularLayout);
            listNumber = (TextView) view.findViewById(R.id.list_number);
            listMessage = (TextView) view.findViewById(R.id.list_message);
            swipeLayout = (LinearLayout) view.findViewById(R.id.swipeLayout);
            undo = (TextView) view.findViewById(R.id.undo);
            mmsImageview = view.findViewById(R.id.mmsImageView);
        }
    }


}