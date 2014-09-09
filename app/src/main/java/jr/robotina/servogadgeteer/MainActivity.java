package jr.robotina.servogadgeteer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private static final int MAX_SPEED = 100;
    private float lastY = 0;
    private int touch = 0;
    private static final int MOVE_THRESHOLD = 3;
    private static final int MOVE_SENSIBILITY = 6;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothThread mThread;
    private BluetoothDevice mPairedDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        senSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        ((Button)findViewById(R.id.start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = ((Button)findViewById(R.id.start)).getText().toString();
                if (text == "Drive!") {
                    if (startMethod())
                        ((Button)findViewById(R.id.start)).setText("Kill Thread");
                } else {
                    ((Button)findViewById(R.id.start)).setText("Drive!");
                    finishMethod();
                }

            }
        });
    }

   private void finishMethod() {
       if(mThread != null)
           mThread.closeConnection();
   }

    private boolean startMethod(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Get First Device

            mPairedDevice = (BluetoothDevice)pairedDevices.toArray()[0];
            if(mThread != null) {
                mThread.closeConnection();
            }

            Toast.makeText(this,"Thread Start",Toast.LENGTH_SHORT).show();

            mThread = new BluetoothThread(mPairedDevice);
            mThread.start();
            return true;
        }
        else {
            Toast.makeText(this,"No paired devices",Toast.LENGTH_SHORT).show();
            return false;
        }


    }

    private void sendData(double speedL, double speedR, String dir) {
        if (dir.equalsIgnoreCase("F")) {
            speedL = Math.min(Math.max(speedL, 0), MAX_SPEED);
            speedR = Math.min(Math.max(speedR, 0), MAX_SPEED);
        } else if (dir.equalsIgnoreCase("B")) {
            speedL = Math.min(Math.max(speedL, -MAX_SPEED), 0);
            speedR = Math.min(Math.max(speedR, -MAX_SPEED), 0);
        } else {
            speedL = Math.min(Math.max(speedL, -MAX_SPEED), MAX_SPEED);
            speedR = Math.min(Math.max(speedR, -MAX_SPEED), MAX_SPEED);
        }

        if (mPairedDevice != null && mThread != null && mThread.connected) {
            Log.d("SPEED", (int) speedL + ";" + (int) speedR);
            mThread.sendData((int) speedL + ";" + (int) speedR);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float y = sensorEvent.values[1];

            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                lastUpdate = curTime;
                lastY = y;

                ((TextView)findViewById(R.id.yTextView)).setText("Y = " + y);
                if (touch == 1) {
                    if (lastY < -MOVE_THRESHOLD) {
                        ((TextView)findViewById(R.id.action)).setText("MOVING FORWARD LEFT");
                        // lastY < 0
                        sendData(MAX_SPEED*(1+(lastY+MOVE_THRESHOLD)/MOVE_SENSIBILITY), MAX_SPEED, "F");
                    } else if (lastY > MOVE_THRESHOLD) {
                        ((TextView)findViewById(R.id.action)).setText("MOVING FORWARD RIGHT");
                        // lastY > 0
                        sendData(MAX_SPEED, MAX_SPEED*(1-(lastY-MOVE_THRESHOLD)/MOVE_SENSIBILITY), "F");
                    } else {
                        ((TextView)findViewById(R.id.action)).setText("MOVING FORWARD");
                        sendData(MAX_SPEED, MAX_SPEED, "F");
                    }
                } else if (touch == 2) {
                    if (lastY < -MOVE_THRESHOLD) {
                        ((TextView)findViewById(R.id.action)).setText("MOVING BACKWARD LEFT");
                        // lastY < 0
                        sendData(-MAX_SPEED*(1+(lastY+MOVE_THRESHOLD)/MOVE_SENSIBILITY), -MAX_SPEED, "B");
                    } else if (lastY > MOVE_THRESHOLD) {
                        ((TextView)findViewById(R.id.action)).setText("MOVING BACKWARD RIGHT");
                        // lastY > 0
                        sendData(-MAX_SPEED, -MAX_SPEED*(1-(lastY-MOVE_THRESHOLD)/MOVE_SENSIBILITY), "B");
                    } else {
                        ((TextView)findViewById(R.id.action)).setText("MOVING BACKWARD");
                        sendData(-MAX_SPEED, -MAX_SPEED, "B");
                    }
                } else {
                    ((TextView)findViewById(R.id.action)).setText("STOP");
                    if (Math.abs(lastY) < MOVE_THRESHOLD) {
                        sendData(0,0,"N");
                    } else {
                        sendData(MAX_SPEED*(lastY*Math.signum(lastY)-MOVE_THRESHOLD)/MOVE_SENSIBILITY,
                                -MAX_SPEED*(lastY*Math.signum(lastY)-MOVE_THRESHOLD)/MOVE_SENSIBILITY,
                                "N");
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action= ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP: {
                touch = 0;
                break;
            }
            default: {
                touch = ev.getPointerCount();
                break;
            }
        }
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
