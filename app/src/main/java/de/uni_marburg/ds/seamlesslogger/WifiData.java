package de.uni_marburg.ds.seamlesslogger;

import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;

import com.orm.SugarRecord;

import java.util.Calendar;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

class WifiData extends SugarRecord {
    private long ts;
    private int networkType;
    private int networkSubType;
    private NetworkInfo.DetailedState states;
    private SupplicantState state;
    private String bssid;
    private String mac;
    private String ssid;
    //private int frequency;
    private int ip;
    private int speed;
    private int netId;
    private int rssi;


    WifiData(WifiInfo wifiInfo) {
        ts = Calendar.getInstance().getTimeInMillis();

        state = wifiInfo.getSupplicantState();
        bssid = wifiInfo.getBSSID();
        mac = wifiInfo.getMacAddress();
        ssid = wifiInfo.getSSID();
        //frequency = wifiInfo.getFrequency();
        ip = wifiInfo.getIpAddress();
        speed = wifiInfo.getLinkSpeed();
        netId = wifiInfo.getNetworkId();
        rssi = wifiInfo.getRssi();

        Classifier.ffillSensor("WIFI_DATA_RSSI").add(Double.valueOf(wifiInfo.getRssi()));
        Classifier.ffillSensor("WIFI_DATA_SPEED").add(Double.valueOf(wifiInfo.getLinkSpeed()));
    }
}

class WifiScanData extends SugarRecord {
    private long ts;
    private String bssid;
    private String ssid;
    private String capabilities;
    //private int centerFreq0;
    //private int centerFreq1;
    //private int channelWidth;
    private int freq;
    private int rssi;
    private long lastSeen;

    WifiScanData(ScanResult scan) {
        ts = Calendar.getInstance().getTimeInMillis();
        bssid = scan.BSSID;
        ssid = scan.SSID;
        capabilities = scan.capabilities;
        //centerFreq0 = scan.centerFreq0;
        //centerFreq1 = scan.centerFreq1;
        //channelWidth = scan.channelWidth;
        freq = scan.frequency;
        rssi = scan.level;
        lastSeen = scan.timestamp;
    }
}