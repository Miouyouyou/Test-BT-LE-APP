package com.miouyouyou.bluetooth_master_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.miouyouyou.bluetooth_master_app.MyyActivity.MYY_REQUEST_FOR_BT;

public class MyyDeviceCommunicationActivity extends AppCompatActivity {

    final public static String MYY_BT_DEVICE_ADDRESS = "MYY_BT_DEVICE_ADDRESS";
    final public static String MYY_TAG = "Myy-DevCommActivity";

    BluetoothAdapter bluetooth_adapter;
    BluetoothDevice remote_device;
    TextView text_connection_status;
    Button button_connect;
    String remote_address;

    final BluetoothGattCallback cb_bluetooth_gatt = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final String action;
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:    action = "Connecting";    break;
                case BluetoothGatt.STATE_CONNECTED:     action = "Connected";     break;
                case BluetoothGatt.STATE_DISCONNECTING: action = "Disconnecting"; break;
                case BluetoothGatt.STATE_DISCONNECTED:  action = "Disconnected";  break;
                default: action = "Unknown";
            }
            final String full_status_string =
                    String.format("%s (%x)", action, status);
            Log.e(MYY_TAG, full_status_string);

            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   text_connection_status.setText(full_status_string);
               }
            });

            gatt.discoverServices();

            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.close();
                gatt.disconnect();
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(MYY_TAG, "Got the services !");
                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {
                    Log.e(MYY_TAG, String.format("Service discovered : %s", service.getUuid().toString()));
                }
            }
            else Log.e(MYY_TAG, "Didn't get any service back...");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_device_communication);

        final Intent received_intent = getIntent();
        remote_address = received_intent.getStringExtra("MYY_BT_DEVICE_ADDRESS");

        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        text_connection_status = findViewById(R.id.myy_bt_connection_status);
        button_connect = findViewById(R.id.myy_button_connect);
        text_connection_status.setText("Not connected");

    }

    @Override
    protected void onResume() {
        super.onResume();
        final String remote_address = this.remote_address;
        final BluetoothAdapter adapter = bluetooth_adapter;
        final BluetoothDevice remote_device = adapter.getRemoteDevice(remote_address);

        setTitle(remote_address);
        Log.e(MYY_TAG, String.format("Adapter : %s, %s", adapter.getName(), adapter.getAddress()));
        Log.e(MYY_TAG, String.format("Remote  : %s, %s", remote_device.getName(), remote_device.getAddress()));

        this.remote_device = remote_device;

        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnectingToRemote(remote_device);
                text_connection_status.setText("Connecting...");
            }
        });
    }

    public void startConnectingToRemote(BluetoothDevice remote_device) {
        /* This should not happen */
        if (bluetooth_adapter == null) {
            Log.e(MYY_TAG, "The adapter is NULL !?");
            return;
        }

        /* Enable the adapter if it's not enabled, first */
        if (!bluetooth_adapter.isEnabled()) {
            text_connection_status.setText("Enabling Bluetooth Device...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MYY_REQUEST_FOR_BT);
        }
        else { /* Start scanning directly if it's already enabled */
            text_connection_status.setText("Connecting....");
            connectToRemote(remote_device);
        }
    }

    public void connectToRemote() {
        connectToRemote(remote_device);
    }
    public void connectToRemote(BluetoothDevice remote) {
        Log.e(MYY_TAG, String.format("Connecting to : %s, %s", remote.getAddress(), remote.getName()));
        text_connection_status.setText("Connecting....");
        remote_device.connectGatt(this, false, cb_bluetooth_gatt);

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == MYY_REQUEST_FOR_BT) {
            if (resultCode == Activity.RESULT_OK) {
                text_connection_status.setText("Connecting....");
                connectToRemote();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                text_connection_status.setText("Bluetooth not enabled.");
            }
        }
    }

}
