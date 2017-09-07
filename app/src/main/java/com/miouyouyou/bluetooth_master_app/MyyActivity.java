package com.miouyouyou.bluetooth_master_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MyyActivity extends AppCompatActivity {

    /* Utilities */
    private void grillePainRapide(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /* 1 is arbitrarily defined. You can use whatever non 0 positive value you want */
    final public static int MYY_REQUEST_FOR_BT = 1;
    final public static int MYY_SCAN_PERIOD_MS = 10000;

    final String MYY_LOG_TAG = "Miouyouyou-BT";

    Button button_start_scan, button_stop_scan;
    /* UI */

    boolean scanning = false;
    ListView list_bluetooth_last_scan_results;

    BluetoothAdapter bluetooth_adapter;
    ArrayList<BluetoothDevice> bluetooth_last_scan_results = new ArrayList<>(10);

    /* Our code */
    /* The callback used when scanning. This receives all the results of the scan */
    private BluetoothAdapter.LeScanCallback bt_scan_callback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void
                onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device != null) {
                        Log.e(MYY_LOG_TAG, String.format("Name : %s, Addr : %s\n", device.getName(), device.getAddress()));
                    }
                    else Log.e(MYY_LOG_TAG, "Wut ?");
                    got_device(device);
                }
            };

    /* Used to schedule the scan timeout. */
    private Handler scheduler = new Handler();
    /* Used to stop scanning after MYY_SCAN_PERIOD_MS */
    private final Runnable stop_bluetooth_scan = new Runnable() {
        @Override
        public void run() {
            Log.e(MYY_LOG_TAG,"Stopping the scan...");
            if (scanning) {
                bluetooth_adapter.stopLeScan(bt_scan_callback);
                scanning(false);
                grillePainRapide("Scan stopped !");
            }
        }
    };

    protected void toggleScanButton(final boolean scanning) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    button_start_scan.setVisibility(View.GONE);
                    button_stop_scan.setVisibility(View.VISIBLE);
                }
                else {
                    button_start_scan.setVisibility(View.VISIBLE);
                    button_stop_scan.setVisibility(View.GONE);
                }
            }
        });
    }
    protected void scanning(boolean state) {
        toggleScanButton(state);
        scanning = state;
    }

    protected void scanForDevices() {
        scanForDevices(bluetooth_adapter);
    }
    protected void scanForDevices(BluetoothAdapter adapter) {
        /* This should not happen */
        if (adapter == null) {
            Log.e(MYY_LOG_TAG, "The adapter is NULL !?");
            return;
        }

        grillePainRapide("Scanning !");
        Log.e(MYY_LOG_TAG, "scanForDevices !");
        /* Schedule the scan timeout */
        scheduler.postDelayed(stop_bluetooth_scan, MYY_SCAN_PERIOD_MS);
        /* Start scanning. LeScan is not some meme-ish mangled French name. It's Low Energy Scan */
        adapter.startLeScan(bt_scan_callback);
        scanning(true);
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == MYY_REQUEST_FOR_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth activated !", Toast.LENGTH_SHORT).show();
                scanForDevices();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, ">:C Activate the Bluetooth already !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void requestScanForDevices(BluetoothAdapter adapter) {
        /* This should not happen */
        if (adapter == null) {
            Log.e(MYY_LOG_TAG, "The adapter is NULL !?");
            return;
        }

        /* Enable the adapter if it's not enabled, first */
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MYY_REQUEST_FOR_BT);
        }
        else { /* Start scanning directly if it's already enabled */
            scanForDevices(adapter);
        }
    }

    public void start_scan_request(View view) {
        requestScanForDevices(bluetooth_adapter);
    }

    public void stop_scan_request(View view) {
        stop_bluetooth_scan.run();
    }

    public void got_device(final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
           @Override
           public void run() {
               if (!bluetooth_last_scan_results.contains(device))
                   bluetooth_last_scan_results.add(device);
               ((BaseAdapter) list_bluetooth_last_scan_results.getAdapter()).notifyDataSetChanged();
           }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myy);

        final ListView list_scan_results    = findViewById(R.id.myy_bt_scan_results);
        ArrayListAdapter list_adapter_scan_results =
                new ArrayListAdapter(bluetooth_last_scan_results, getLayoutInflater());
        list_scan_results.setAdapter(list_adapter_scan_results);

        /* -- To remove -- */
        BluetoothAdapter the_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        ActionBar action_bar = getSupportActionBar();
        if (action_bar != null) {
            action_bar.setTitle(the_bluetooth_adapter.getName());
            action_bar.setSubtitle(the_bluetooth_adapter.getAddress());
        }

        /* -- --------- -- */

        list_bluetooth_last_scan_results = list_scan_results;
        bluetooth_adapter = the_bluetooth_adapter;
        button_start_scan = findViewById(R.id.myy_button_start_bt_scan);
        button_stop_scan  = findViewById(R.id.myy_button_stop_bt_scan);
    }

    class BluetoothScanResult {
        String address;
        String name;
        final TextView text_bt_found_address;
        final TextView text_bt_found_name;

        BluetoothScanResult(View container) {
            text_bt_found_address = container.findViewById(R.id.myy_bt_found_address);
            text_bt_found_name    = container.findViewById(R.id.myy_bt_found_name);
        }

        void setText(BluetoothDevice device) {
            final String device_address = device.getAddress();
            final String device_name    = device.getName();
            address = device_address;
            name    = device_name;
            text_bt_found_address.setText(address);
            text_bt_found_name.setText(name);
        }
    }

    /* The listener will be simple : We just open an Activity based on the address stored in the
       tag.
     */
    final View.OnClickListener list_item_listener_scan_result = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            stop_bluetooth_scan.run();
            final Intent intent =
                    new Intent(getApplicationContext(), MyyDeviceCommunicationActivity.class);
            intent.putExtra(
                    MyyDeviceCommunicationActivity.MYY_BT_DEVICE_ADDRESS,
                    ((BluetoothScanResult) view.getTag()).address
            );
            startActivity(intent);
        }
    };

    /* This class is semi-generic semi-specific... This will do for that example though. */
    public class ArrayListAdapter extends BaseAdapter {

        LayoutInflater layout_inflater;
        ArrayList<BluetoothDevice> list_to_display;

        ArrayListAdapter(ArrayList<BluetoothDevice> list, LayoutInflater inflater) {
            list_to_display = list;
            layout_inflater = inflater;
        }

        @Override
        public Object getItem(final int position) {
            return list_to_display.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return getItem(position).hashCode();
        }

        @Override
        public int getCount() {
            return list_to_display.size();
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup container) {
            if (convertView == null) {
                convertView = layout_inflater.inflate(
                        R.layout.listitem_bluetooth_last_scan_result,
                        container,
                        false
                );
                BluetoothScanResult attached_result = new BluetoothScanResult(convertView);
                /* TAG... What a horrible name. Couldn't they name it "Private data" or something ?
                   Because, 'tags' is basically a field to store your own View related data...
                 */
                convertView.setTag(attached_result);
                convertView.setOnClickListener(list_item_listener_scan_result);
            }

            /* On the first initialization (vs recycled phases), the getTag/setTag dance is slower,
               yeah...
               However, defining view_elements before the if block and checking if it's null
               every time might be even worse...
            */
            final BluetoothScanResult scan_result = (BluetoothScanResult) convertView.getTag();

            /* This should not happen... */
            try {
                scan_result.setText((BluetoothDevice) getItem(position));
            }
            catch (Exception e) {
                /* Let's avoid crashing the whole app is something goes wrong here... */
                Log.e(MYY_LOG_TAG, "Something went wrong when listing scan results...", e);
            }
            return convertView;
        }
    }
}
