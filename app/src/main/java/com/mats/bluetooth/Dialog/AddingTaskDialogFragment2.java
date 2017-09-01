package com.mats.bluetooth.Dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mats.bluetooth.R;
//import com.shopper.android.fragments.Adapters.SwipeCursorAdapter;
//import com.shopper.android.fragments.DbHelper.Database;


public class AddingTaskDialogFragment2 extends DialogFragment {
    public interface ReplyMessageListener {
        void onReply(String number, String text);

    }

    private final String TAG = AddingTaskDialogFragment2.class.getSimpleName();
    //    private ListView listView;
//    private SwipeCursorAdapter listAdapter;
//    private Database dbHelper;
    private String number, user, message;

    private String title = "test";
    private String hint;
    private String searchText = "";
    private boolean itemAdded = false;
    private EditText editTextMessage;
    private TextView inMessage, inNumber;
    private Button sendButton;
    private ReplyMessageListener mReplyMessageListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        user = getArguments().getString("user");
        number = getArguments().getString("number");
        message = getArguments().getString("message");
        setStyle(DialogFragment.STYLE_NORMAL,android.R.style.Theme_Holo_Light_NoActionBar);
//        setStyle(DialogFragment.STYLE_NORMAL,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        Log.d(TAG, "onCreate: ");
//        try {
//            mReplyMessageListener = (ReplyMessageListener) getTargetFragment();
////            Log.d(TAG, "onCreate: test");
//        } catch (ClassCastException e) {
//            throw new ClassCastException(getTargetFragment().toString() + " must implement ReplyMessageListener");
//        }
//        switch (message){
//            case R.id.list_fab:
//                title = getString(R.string.dialog_add_list);
//                hint = getString(R.string.dialog_add_list_hint);
//                break;
//            case R.id.item_fab:
//                title = getString( R.string.dialog_add_item);
//                hint = getString( R.string.dialog_add_item_hint);
//                break;
//        }


    }
//    private ReplyMessageListener mReplyMessageListener;


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
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        View v = inflater.inflate(R.layout.dialog_task2, container, false);
        inMessage = v.findViewById(R.id.inMessage);
        inNumber = v.findViewById(R.id.inNumber);
        sendButton = v.findViewById(R.id.dialogSendBtn);
        editTextMessage = v.findViewById(R.id.dialogSendMessage);
        inMessage.setText(message);
//        inNumber.setText(user);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mReplyMessageListener.onReply(number, editTextMessage.getText().toString());
                dismiss();
            }
        });
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

    public String makePerfect(String inString) {


        return inString.substring(0, 1).toUpperCase() + inString.substring(1).toLowerCase();
    }


}

