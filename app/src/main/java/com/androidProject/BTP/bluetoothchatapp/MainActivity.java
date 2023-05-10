package com.androidProject.BTP.bluetoothchatapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

//import java.text.FieldPosition;
//import java.text.Format;
//import java.text.ParsePosition;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.ListIterator;


import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;

    private static final String deviceAddress = "deviceAddress";

    private static final int ACTIVITY_LIFETIME = 30000;

    private int currentDeviceIndex = 0;


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE: System.out.println("State is assigned to be None");
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            System.out.println("Step->5: Finally, I am in handleMessage of MainActivity.java!");
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
//                    StringAlignUtils util = new StringAlignUtils(70, StringAlignUtils.Alignment.RIGHT);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    adapterMainChat.add(connectedDevice + ": " + inputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        init();
        initBluetooth();
        chatUtils = new ChatUtils(context, handler);
    }

    private void init() {
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        btnSendMessage = findViewById(R.id.btn_send_msg);

        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String message = edCreateMessage.getText().toString();
                System.out.println("message is: "+ message);
                if (!message.isEmpty()) {
                    edCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No Bluetooth found", Toast.LENGTH_SHORT).show();
        }
//        else {
//
//        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                checkPermissions();
                return true;
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void startIteration(ArrayList<BluetoothDevice> deviceList){

        if(currentDeviceIndex >= deviceList.size()){
            // we have processed all the devices, do something else
            // for example, show a message or finish the current activity
//            finish();
            System.out.println("Device List has been finished!");
            currentDeviceIndex = 0;
            return;
        }
//        for( BluetoothDevice device: pairedDevices){
////            BluetoothDevice firstElement = it.next();
//            System.out.println("The first element is: " + device.getAddress() + " and its device: "+device.getName());
//            String address = device.getAddress();
//            Log.d("Selected Address", address);
//
//            Intent intent = new Intent();
//            intent.putExtra("deviceAddress", address);
//            startActivityForResult(intent, SELECT_DEVICE);
////            setResult(Activity.RESULT_OK, intent);
////            finish();
////            System.out.println("Hey Shivam, complete this soon!");
//            try {
//                Thread.sleep(30000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        BluetoothDevice device = deviceList.get(currentDeviceIndex);
        String address = device.getAddress();
        String name = device.getName();
        Log.d("Selected Address", address);
        Log.d("Selected Device Name", name);

//        Intent intent = new Intent();
//        intent.putExtra(deviceAddress, address);
//        startActivityForResult(intent, SELECT_DEVICE); // need to see where it goes
        chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));


        currentDeviceIndex++;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // we have waited for 30 seconds, start the next iteration
                // stopping the thread from line 214
                chatUtils.stop(); // commented because chatUtils.connect() is calling chatUtils.stop(), no need to call separately now
                System.out.println("Disconnecting the current connection: "+name);

                startIteration(deviceList);
                // adding print

            }
        }, 20000);
//        currentIteration++;
//        Intent intent = new Intent(context, DeviceListActivity.class);
//        startActivityForResult(intent, SELECT_DEVICE);

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (currentIteration < 3) {
//                    finishActivity(1);
//                    // start next iteration after a delay of 5 seconds
//                    new Handler().postDelayed(() -> startIteration(), 5000);
//                } else {
//                    // we have completed the maximum number of iterations, do something else
//                    // for example, show a message or finish the current activity
//                    finish();
//                }
////                finishActivity(1);
//            }
//        }, 15000);
    }
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
        else {
//            int i=0;
//            while(i<3) {
//                Intent intent = new Intent(context, DeviceListActivity.class);
//                startActivityForResult(intent, SELECT_DEVICE);
//                i++;
//            }
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
            currentDeviceIndex = 0;
            startIteration(deviceList);
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
//            String address = data.getStringExtra(deviceAddress);
//            System.out.println("device name obtained is(in onActivityResult) : " + address);
//            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("I don't know when this function is getting called.");
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Intent intent = new Intent(context, DeviceListActivity.class);
//                startActivityForResult(intent, SELECT_DEVICE);
//            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }
    }

    @Override
    protected void onDestroy() {
        System.out.println("In destroy function!");
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();
        }
    }
}
