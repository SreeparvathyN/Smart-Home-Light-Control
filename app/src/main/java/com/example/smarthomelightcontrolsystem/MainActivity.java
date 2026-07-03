package com.example.smarthomelightcontrolsystem;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnLivingOn, btnLivingOff, btnBedroomOn, btnBedroomOff, btnAllOn, btnAllOff;
    TextView status;
    View statusDot;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;

    String deviceAddress = "00:25:01:31:10:D3";

    boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLivingOn = findViewById(R.id.btnLivingOn);
        btnLivingOff = findViewById(R.id.btnLivingOff);
        btnBedroomOn = findViewById(R.id.btnBedroomOn);
        btnBedroomOff = findViewById(R.id.btnBedroomOff);
        btnAllOn = findViewById(R.id.btnAllOn);
        btnAllOff = findViewById(R.id.btnAllOff);
        status = findViewById(R.id.status);
        statusDot = findViewById(R.id.statusDot);

        updateStatusUI("Initializing...", R.color.statusWaiting);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // BUTTONS
        btnLivingOn.setOnClickListener(v -> sendData("A"));
        btnLivingOff.setOnClickListener(v -> sendData("a"));

        btnBedroomOn.setOnClickListener(v -> sendData("B"));
        btnBedroomOff.setOnClickListener(v -> sendData("b"));

        btnAllOn.setOnClickListener(v -> sendData("C"));
        btnAllOff.setOnClickListener(v -> sendData("c"));

        // Auto connect once app starts
        if (checkPermissions()) {
            connectBluetooth();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBluetooth();
            } else {
                updateStatusUI("Permission Denied", R.color.statusDisconnected);
            }
        }
    }

    private void updateStatusUI(String message, int colorResId) {
        runOnUiThread(() -> {
            status.setText("Bluetooth Status: " + message);
            statusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
        });
    }

    private void connectBluetooth() {

        new Thread(() -> {
            try {
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    updateStatusUI("Bluetooth OFF", R.color.statusDisconnected);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    updateStatusUI("Permission Missing", R.color.statusDisconnected);
                    return;
                }

                // Look for HC-05 in paired devices
                BluetoothDevice device = null;
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                StringBuilder foundDevices = new StringBuilder();
                
                if (pairedDevices != null && !pairedDevices.isEmpty()) {
                    for (BluetoothDevice d : pairedDevices) {
                        String name = d.getName();
                        String address = d.getAddress();
                        foundDevices.append(name != null ? name : "Unknown").append(", ");
                        
                        if ((name != null && name.equalsIgnoreCase("HC-05")) || address.equals(deviceAddress)) {
                            device = d;
                            break;
                        }
                    }
                }

                if (device == null) {
                    String msg = foundDevices.length() > 0 ? 
                        "HC-05 not found. Found: " + foundDevices.toString() : 
                        "No paired devices found.";
                    updateStatusUI(msg, R.color.statusDisconnected);
                    return;
                }

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothAdapter.cancelDiscovery();

                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                updateStatusUI("Connected", R.color.statusConnected);

            } catch (Exception e) {
                isConnected = false;
                Log.e("BluetoothError", "Connection failed", e);
                updateStatusUI("Error: " + e.getMessage(), R.color.statusDisconnected);
            }
        }).start();
    }

    private void sendData(String data) {

        new Thread(() -> {
            try {

                if (!isConnected || outputStream == null) {
                    updateStatusUI("Trying to reconnect...", R.color.statusWaiting);
                    connectBluetooth();
                    Thread.sleep(1000);
                }

                if (outputStream != null) {
                    outputStream.write(data.getBytes());
                    updateStatusUI("Sent: " + data, R.color.statusConnected);
                }

            } catch (Exception e) {
                updateStatusUI("Bluetooth Error", R.color.statusDisconnected);
            }
        }).start();
    }
}