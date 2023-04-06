package com.androidProject.BTP.bluetoothchatapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
   // UI elements
   private ListView listPairedDevices, listAvailableDevices;
   private ProgressBar progressScanDevices;

   // Adapters for list views
   private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;

   private Context context;
   private BluetoothAdapter bluetoothAdapter;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_device_list);
       context = this;

       // Initialize UI elements and adapters
       init();
   }

   private void init() {
       listPairedDevices = findViewById(R.id.list_paired_devices);
       listAvailableDevices = findViewById(R.id.list_available_devices);
       progressScanDevices = findViewById(R.id.progress_scan_devices);

       adapterPairedDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);
       adapterAvailableDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);

       listPairedDevices.setAdapter(adapterPairedDevices);
       listAvailableDevices.setAdapter(adapterAvailableDevices);

       // Listener for selecting an available device
       listAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               // Get the device address from the view and return it to the calling activity
               String info = ((TextView) view).getText().toString();
               String address = info.substring(info.length() - 17);

               Intent intent = new Intent();
               intent.putExtra("deviceAddress", address);
               setResult(RESULT_OK, intent);
               finish();
           }
       });

       // Get the default Bluetooth adapter and paired devices
       bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

       // Add paired devices to the adapter
       if (pairedDevices != null && pairedDevices.size() > 0) {
           for (BluetoothDevice device : pairedDevices) {
               adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
           }
       }

       // Register broadcast receivers for discovering new devices and finishing discovery
       IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
       registerReceiver(bluetoothDeviceListener, intentFilter);
       IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
       registerReceiver(bluetoothDeviceListener, intentFilter1);

       // Listener for selecting a paired device
       listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               // Cancel discovery, get the device address from the view and return it to the calling activity
               bluetoothAdapter.cancelDiscovery();

               String info = ((TextView) view).getText().toString();
               String address = info.substring(info.length() - 17);

               Log.d("Address", address);

               Intent intent = new Intent();
               intent.putExtra("deviceAddress", address);

               setResult(Activity.RESULT_OK, intent);
               finish();
           }
       });
   }

    // Initialize a BroadcastReceiver to listen for Bluetooth device discovery events
    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // If a new device is discovered, add it to the list of available devices
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Only add the device to the list if it's not already bonded
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                }
            } 
            // If the discovery process finishes, hide the progress bar and display a message if no devices were found
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressScanDevices.setVisibility(View.GONE);
                if (adapterAvailableDevices.getCount() == 0) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Click on the device to start the chat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    // Inflate the menu layout for the device list screen
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan_devices:
                // When the "Scan Devices" button is pressed, start scanning for available devices
                scanDevices();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Method to initiate device discovery and display a progress bar while scanning
    private void scanDevices() {
        progressScanDevices.setVisibility(View.VISIBLE);
        adapterAvailableDevices.clear();
        Toast.makeText(context, "Scan started", Toast.LENGTH_SHORT).show();

        // If the device is already discovering, cancel the discovery process to restart it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Start discovering nearby Bluetooth devices
        bluetoothAdapter.startDiscovery();
    }

    // Unregister the BroadcastReceiver when the activity is destroyed to prevent memory leaks
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothDeviceListener != null) {
            unregisterReceiver(bluetoothDeviceListener);
        }
    }

}
