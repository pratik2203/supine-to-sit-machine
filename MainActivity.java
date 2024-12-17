package com.klecet.Supine2Sit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.klecet.supine2sit.R;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    private static final String TAG = "spine2sit";

    private BluetoothSocket socket;

    private OutputStream os;
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver bluetoothReceiver;
    public String deviceName;
    public String deviceAddress;
    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    TextView nameView;


    Button forwardbutton, backwardbutton, stopbutton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameView = (TextView) findViewById(R.id.controllerName);


        forwardbutton = (Button) findViewById(R.id.fb);
        backwardbutton = (Button) findViewById(R.id.bb);
        stopbutton = (Button) findViewById(R.id.stb);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    // A Bluetooth device is connected
                    Toast.makeText(context, "Bluetooth device connected", Toast.LENGTH_SHORT).show();
                }


            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(bluetoothReceiver, filter);

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), R.string.Bluetooth_NA, Toast.LENGTH_LONG).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivity(enableBtIntent);
        }

        forwardbutton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "UP");
                    sendSignal('U');
                }
                return true;
            }
        });

        backwardbutton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "DOWN");
                    sendSignal('D');
                }
                return true;
            }
        });

        stopbutton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "Stop");
                    sendSignal('S');
                }
                return true;
            }
        });

        checkPermissions();


    }

    private void checkPermissions(){
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    1
            );
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        deviceName = preferences.getString("controllerName", "NA");
        deviceAddress = preferences.getString("controllerAddress", "");
        nameView.setText("Device " + "\"" + deviceName + "\"");
        if (!deviceAddress.equals("")) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                //nameView.setText("Connected to " + "\"" + "NA" + "\"");
                //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            mBluetoothAdapter.cancelDiscovery();
            if(socket != null){
                try {
                    socket.connect();
                } catch (IOException e) {
                    //nameView.setText("Connected to " + "\"" + "NA" + "\"");
                    //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "socket connect failed: " + e.getMessage() + "\n");
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        Log.e(TAG, "socket closing failed: " + e1.getMessage() + "\n");
                        //Toast.makeText(getApplicationContext(), e1.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
            if (socket != null){
                try {
                    os = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "getting output stream failed: " + e.getMessage() + "\n");
                    //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        else {
            Toast.makeText(getApplicationContext(),"Connect to a device",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (os != null && socket.isConnected()) {
            try {
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "flushing output stream failed: " + e.getMessage() + "\n");
                Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
        try {
            if(socket != null)
                socket.close();
        } catch (IOException e) {
            Log.e(TAG, "closing socket failed: " + e.getMessage() + "\n");
            Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver when the activity is destroyed
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }
    }

    public void sendSignal(char message) {
        if(socket != null){
            if(socket.isConnected()) {
                try {
                    if (os != null){
                        os.write(message);
                        Log.d("OS Message : ", String.valueOf(message));
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(getBaseContext(), "Connect to the selected bluetooth device first", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getBaseContext(), "Connect to the selected bluetooth device first", Toast.LENGTH_LONG).show();
        }
    }

    public void selectFromList(View view) {
        Intent intent = new Intent(this,SelectController.class);
        startActivity(intent);
    }


}
