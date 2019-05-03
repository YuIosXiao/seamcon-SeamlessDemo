package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;
import java.util.Calendar;
import java.util.Locale;

import android.bluetooth.BluetoothDevice;

class BluetoothDeviceData extends SugarRecord {
    private long ts;
    private String name;
    private String address;
    private int bondState;
    private int rssi;
    private int deviceClass;

    BluetoothDeviceData(BluetoothDevice dev, int rssi) {
        ts = Calendar.getInstance().getTimeInMillis();
        name = dev.getName();
        address = dev.getAddress();
        bondState = dev.getBondState();
        rssi = rssi;
        deviceClass = dev.getBluetoothClass().getDeviceClass();
    }

    public String toString() {
        return String.format(Locale.getDefault(), "Bluetooth: %s, %s, %d, %d", address, name, bondState, rssi);
    }

}
