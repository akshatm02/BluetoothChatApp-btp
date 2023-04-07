package com.androidProject.BTP.bluetoothchatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
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

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;

    // Request codes for location permission and selecting a device
    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;

    // Message types
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    // Keys for messages
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;

    // Handler to manage messages
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE:
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    // Write a message to the chat
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    // Read a message from the chat
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    adapterMainChat.add(connectedDevice + ": " + inputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // Get the name of the connected device and display it in a toast message
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    // Display a toast message
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    // Set the subtitle of the action bar
    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    // This is the method that gets called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call the parent onCreate method and pass in the saved instance state
        super.onCreate(savedInstanceState);

        // Set the content view of the activity to the activity_main layout
        setContentView(R.layout.activity_main);

        // Set the context variable to the current instance of the activity
        context = this;

        // Call the init method, which initializes necessary components for the activity
        init();

        // Call the initBluetooth method, which initializes Bluetooth functionality for the activity
        initBluetooth();

        // Create a new instance of ChatUtils, which likely initializes a utility class for handling chat functionality in the activity
        chatUtils = new ChatUtils(context, handler);
    }


    // This method initializes necessary components for the activity
    private void init() {
        // Set up the main chat list view, message input field, and send button
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        btnSendMessage = findViewById(R.id.btn_send_msg);

        // Create a new ArrayAdapter to hold the chat messages
        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        // Set up a click listener for the send button
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the text from the message input field
                String message = edCreateMessage.getText().toString();

                // If the message is not empty, clear the input field and send the message through the chatUtils object
                if (!message.isEmpty()) {
                    edCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });
    }

    // This method initializes Bluetooth functionality for the activity
    private void initBluetooth() {
        // Get the default Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If there is no Bluetooth adapter, display a toast message indicating that Bluetooth is not available
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No bluetooth found", Toast.LENGTH_SHORT).show();
        }
    }

    // This method is called to create the options menu for the activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu layout and add it to the menu parameter
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);

        // Call the parent method and return its result
        return super.onCreateOptionsMenu(menu);
    }

    // This method is called when an item in the options menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // If the "Search Devices" menu item is selected, call the checkPermissions method
            case R.id.menu_search_devices:
                checkPermissions();
                return true;
            // If the "Enable Bluetooth" menu item is selected, call the enableBluetooth method
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            // For any other menu item, call the parent method and return its result
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // This method checks if the ACCESS_FINE_LOCATION permission has been granted, and requests it if necessary
    private void checkPermissions() {
        // If the ACCESS_FINE_LOCATION permission has not been granted, request it from the user
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } 
        // If the permission has already been granted, start the DeviceListActivity to search for nearby Bluetooth devices
        else {
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    // This method is called when the DeviceListActivity finishes and returns a result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the result is from the DeviceListActivity and is successful, connect to the selected device
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            // Get the MAC address of the selected device from the intent data
            String address = data.getStringExtra("deviceAddress");
            // Connect to the selected device using the ChatUtils object
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        // Call the parent method and pass along the parameters
        super.onActivityResult(requestCode, resultCode, data);
    }

    // This function is called when the user grants or denies a runtime permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Check if the location permission was requested
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            // If permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Start the DeviceListActivity to search for available devices
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            } else {
                // If permission was denied, show a dialog asking for permission
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // If the "Grant" button is clicked, request the permission again
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // If the "Deny" button is clicked, finish the MainActivity
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // This function enables Bluetooth and makes the device discoverable if it isn't already.
    private void enableBluetooth() {
        // If Bluetooth is not enabled, enable it
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        // If the device is not discoverable, make it discoverable for 300 seconds (5 minutes)
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }
    }

    // This function is called when the MainActivity is destroyed, and stops the chatUtils service if it isn't already.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If the ChatUtils object is not null, stop the connection
        if (chatUtils != null) {
            chatUtils.stop();
        }
    }

}
