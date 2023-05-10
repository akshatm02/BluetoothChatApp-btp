package com.androidProject.BTP.bluetoothchatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class ChatUtils extends AppCompatActivity {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String APP_NAME = "BluetoothChatApp";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;
    private final int SELECT_DEVICE = 102;
    private Timer disconnectTimer;


    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
//        if (acceptThread == null) {
//            acceptThread = new AcceptThread();
//            acceptThread.start();
//        }
//        setState(STATE_LISTEN);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        System.out.println("START: In the start function now!");
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {

        //chatWindow code starts-
//        if (disconnectTimer != null) {
//            disconnectTimer.cancel();
//            disconnectTimer = null;
//        }
        // - code closed

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        System.out.println("Functioning Done for the device.");
        setState(STATE_NONE);
//        disconnect();
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        // Stop previous device's thread and create one for new device.
        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        System.out.println("Step-1-> Setting the state to state_connecting!");
        setState(STATE_CONNECTING);

        // chatWindow code starts -
        // Schedule disconnection after 10 seconds
//        disconnectTimer = new Timer();
//        disconnectTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                stop();
//                System.out.println("Hey! I am in disconnectTimer function!");
//            }
//        }, 10000);
        // - chatWindow code ends
    }

    public void write(byte[] buffer) {
        ConnectedThread connThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
//                Intent intent = new Intent(context, DeviceListActivity.class);
//                startActivityForResult(intent, SELECT_DEVICE);
                return;
//
            }

            connThread = connectedThread;
        }
        if (connThread != null) {
            connThread.write(buffer);
        }
    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
//            String address = data.getStringExtra("deviceAddress");
//            connect(bluetoothAdapter.getRemoteDevice(address));
//            setState(STATE_CONNECTED);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    public boolean isConnected() {
        if(state != STATE_CONNECTED) return true;
        return false;
    }

//    public void disconnect() {
//        try {
//            socket.close();
//        } catch (IOException e) {
//
//        }
//    }
    public void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            setState(STATE_NONE);
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
                System.out.println("ACCEPT-THREAD: listen socket created!");
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }

            serverSocket = tmp;
        }

        public void run() {
            System.out.println("ACCEPT-THREAD-RUN-step-2: I am in the run function of Accept thread!");
            BluetoothSocket socket = null;
            try {
                System.out.println("ACCEPT-THREAD-RUN-BEFORE: socket acceptance");
                socket = serverSocket.accept();
                System.out.println("ACCEPT-THREAD-RUN-AFTER: socket accepted");
            } catch (IOException e) {
                System.out.println("ACCEPT-THREAD-RUN: socket connection issue");
                Log.e("Accept->Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept->Close", e.toString());
                }
            }

            if (socket != null) {
                System.out.println("Socket is not null, IN ACCEPT THREAD!");
                switch (state) {
                    case STATE_LISTEN: System.out.println("State_listen of accept thread invoked!");
                    case STATE_CONNECTING:
                        System.out.println("Step->3: I am calling the connected function now!");
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE: System.out.println("state_none of accept thread invoked!");
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

        public void run() {
            System.out.println("Hey, I have reached the run function of connect thread.");
            try {
                System.out.println("Before: Socket connection!");
                socket.connect();       // check
                System.out.println("After: socket connection!");
            } catch (IOException e) {
                Log.e("Connect->Run", e.toString());
                Log.e("Connect->Run", e.toString());
                try {
                    System.out.println("Closing the socket for: "+device.getName());
                    socket.close();
                    if (!socket.isConnected()) {
                        System.out.println("Socket has been successfully CLOSED.");
                    } else {
                        System.out.println("Socket was not closed.");
                    }
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
                connectionFailed(device);
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Connected>Constructor", e.toString());
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }
        public void run() {
            System.out.println("Hey, I have reached the run function of connected thread after using the connected function.");
            byte[] buffer = new byte[1024];
            int bytes;
            while(true)
            {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                System.out.println(buffer);
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e("Connected->write", e.toString());
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connected->Cancel", e.toString());
            }
        }
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connectionFailed(BluetoothDevice device) {
        System.out.println("CONNECTION-FAILED: Came from connect thread socket connection issue!");
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device: "+ device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        System.out.println("step->4: Hey, I am going in MainActivity.java after setting up connected functionality for" + device.getName());
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
