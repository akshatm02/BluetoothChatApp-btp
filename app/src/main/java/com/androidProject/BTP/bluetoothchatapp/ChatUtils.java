// This is a Java code file for an Android Bluetooth chat app
// It provides a chat functionality over Bluetooth between two Android devices
// The file contains classes to handle connection, sending, and receiving messages
// It also defines a UUID and an app name used in Bluetooth communication

// Import statements for required Android classes and libraries
package com.androidProject.BTP.bluetoothchatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

// ChatUtils is a class that manages all the Bluetooth connections and message exchanges
public class ChatUtils {
    // Private member variables for ChatUtils class
    private Context context;                    // Context for the application
    private final Handler handler;              // Handler for the thread
    private BluetoothAdapter bluetoothAdapter;  // Bluetooth adapter for the device
    private ConnectThread connectThread;        // Thread for connecting to a device
    private AcceptThread acceptThread;          // Thread for accepting incoming connections
    private ConnectedThread connectedThread;    // Thread for connected devices

    // Public constants for states of Bluetooth connection
    public static final int STATE_NONE = 0;               // No connection established
    public static final int STATE_LISTEN = 1;             // Listening for incoming connections
    public static final int STATE_CONNECTING = 2;         // Attempting to connect to a device
    public static final int STATE_CONNECTED = 3;          // Connected to a device

    // Private variable to hold the current state of the Bluetooth connection
    private int state;

    // Constructor for ChatUtils class
    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        
        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // Get the default Bluetooth adapter for the device
    }

    // Getter method for the state of the Bluetooth connection
    public int getState() {
        return state;
    }

    // Setter method for the state of the Bluetooth connection
    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    // Method to start accepting incoming connections
    private synchronized void start() {
        // If there is a connectThread already, cancel it
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        // If there is no acceptThread, create and start a new one
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        
        // If there is a connectedThread already, cancel it
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        
        setState(STATE_LISTEN);  // Set the state to STATE_LISTEN
    }
    // Method to stop all threads and reset the state
    public synchronized void stop() {
        if (connectThread != null) {    // If there is an existing ConnectThread, cancel it
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {    // If there is an existing AcceptThread, cancel it
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) { // If there is an existing ConnectedThread, cancel it
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE); // Set the state to STATE_NONE to indicate that there is no active connection
    }
    
    /**
    Attempts to establish a Bluetooth connection with the specified device.
    If the state is already STATE_CONNECTING, cancels the current connection attempt
    and starts a new one with the new device.
    @param device the Bluetooth device to connect to
    */
    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) { // if already attempting to connect, cancel and start new connection
            connectThread.cancel();
            connectThread = null;
        }
        
        // create and start new connection thread
        connectThread = new ConnectThread(device);
        connectThread.start();
        
        // cancel any previous connected thread and set state to connecting
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_CONNECTING);
    }

    // Method to write data to the connected device
    public void write(byte[] buffer) {
        ConnectedThread connThread;
        // Synchronize on the BluetoothService instance to avoid race conditions
        synchronized (this) {
            // If not currently connected, return without writing
            if (state != STATE_CONNECTED) {
                return;
            }
            // Otherwise, assign the currently connected thread to connThread variable
            connThread = connectedThread;
        }
        // Call the write method of the connected thread to send the data to the device
        connThread.write(buffer);
    }

    // Thread class for accepting incoming connections
    // Define a private class "AcceptThread" that extends the Thread class
    private class AcceptThread extends Thread {
        
        // Declare a BluetoothServerSocket object to be used for incoming connections
        private BluetoothServerSocket serverSocket;

        // Constructor for the AcceptThread class
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Attempt to create a new BluetoothServerSocket object with a specific UUID and app name
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }

            // Set the serverSocket variable to the newly created BluetoothServerSocket object
            serverSocket = tmp;
        }

        // Method that is executed when the AcceptThread is started
        public void run() {
            BluetoothSocket socket = null;

            // Attempt to accept incoming connections from other Bluetooth devices
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept->Run", e.toString());

                // If there is an error accepting incoming connections, close the serverSocket object
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept->Close", e.toString());
                }
            }

            // If a connection is successfully established, determine the app's current connection state and take appropriate action
            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {

                            // If the app is not in a listen or connecting state, close the socket object
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }

        // Method used to close the serverSocket object and stop listening for incoming connections
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }



    // Define a private class "ConnectThread" that extends the Thread class
    private class ConnectThread extends Thread {
        
        // Declare a BluetoothSocket object and a BluetoothDevice object for use in establishing a connection
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        // Constructor for the ConnectThread class
        public ConnectThread(BluetoothDevice device) {
            
            // Set the device variable to the device passed in to the constructor
            this.device = device;

            BluetoothSocket tmp = null;

            // Attempt to create a new BluetoothSocket object with a specific UUID for the given device
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            // Set the socket variable to the newly created BluetoothSocket object
            socket = tmp;
        }

        // Method that is executed when the ConnectThread is started
        public void run() {
            
            // Attempt to connect to the remote device
            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("Connect->Run", e.toString());

                // If there is an error connecting to the remote device, close the socket object and call the connectionFailed() method
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
                connectionFailed();
                return;
            }

            // If the connection is successful, set the connectThread variable to null and call the connected() method with the socket and device objects
            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connected(socket, device);
        }

        // Method used to close the socket object and stop attempting to connect to the remote device
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }


    // Define a private method called connectionLost() that sends a message to the main thread indicating that the connection to the remote device has been lost
    private void connectionLost() {
        
        // Create a new message object and obtain a reference to the handler object in the main thread
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        
        // Create a new bundle object and put a string value indicating that the connection has been lost
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        
        // Set the bundle object as the data for the message object
        message.setData(bundle);
        
        // Send the message to the handler object in the main thread
        handler.sendMessage(message);

        // Call the start() method of the ChatUtils object to restart the thread for accepting incoming connections
        ChatUtils.this.start();
    }


    // Define a private synchronized method called connectionFailed() that sends a message to the main thread indicating that the connection to the remote device failed
    private synchronized void connectionFailed() {
        
        // Create a new message object and obtain a reference to the handler object in the main thread
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        
        // Create a new bundle object and put a string value indicating that the connection failed
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device");
        
        // Set the bundle object as the data for the message object
        message.setData(bundle);
        
        // Send the message to the handler object in the main thread
        handler.sendMessage(message);

        // Call the start() method of the ChatUtils object to restart the thread for accepting incoming connections
        ChatUtils.this.start();
    }


    // Define a private synchronized method called connected() that handles the connected state of the Bluetooth chat application
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        
        // Cancel any existing connectThread or connectedThread
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Create a new connectedThread and start it
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Create a new message object and obtain a reference to the handler object in the main thread
        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        
        // Create a new bundle object and put the name of the remote device in it
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        
        // Set the bundle object as the data for the message object
        message.setData(bundle);
        
        // Send the message to the handler object in the main thread
        handler.sendMessage(message);

        // Set the state of the Bluetooth chat application to connected
        setState(STATE_CONNECTED);
    }

}
