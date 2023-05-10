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

import java.util.Iterator;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private ListView listPairedDevices, listAvailableDevices;
    private ProgressBar progressScanDevices;

    private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;

        init();
    }

    private void init() {
//        listPairedDevices = findViewById(R.id.list_paired_devices);
//        listAvailableDevices = findViewById(R.id.list_available_devices);
//        progressScanDevices = findViewById(R.id.progress_scan_devices);

//        adapterPairedDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);
//        adapterAvailableDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);

//        listPairedDevices.setAdapter(adapterPairedDevices);
//        listAvailableDevices.setAdapter(adapterAvailableDevices);

//        listAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                String info = ((TextView) view).getText().toString();
//                String address = info.substring(info.length() - 17);
//
//                Intent intent = new Intent();
//                intent.putExtra("deviceAddress", address);
//                setResult(RESULT_OK, intent);
//                finish();
//            }
//        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

//        if (pairedDevices != null && pairedDevices.size() > 0) {
//            for (BluetoothDevice device : pairedDevices) {
//                adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
//            }
//        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);

//        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                bluetoothAdapter.cancelDiscovery();
//
//                String info = ((TextView) view).getText().toString();
//                String address = info.substring(info.length() - 17);
//
//                Log.d("Address", address);
//                System.out.println("address    "+address);
//
//                Intent intent = new Intent();
//                intent.putExtra("deviceAddress", address);
//
//                setResult(Activity.RESULT_OK, intent);
//                finish();
//            }
//        });
//        boolean deviceSelected = false; // Initialize a flag variable to keep track of whether a device has been successfully selected
//
//        for (int i = 0; i < 10 && !deviceSelected; i++) { // Iterate through the first 10 items in the list until a device is selected or the end of the list is reached
//            listPairedDevices.setSelection(i); // Select the current item
//
//            View selectedItem = listPairedDevices.getSelectedView(); // Get the selected item
//            String info = ((TextView) selectedItem).getText().toString();
//            String address = info.substring(info.length() - 17); // Extract the device address
//
//            // Try to connect to the selected device here, and set deviceSelected to true if successful
//            // ...
//
//            if (deviceSelected) {
//                // If a device has been successfully selected, log its address and break out of the loop
//                Log.d("Selected Address", address);
//                break;
//            }
//        }
//
//        if (!deviceSelected) {
//            // If no device was successfully selected, log an error message
//            Log.e("Device Selection Error", "No devices were successfully selected");
//        }
        // Select the first item in the list
        bluetoothAdapter.cancelDiscovery();

//        for (BluetoothDevice device : pairedDevices) {
//            adapterPairedDevices.add(device.getName() + "\n" + device.getAddress());
//            System.out.println(device.getName() + " " + device.getAddress());
//        }
//        System.out.println(listPairedDevices.getCount());
//        listPairedDevices.setSelection(0);
//
        // Get the selected item

//        View selectedItem = listPairedDevices.getSelectedView();
//        Iterator<BluetoothDevice> it = pairedDevices.iterator();
//        for (Integer element : myList) {
//            // do something with the element
//        }
        for( BluetoothDevice device: pairedDevices){
//            BluetoothDevice firstElement = it.next();
            System.out.println("The first element is: " + device.getAddress() + " and its device: "+device.getName());
            String address = device.getAddress();
            Log.d("Selected Address", address);

            Intent intent = new Intent();
            intent.putExtra("deviceAddress", address);

            setResult(Activity.RESULT_OK, intent);
            finish();
            System.out.println("Hey shivam, complete this shit soon!");

        }
//        finish();
//        if (it.hasNext()) {
//            BluetoothDevice firstElement = it.next();
//            System.out.println("The first element is: " + firstElement.getAddress());
//            String address = firstElement.getAddress();
//            Log.d("Selected Address", address);
//
//            Intent intent = new Intent();
//            intent.putExtra("deviceAddress", address);
//
//            setResult(Activity.RESULT_OK, intent);
//            finish();
//        }

//        if(selectedItem != null){
//            String info = ((TextView) selectedItem).getText().toString();
//            String address = info.substring(info.length() - 17);
//
//            // Log the selected item's address
//            Log.d("Selected Address", address);
//
//            Intent intent = new Intent();
//            intent.putExtra("deviceAddress", address);
//
//            setResult(Activity.RESULT_OK, intent);
//            finish();
//        }
//        else{
//            System.out.println("Not executing");
//            Toast.makeText(context, "No VIEW found", Toast.LENGTH_SHORT).show();
//        }


    }

    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressScanDevices.setVisibility(View.GONE);
                if (adapterAvailableDevices.getCount() == 0) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Click on the device to start the chat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan_devices:
                scanDevices();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scanDevices() {
        progressScanDevices.setVisibility(View.VISIBLE);
        adapterAvailableDevices.clear();
        Toast.makeText(context, "Scan started", Toast.LENGTH_SHORT).show();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothDeviceListener != null) {
            unregisterReceiver(bluetoothDeviceListener);
        }
    }
}
