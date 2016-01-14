package com.vegetarianbaconite.teslainspect;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.WifiConfiguration;
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
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Collection;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DeviceNameReceiver.OnDeviceNameReceivedListener {

    TextView widiName, widiConnected, wifiEnabled, batteryLevel, osVersion, airplaneMode, bluetooth,
        wifiConnected, passFail, appsStatus;
    FrameLayout isRC, isDS, isCC;
    ActionBar ab;
    final int dsid = 9277, ccid = 10650;
    String rcApp = "com.qualcomm.ftcrobotcontroller", dsApp = "com.qualcomm.ftcdriverstation",
            ccApp = "com.zte.wifichanneleditor", widiNameString = "";
    DeviceNameReceiver mDeviceNameReceiver;
    Pattern osRegex1, osRegex2, teamNoRegex, rcRegex, dsRegex;
    Handler handler;
    Runnable refreshRunnable;
    TimerTask task;
    IntentFilter filter;
    Button wifiButton;
    Button directButton;

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
        appsStatus = (TextView) findViewById(R.id.appsStatus);

        wifiButton = (Button) findViewById(R.id.wifiButton);
        wifiButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deleteAllWifi();
                Toast.makeText(getApplicationContext(), "Deleted remembered Wifi Networks!", Toast.LENGTH_SHORT).show();
            }
        });

        directButton = (Button) findViewById(R.id.directButton);
        directButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deletePersistentInfo();
                Toast.makeText(getApplicationContext(), "Deleted remembered WifiDirect Connections!", Toast.LENGTH_SHORT).show();
            }
        });

        osRegex1 = Pattern.compile("4\\.2\\.\\d");
        osRegex2 = Pattern.compile("4\\.4\\.\\d");
        teamNoRegex = Pattern.compile("^\\d{1,5}(-\\w)?-(RC|DS)\\z");
        rcRegex = Pattern.compile("RC");
        dsRegex = Pattern.compile("DS");

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
        phoneIsDS();
        if(!validateVersion()) return false;
        if(!getAirplaneMode()) return false;
        if(getBluetooth()) return false;
        if(!getWiFiEnabled()) return false;
        if(getWifiConnected()) return false;
        if(!validateDeviceName()) return false;

        // Check if name string has a carriage return or line feed
        if (widiNameString.contains("\n") || widiNameString.contains("\r")) return false;

        return validateAppsInstalled();
    }

    private void refresh() {
        widiConnected.setText(getWiDiConnected() ? "\u2713" : "X");
        wifiEnabled.setText(getWiFiEnabled() ? "\u2713" : "X");
        osVersion.setText(Build.VERSION.RELEASE);
        airplaneMode.setText(getAirplaneMode() ? "\u2713" : "X");
        bluetooth.setText(getBluetooth() ? "On" : "Off");
        wifiConnected.setText(getWifiConnected() ? "Yes" : "No");
        widiName.setText(widiNameString);
        appsStatus.setText(validateAppsInstalled() ? "\u2713" : "X");

        widiConnected.setTextColor(getWiDiConnected() ? Color.GREEN : Color.RED);
        wifiEnabled.setTextColor(getWiFiEnabled() ? Color.GREEN : Color.RED);
        airplaneMode.setTextColor(getAirplaneMode() ? Color.GREEN : Color.RED);
        bluetooth.setTextColor(!getBluetooth() ? Color.GREEN : Color.RED);
        osVersion.setTextColor(validateVersion() ? Color.GREEN : Color.RED);

        widiName.setTextColor((validateDeviceName() && !(widiNameString.contains("\n") || widiNameString.contains("\r"))) ? Color.GREEN : Color.RED);

        wifiConnected.setTextColor(!getWifiConnected() ? Color.GREEN : Color.RED);
        appsStatus.setTextColor(validateAppsInstalled() ? Color.GREEN : Color.RED);

        isRC.removeAllViews();
        isDS.removeAllViews();
        isCC.removeAllViews();

        if(packageExists(ccApp)) {
            isCC.addView(getTV(true));
        } else {
            isCC.addView(buildButton(ccid));
        }

        if(!(phoneIsDS() == null)) {
            if(!phoneIsDS()) {
                if (packageExists(rcApp)) {
                    isRC.addView(getTV(true));
                } else {
                    isRC.addView(getTV(false));
                }
            } else {
                isRC.addView(getNotApplicableTV());
            }

            if(phoneIsDS()) {
                if (packageExists(dsApp)) {
                    isDS.addView(getTV(true));
                } else {
                    isDS.addView(buildButton(dsid));
                }
            } else {
                isDS.addView(getNotApplicableTV());
            }
        }

        getBatteryInfo();

        if (widiNameString.contains("\n") || widiNameString.contains("\r")) {
            passFail.setText("FAIL - Invalid Name");
            passFail.setTextColor(Color.RED);
        } else {
            passFail.setText(validateInputs() ? "Pass" : "Fail");
            passFail.setTextColor(validateInputs() ? Color.GREEN : Color.RED);
        }
    }

    public void explainErrors() {

    }

    @Override
    public void onDeviceNameReceived(String deviceName) {
        widiNameString = deviceName;
        refresh();
    }

    public Boolean getAirplaneMode() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public void setAirplaneMode() {
        try {
            startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
        } catch (Exception e) {

        }
    }

    public Boolean getWifiConnected() {
        WifiManager m = (WifiManager) getSystemService(WIFI_SERVICE);
        SupplicantState s = m.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
        Log.v("getWifiConnected", state.toString());

        return(state == NetworkInfo.DetailedState.CONNECTED ||
                state == NetworkInfo.DetailedState.OBTAINING_IPADDR);
    }

    public void setWifiConnected() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
    }

    public Boolean getBluetooth() {
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

    public void setBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public Boolean validateVersion() {
        return(osRegex2.matcher(Build.VERSION.RELEASE).find() || osRegex1.matcher(Build.VERSION.RELEASE).find());
    }

    public Boolean validateDeviceName() {
        return(teamNoRegex.matcher(widiNameString)).find();
    }

    public Boolean getWiFiEnabled() {
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    public void setWifiEnabled() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }

    public Boolean getWiDiConnected() {
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

    public Boolean validateAppsInstalled() {
        if(!packageExists(ccApp)) return false;
        if(phoneIsDS() == null) return false;

        if(phoneIsDS()) {
            return packageExists(dsApp);
        } else {
            return packageExists(rcApp);
        }
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

    public Boolean phoneIsDS() {
        if(dsRegex.matcher(widiNameString).find())
            return true;
        if(rcRegex.matcher(widiNameString).find())
            return false;
        return null;
    }

    public boolean packageExists (String targetPackage){
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
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

    private TextView getNotApplicableTV () {
        TextView tv = new TextView(this);

        tv.setText("N/A");

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

    private void deleteAllWifi() {
        WifiManager mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = mainWifiObj.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            mainWifiObj.removeNetwork(i.networkId);
            mainWifiObj.saveConfiguration();
        }
    }

    private void deletePersistentInfo() {
        final WifiP2pManager wifiP2pManagerObj = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        final Context context = getApplicationContext();
        final Channel channel = wifiP2pManagerObj.initialize(context, context.getMainLooper(), new ChannelListener() {
                    @Override
                    public void onChannelDisconnected() {
                        Log.d("WIFIDIRECT", "Channel disconnected!");
                    }
                });

        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(wifiP2pManagerObj, channel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}