package com.mats.bluetooth.Dialog;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.Adapter.RVAdapter2;
import com.mats.bluetooth.DbHelper.Database;
import com.mats.bluetooth.R;

import java.util.Objects;


public class AddingTaskDialogFragment2 extends DialogFragment {
    public interface ReplyMessageListener {
        void onReply(String number, String text);

        void onMarkRead(String id, String number, String message);
    }

    private final String TAG = AddingTaskDialogFragment2.class.getSimpleName();
    private String number, user, message, id, thread;
    private Database dbHelper;
    private EditText editTextMessage;
    private TextView inMessage;
    private ImageView imageView;
    private Button sendButton/*, markReadButton*/;
    private ReplyMessageListener mReplyMessageListener;
    private RVAdapter2 rvAdapter;
    private RecyclerView mRecyclerview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = Database.getInstance(getContext());

        id = getArguments().getString("id");
        thread = getArguments().getString("thread");
        user = getArguments().getString("user");
        number = getArguments().getString("number");
        message = getArguments().getString("message");
        Log.d(TAG, "onCreate: fffffffffffff" + thread);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_NoActionBar);
//        setStyle(DialogFragment.STYLE_NORMAL,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        Log.d(TAG, "onCreate: ");


    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mReplyMessageListener = (ReplyMessageListener) context;
            Log.d(TAG, "onAttach: ");
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ReplyMessageListener");
        }

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        View v = inflater.inflate(R.layout.dialog_task2, container, false);
        inMessage = v.findViewById(R.id.inMessage);
        imageView = v.findViewById(R.id.imageView);
        TextView inNumber = v.findViewById(R.id.inNumber);
        sendButton = v.findViewById(R.id.dialogSendBtn);
//        markReadButton = v.findViewById(R.id.dialogMarkRead);
        editTextMessage = v.findViewById(R.id.dialogSendMessage);
        mRecyclerview = v.findViewById(R.id.recyclerViewDialog);


        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        inMessage.setText(message);
//        inNumber.setText(user);

        rvAdapter = new RVAdapter2(dbHelper.getSMS2(thread));
        mRecyclerview.setAdapter(rvAdapter);
        Cursor cursor = dbHelper.getOneSMS(id);
        cursor.moveToFirst();
        Log.d(TAG, "onCreateView: " + id + " Cursor size " + cursor.getCount());
        String image = cursor.getString(cursor.getColumnIndex(Database.KEY_IMAGE));
        if (image != null) {
        Bitmap bitmap;
//        try {
            byte[] encodeByte = Base64.decode(image, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            Point size = new Point();
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getSize(size);
            int width = size.x / 2;
            int nh = (int) (bitmap.getHeight() * ((double) width / bitmap.getWidth()));
            imageView.getLayoutParams().width = width;
            imageView.getLayoutParams().height = nh;
            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "onCreateView: Image size " + image.length());

//        } catch (Exception e) {
//            e.getMessage();
//        }


        }


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mReplyMessageListener.onReply(number, editTextMessage.getText().toString());
                dismiss();
            }
        });

//        markReadButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mReplyMessageListener.onMarkRead(id, number, message);
//                dismiss();
//            }
//        });
        Log.d(TAG, "onCreateView: ");

        return v;

//        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

//        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    /*
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.d(TAG, "onCreateDialog: ");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    //        dbHelper = Database.getInstance(getContext());
            View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_task2, null);
    //        listView = (ListView) view.findViewById(R.id.dialog_list);
            search = (EditText) view.findViewById(R.id.dialogSearch);

            search.setHint(hint);
            final Button btnAdd = (Button) view.findViewById(R.id.dialog_add);
    //        btnAdd.setOnClickListener(new View.OnClickListener() {
    //            @Override
    //            public void onClick(View v) {
    //
    //                    switch (message) {
    //                        case R.id.list_fab:
    //                            long listNo = dbHelper.createList(makePerfect(search.getText().toString()));
    //                            mReplyMessageListener.onListAdded(listNo);
    //                            search.setText("");
    //                            Toast.makeText(getContext(), "List " + search.getText().toString() + " created", Toast.LENGTH_SHORT).show();
    //                            dismiss();
    //                            break;
    //                        case R.id.item_fab:
    //                            dbHelper.createItem(makePerfect(search.getText().toString()), "default", number);
    //                            mReplyMessageListener.onItemAdded(number);
    //                            search.setText("");
    //                            Toast.makeText(getContext(), search.getText().toString() + " added to list", Toast.LENGTH_SHORT).show();
    //                            itemAdded = true;
    //                            break;
    //                }
    //            }
    //        });

    //        getDialog().setTitle("Reply to SMS");

            builder.setTitle("Reply to SMS")
                    .setView(view);
    //        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialog, int which) {
    //                if(itemAdded) {
    //                    mReplyMessageListener.onTaskAddingCancel();
    //                }
    //                dialog.cancel();
    //            }
    //        });
            AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    if (search.length() == 0) {
    //                    btnAdd.setVisibility(View.GONE);
                    }

    //                search.addTextChangedListener(new TextWatcher() {
    //                    @Override
    //                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    //                    }
    //
    //                    @Override
    //                    public void onTextChanged(CharSequence s, int start, int before, int count) {
    //                        if (s.length() == 0) {
    //                            btnAdd.setVisibility(View.GONE);
    //
    //                            if(message != R.id.list_fab){
    //                                listAdapter.changeCursor(dbHelper.getAutocomplete(search.getText().toString(),number ));
    //                            }
    //
    //                        } else {
    //                            btnAdd.setVisibility(View.VISIBLE);
    //
    //
    //                            if(message != R.id.list_fab){
    //                                listAdapter.changeCursor(dbHelper.getAutocomplete(search.getText().toString(),number));
    //                            }
    //                        }
    //                        searchText = search.getText().toString();
    //                    }
    //
    //                    @Override
    //                    public void afterTextChanged(Editable s) {
    //
    //                    }
    //                });
                }
            });
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            search.setOnEditorActionListener(mWriteListener);

    //        Log.d("dd", "onEditorAction: " + mReplyMessageListener);


            return builder.create();
        }
    */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                String message = view.getText().toString();
                Toast.makeText(getActivity(), message,
                        Toast.LENGTH_LONG).show();
            }
            Log.d("dd", "onEditorAction: " + actionId + " : " + EditorInfo.IME_NULL + " : " + mReplyMessageListener);
            mReplyMessageListener.onReply(number, editTextMessage.getText().toString());
            dismiss();
            return true;
        }
    };
//    @Override
//    public void onAddItemFromDialog(String item,String category) {
//        Log.d("TAG", "onAddItemFromDialog: hvhjvjh" + category);
//        dbHelper.createItem(item,category,number);
//        mReplyMessageListener.onItemAdded(number);
//        listAdapter.changeCursor(dbHelper.getAutocomplete(searchText,number));
//        itemAdded = true;
//        Toast.makeText(getContext(), item + " added to list", Toast.LENGTH_SHORT).show();
//
//    }


}

