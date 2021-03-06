package jr.robotina.servogadgeteer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.plus.PlusShare;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    InputStream inputStream;
    Handler mImageHandler;
    ImageView mImageView;
    Context mContext;
    final String TAG = "BT";
    final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothThread(BluetoothDevice d, Handler handler, ImageView imageVew, Context context) {
        pairedDevice = d;
        connected = false;
        mImageHandler = handler;
        mImageView = imageVew;
        mContext = context;
    }

    @Override
    public void run() {
        try {
            connectToSocket();
            while (connected) {
                int b1 = inputStream.read();
                int b2 = inputStream.read();
                int b3 = inputStream.read();
                int length = b1 * 256 * 256 + b2 * 256 + b3;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[16384];

                while (length > 0) {
                    int bytesRead = inputStream.read(buffer, 0, buffer.length);
                    baos.write(buffer, 0, bytesRead);
                    length -= bytesRead;
                }

                final byte[] bytes = baos.toByteArray();
                Log.d(TAG, "Bytes: " + bytes.length);
                final Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mImageHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(bmp);

                        /*File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
                        try {
                            f.createNewFile();
                            FileOutputStream fo = new FileOutputStream(f);
                            //fo.write(bytes.toByteArray());
                            fo.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.jpg"));
                        mContext.startActivity(Intent.createChooser(share, "Share Image"));*/
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error: ", e);
            //socketError();
        }

    }
    /*
    public void run() {
        connectToSocket();
        byte[] buffer = new byte[1024];
        while(connected) {
            Log.d(TAG, "Thread Running");
            try {
                Log.d(TAG, "Buffer size: " + inputStream.read(buffer));
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

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
            inputStream  = mSocket.getInputStream();
            Log.d(TAG,"Connected" );
        } catch (IOException e) {
            Log.e(TAG,"Error",e);
            e.printStackTrace();
            connected  = false;
        }
        connected = true;
    }
}