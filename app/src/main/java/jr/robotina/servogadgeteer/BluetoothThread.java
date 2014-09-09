package jr.robotina.servogadgeteer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by slarrain on 09-09-14.
 */
class BluetoothThread extends Thread {

    BluetoothDevice pairedDevice;
    BluetoothSocket mSocket;
    boolean connected;
    OutputStream outputStream;
    final String TAG = "BT";
    final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothThread(BluetoothDevice d) {
        pairedDevice = d;
        connected = false;
    }

    public void run() {
        connectToSocket();
        while(connected) {
            Log.d(TAG, "Thread Running");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendData(String data) {
        Log.d(TAG, "Sending data");
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            Log.e(TAG,"Error",e);
            e.printStackTrace();
            closeConnection();
        }
        Log.d(TAG, "Success");
    }

    public void closeConnection() {
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected  = false;
    }

    protected void connectToSocket() {
        try {
            Log.d(TAG,"Starting Connection" );
            mSocket = pairedDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            outputStream = mSocket.getOutputStream();
            Log.d(TAG,"Connected" );
        } catch (IOException e) {
            Log.e(TAG,"Error",e);
            e.printStackTrace();

            connected  = false;
        }
        connected = true;
    }
}