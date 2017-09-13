///*
// * Copyright (C) 2014 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.mats.bluetooth;
//
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentTransaction;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//
////import com.example.android.common.logger.Log;
//
///**
// * This fragment controls Bluetooth to communicate with other devices.
// */
//public class SelectFragment extends Fragment {
//    private Button masterBtn;
//    private Button slaveBtn;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
////        setHasOptionsMenu(true);
//
//
//    }
//
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        setup();
//
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.activity_select, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        masterBtn = (Button) view.findViewById(R.id.master_btn);
//        slaveBtn = (Button) view.findViewById(R.id.slave_btn);
//    }
//
//    private void setup() {
//
//        // Initialize the send button with a listener that for click events
//        masterBtn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
//                MasterFragment fragment = new MasterFragment();
//                transaction.replace(R.id.sample_content_fragment, fragment);
//                transaction.commit();
//
//            }
//        });
//        slaveBtn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
//                SlaveFragment fragment = new SlaveFragment();
//                transaction.replace(R.id.sample_content_fragment, fragment);
//                transaction.commit();
//
//            }
//        });
//    }
//
////
////    /**
////     * Updates the status on the action bar.
////     *
////     * @param subTitle status
////     */
////    private void setStatus(CharSequence subTitle) {
////        FragmentActivity activity = getActivity();
////        if (null == activity) {
////            return;
////        }
////        final ActionBar actionBar = activity.getActionBar();
////        if (null == actionBar) {
////            return;
////        }
////        actionBar.setSubtitle(subTitle);
////    }
//
//
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//
//        inflater.inflate(R.menu.master, menu);
//    }
//
//
//
////    @Override
////    public boolean onOptionsItemSelected(MenuItem item) {
////        switch (item.getItemId()) {
////            case R.id.secure_connect_scan: {
////                // Launch the DeviceListActivity to see devices and do scan
////                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
////                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
////                return true;
////            }
////            case R.id.insecure_connect_scan: {
////                // Launch the DeviceListActivity to see devices and do scan
////                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
////                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
////                return true;
////            }
////            case R.id.discoverable: {
////                // Ensure this device is discoverable by others
////                ensureDiscoverable();
////                return true;
////            }
////            case R.id.permission: {
////                // Fix permissions
////                checkPermission();
////                return true;
////            }
////        }
////        return false;
////    }
//
//}
