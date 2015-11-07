package com.vegetarianbaconite.teslainspect;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.TimerTask;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DeviceNameReceiver.OnDeviceNameReceivedListener {

    TextView widiName, widiConnected, wifiEnabled, batteryLevel, osVersion, airplaneMode, bluetooth,
        wifiConnected, passFail, appsInstalled;
    FrameLayout isRC, isDS, isCC;
    ActionBar ab;
    final int rcid = 1001, dsid = 1002, ccid = 1003;
    String rcApp = "com.qualcomm.ftcrobotcontroller", dsApp = "com.qualcomm.ftcdriverstation",
            ccApp = "com.zte.wifichanneleditor", widiNameString = "";
    DeviceNameReceiver mDeviceNameReceiver;
    Pattern osRegex1, osRegex2, teamNoRegex;
    Handler handler;
    Runnable refreshRunnable;
    TimerTask task;
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ab = getSupportActionBar();
        ab.setTitle("Tesla Inspect: " + BuildConfig.VERSION_NAME);

        refreshRunnable =  new Runnable() {
            @Override
            public void run() {
                refresh();
                handler.postDelayed(getRefreshRunnable(), 1000);
                Log.d("Handler", "Boop.");
            }
        };

        task = new TimerTask() {
            @Override
            public void run() {
                refresh();
                Log.d("TimerTask", "Boop.");
            }
        };

        isRC = (FrameLayout) findViewById(R.id.isRCInstalled);
        isDS = (FrameLayout) findViewById(R.id.isDSInstalled);
        isCC = (FrameLayout) findViewById(R.id.isCCInstalled);

        widiName = (TextView) findViewById(R.id.widiName);
        widiConnected = (TextView) findViewById(R.id.widiConnected);
        wifiEnabled = (TextView) findViewById(R.id.wifiEnabled);
        batteryLevel = (TextView) findViewById(R.id.batteryLevel);
        osVersion = (TextView) findViewById(R.id.osVersion);
        airplaneMode = (TextView) findViewById(R.id.airplaneMode);
        bluetooth = (TextView) findViewById(R.id.bluetoothEnabled);
        wifiConnected = (TextView) findViewById(R.id.wifiConnected);
        passFail = (TextView) findViewById(R.id.passFail);
        appsInstalled = (TextView) findViewById(R.id.appsInstalled);

        osRegex1 = Pattern.compile("4\\.2\\.\\d");
        osRegex2 = Pattern.compile("4\\.4\\.\\d");
        teamNoRegex = Pattern.compile("\\d{1,5}-\\w+");

        initReciever();
        startReceivingWidiInfo();


        handler = new Handler();
        handler.postDelayed(getRefreshRunnable(), 1000);


        //new Timer().scheduleAtFixedRate(task, 0, 1000);

        refresh();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mDeviceNameReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        startReceivingWidiInfo();
        super.onResume();
    }

    private Boolean validateInputs() {
        if(!validateVersion()) return false;
        if(!getAirplaneMode()) return false;
        if(getBluetooth()) return false;
        if(!getWiFiEnabled()) return false;
        if(getWifiConnected()) return false;
        if(!validateDeviceName()) return false;
        if(!validateAppsInstalled()) return false;
        return true;
    }

    private void refresh() {
        widiConnected.setText(getWiDiConnected() ? "\u2713" : "X");
        wifiEnabled.setText(getWiFiEnabled() ? "\u2713" : "X");
        osVersion.setText(Build.VERSION.RELEASE);
        airplaneMode.setText(getAirplaneMode() ? "\u2713" : "X");
        bluetooth.setText(getBluetooth() ? "On" : "Off");
        wifiConnected.setText(getWifiConnected() ? "Yes" : "No");
        widiName.setText(widiNameString);

        widiConnected.setTextColor(getWiDiConnected() ? Color.GREEN : Color.RED);
        wifiEnabled.setTextColor(getWiFiEnabled() ? Color.GREEN : Color.RED);
        airplaneMode.setTextColor(getAirplaneMode() ? Color.GREEN : Color.RED);
        bluetooth.setTextColor(!getBluetooth() ? Color.GREEN : Color.RED);
        osVersion.setTextColor(validateVersion() ? Color.GREEN : Color.RED);
        widiName.setTextColor(validateDeviceName() ? Color.GREEN : Color.RED);
        wifiConnected.setTextColor(!getWifiConnected() ? Color.GREEN : Color.RED);
        appsInstalled.setTextColor(validateAppsInstalled() ? Color.GREEN : Color.RED);

        isRC.removeAllViews();
        isDS.removeAllViews();
        isCC.removeAllViews();

        if(packageExists(rcApp)) {
            isRC.addView(getTV(true));
        } else {
            isRC.addView(getTV(false));
        }

        if(packageExists(dsApp)) {
            isDS.addView(getTV(true));
        } else {
            isDS.addView(buildButton(dsid));
        }

        if(packageExists(ccApp)) {
            isCC.addView(getTV(true));
        } else {
            isCC.addView(buildButton(ccid));
        }

        getBatteryInfo();

        passFail.setText(validateInputs() ? "Pass" : "Fail");
        passFail.setTextColor(validateInputs() ? Color.GREEN : Color.RED);
    }

    @Override
    public void onDeviceNameReceived(String deviceName) {
        widiNameString = deviceName;
        refresh();
    }

    private Boolean getAirplaneMode() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private Boolean getWifiConnected() {
        WifiManager m = (WifiManager) getSystemService(WIFI_SERVICE);
        SupplicantState s = m.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
        Log.v("getWifiConnected", state.toString());

        return(state == NetworkInfo.DetailedState.CONNECTED ||
                state == NetworkInfo.DetailedState.OBTAINING_IPADDR);
    }

    private Boolean getBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                return false;
            }
        }

        return true;
    }

    private Boolean validateVersion() {
        return(osRegex2.matcher(Build.VERSION.RELEASE).find() || osRegex1.matcher(Build.VERSION.RELEASE).find());
    }

    private Boolean validateDeviceName() {
        return(teamNoRegex.matcher(widiNameString)).find();
    }

    private Boolean getWiFiEnabled() {
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    private Boolean getWiDiConnected() {
        return new WifiP2pDevice().status == WifiP2pDevice.CONNECTED;
    }

    private void initReciever() {
        mDeviceNameReceiver = new DeviceNameReceiver();
        mDeviceNameReceiver.setOnDeviceNameReceivedListener(this);
        filter = new IntentFilter(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION );
    }

    private void startReceivingWidiInfo() {
        registerReceiver(mDeviceNameReceiver, filter);
    }

    private Boolean validateAppsInstalled() {
        if(!packageExists(ccApp)) return false;
        if(!packageExists(dsApp) && !packageExists(rcApp)) return false;
        return true;
    }

    private void getBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;

        batteryLevel.setText(Math.round(batteryPct * 100f) + "%");
    }

    private Runnable getRefreshRunnable() {
        return refreshRunnable;
    }

    public boolean packageExists (String targetPackage){
        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private TextView getTV (boolean installed) {
        TextView tv = new TextView(this);

        tv.setText(installed ? "\u2713" : "X");
        tv.setTextColor(installed ? Color.GREEN : Color.RED);

        return tv;
    }

    private Button buildButton(int id) {
        Button button = new Button(this);

        button.setText("Install");
        button.setId(id);
        button.setOnClickListener(this);

        return button;
    }

    private void startStore(String appPackageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == dsid) {
            startStore(dsApp);
        }

        if (id == ccid) {
            startStore(ccApp);
        }
    }
}
