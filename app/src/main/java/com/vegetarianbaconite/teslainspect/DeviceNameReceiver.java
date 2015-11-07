package com.vegetarianbaconite.teslainspect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

public class DeviceNameReceiver extends BroadcastReceiver {
    private OnDeviceNameReceivedListener listener;

    public DeviceNameReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        String thisDeviceName = device.deviceName;

        if (listener != null) {
            listener.onDeviceNameReceived(thisDeviceName);
        }
    }

    public void setOnDeviceNameReceivedListener(Context context) {
        this.listener = (OnDeviceNameReceivedListener) context;
    }

    public interface OnDeviceNameReceivedListener {
        void onDeviceNameReceived(String deviceName);
    }
}
