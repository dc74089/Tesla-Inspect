package com.vegetarianbaconite.teslainspect;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

import java.util.List;

public class AutoFixer {
    private Context c;
    private Thread t;
    private Boolean fixing = false;
    private List<Integer> errs;

    public AutoFixer(Context context) {
        this.c = context;
    }

    public void fix(List<Integer> errorCodes) {
        if (fixing) return;
        this.errs = errorCodes;
        t = new Thread(fixerRunnable);
        t.start();
    }

    private Runnable fixerRunnable = new Runnable() {
        @Override
        public void run() {
            fixing = true;

            if (errs.contains(R.string.airplaneError)) {
                startSettingsForAirplaneMode();
                return;
            }

            if (errs.contains(R.string.bluetoothError)) {
                disableBluetooth();
            }

            if (errs.contains(R.string.wifiEnabledError)) {
                enableWifi();
            }

            if (errs.contains(R.string.wifiConnectedError)) {
                disconnectWifi();
            }

            if (errs.contains(R.string.widiNameError)) {
                startSettingsForWiDiName();
            }

            fixing = false;
        }
    };

    public void startSettingsForAirplaneMode() {
        try {
            c.startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
            Toast.makeText(c, "Please enable Airplane Mode and then return to Tesla Inspect",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {

        }
    }

    public void disableBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public void enableWifi() {
        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    public void disconnectWifi() {
        WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
    }

    public void startSettingsForWiDiName() {
        try {
            c.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            Toast.makeText(c, "There is an error with your WiFi Direct name. Please go to your " +
                    "WiFi Direct settings and change it.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {

        }
    }
}
