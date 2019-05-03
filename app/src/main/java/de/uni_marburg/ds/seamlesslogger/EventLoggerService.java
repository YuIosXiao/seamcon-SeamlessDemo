package de.uni_marburg.ds.seamlesslogger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

import static de.uni_marburg.ds.seamlesslogger.DatabaseHelper.addRecord;
import static de.uni_marburg.ds.seamlesslogger.DatabaseHelper.dumpRecords;


public class EventLoggerService extends Service {
    private static final long DETECTION_INTERVAL_IN_MILLISECONDS = 1000;
    ArrayList<String> sensorStringList;
    private boolean stopHandlers = false;

    /************************************** GLOBAL VARIABLES **************************************/
    // Simple sensors
    private SensorManager sensorManager;
    private static final int[] INTERESTING_SENSORS = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_ORIENTATION,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_STEP_COUNTER,
    };

    // Trigger senors
    private static final int[] INTERESTING_TRIGGER_SENSORS = {
            Sensor.TYPE_SIGNIFICANT_MOTION,
            //Sensor.TYPE_STATIONARY_DETECT,
            Sensor.TYPE_STEP_DETECTOR,
    };

    // Location
    private LocationManager locationManager;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;

    // Cellular
    TelephonyManager telephonyManager;
    CustomPhoneStateListener phoneStateListener = new CustomPhoneStateListener(this);

    // Calendar
    public static final long CALENDAR_LOGGING_INTERVAL = 1000 * 60 * 60 * 24; // 1 day
    private Handler calendarLoggerHandler = new Handler();
    private Timer calendarLoggerTimer = null;


    /****************************** LISTENERS AND INTENT RECEIVERS ********************************/
    // Simple sensors
    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                // direct 3d sensors
                case Sensor.TYPE_ACCELEROMETER:
                    addRecord(new AccelerometerData(event.values));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    addRecord(new GyroscopeData(event.values));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    addRecord(new MagneticFieldData(event.values));
                    break;

                // derived 3d sensors
                case Sensor.TYPE_GRAVITY:
                    addRecord(new GravityData(event.values));
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    addRecord(new LinearAccelerationData(event.values));
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    addRecord(new RotationData(event.values));
                    break;
                case Sensor.TYPE_ORIENTATION:
                    addRecord(new OrientationData(event.values));
                    break;

                // scalar sensors
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    addRecord(new AmbientTemperatureData(event.values[0]));
                    break;
                case Sensor.TYPE_LIGHT:
                    addRecord(new LightData(event.values[0]));
                    break;
                case Sensor.TYPE_PRESSURE:
                    addRecord(new PressureData(event.values[0]));
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    addRecord(new RelativeHumidityData(event.values[0]));
                    break;
                case Sensor.TYPE_STEP_COUNTER:
                    addRecord(new StepCounterData(event.values[0]));
                    break;

                default:
                    Log.w("SEAMLESS", event.sensor.getName() + " not logged: " + Arrays.toString(event.values));
            }

        }
    };


    // Trigger sensors
    private TriggerEventListener triggerSensorListener = new TriggerEventListener() {
        public void onTrigger(TriggerEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_SIGNIFICANT_MOTION:
                    addRecord(new SignificantMotionData(event.values[0]));
                    break;
                //case Sensor.TYPE_STATIONARY_DETECT:
                //    addRecord(new StationaryDetectData(event.values[0]));
                //    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    addRecord(new StepDetectData(event.values[0]));
                    break;
                default:
                    Log.w("SEAMLESS", event.sensor.getName() + " not logged: " + Arrays.toString(event.values));
            }

        }
    };


    // Location
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            addRecord(new GPSData(location));
        }

        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };


    // Battery
    private BroadcastReceiver powerReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean interestingAction = Intent.ACTION_BATTERY_CHANGED.equals(action) || Intent.ACTION_POWER_CONNECTED.equals(action) || Intent.ACTION_POWER_DISCONNECTED.equals(action);

            if (interestingAction){
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

                int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level / (float)scale;

                addRecord(new PowerData(isCharging, usbCharge, acCharge, batteryPct));
            }
        }
    };


    // Audio
    private BroadcastReceiver audioReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // It is an interesting intent, when a headset gets (un)plugged,
            // the bluetooth audio state changes,
            // ringer mode changes (volume, vibrating, alarms, off)
            // or volume buttons are pushed.
            boolean interestingAudioIntent = AudioManager.ACTION_HEADSET_PLUG.equals(action)
                    || AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)
                    || AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)
                    || "android.media.VOLUME_CHANGED_ACTION".equals(action);

            if (interestingAudioIntent) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

                /*
                // Get alls available devices and save all of them.
                for (AudioDeviceInfo info : audioManager.getDevices(AudioManager.GET_DEVICES_ALL)) {
                    addRecord(new AudioDeviceData(info));
                }
                */

                addRecord(new AudioData(audioManager));
            }
        }
    };


    // Bluetooth
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // If a new device was found, get the RSSI, Name, and pairing state.
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                addRecord(new BluetoothDeviceData(device, rssi));
            }

            // If the discovery is finished, we start a new one.
            // Each discovery process takes about 12 seconds.
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothAdapter.startDiscovery();
            }
        }
    };


    // Wifi
    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            addRecord(new WifiData(wifiInfo));
        }
    };


    // WiFi active Scan
    private Handler wifiScanTrigger = new Handler();
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // If a new device was found, get the RSSI, Name, and pairing state.
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                for (ScanResult result : wifiManager.getScanResults()) {
                    addRecord(new WifiScanData(result));
                }

                // wifiManager.startScan();
            }
        }
    };

    // WiFi Info
    private Handler wifiInfoTrigger = new Handler();

    // Network connectivity
    private Handler networkConnectivityTrigger = new Handler();

    // Device State
    private Handler deviceStateData = new Handler();
    private Task<Void> activityTask;
    private Intent activityIntent;

    // Classification Task
    private Handler classificationTask = new Handler();

    class TCPProbeTask extends AsyncTask<Void, Void, Void> {
        private long probeStart;
        private long probeFinish;
        private Exception exception;

        protected Void doInBackground(Void... params) {
            try {
//                final String hostAddress = "95.143.172.228";    // jonashoechst.de
                final String hostAddress = "216.58.208.46";     // google.com

                probeStart = System.currentTimeMillis();
                Socket socket = new Socket(hostAddress, 80);
                socket.close();

                probeFinish = System.currentTimeMillis();

                addRecord(new TCPProbeData(probeFinish, (probeFinish-probeStart)));
            } catch (UnknownHostException e) {
                exception = e;
                e.printStackTrace();
            } catch (IOException e) {
                exception = e;
                e.printStackTrace();
            }

            return null;
        }
    }

    class PingTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            PingData pd = new PingData(1000.0, null, -1);

            try {
                Runtime runtime = Runtime.getRuntime();

                // Get the wifi default gateway
                Process ip_process = runtime.exec("/system/bin/ip route show table 0");
                BufferedReader in = new BufferedReader(new InputStreamReader(ip_process.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    String[] words = line.split(" ");
                    if (words.length <= 5)
                        continue;
                    else if (words[4].equals("wlan0"))
                        pd.default_gw = words[2];
                }

                if (pd.default_gw == null) {
                    throw new Exception("No gateway on Wifi present.");
                }

                // Ping the default gateway
                Process ping_process = runtime.exec("/system/bin/ping -W1 -w1 -c2 -i0.2 " + pd.default_gw);
                in = new BufferedReader(new InputStreamReader(ping_process.getInputStream()));

                while ((line = in.readLine()) != null){
                    String[] words = line.split(" ");

                    if ( words.length >= 7 && words[1].equals("bytes") ) {
                        pd.rtt = Double.parseDouble(words[6].split("=")[1]);
                        pd.ttl = Integer.parseInt(words[5].split("=")[1]);
                        addRecord(pd);
                    }
                }

                ping_process.waitFor();

            } catch (Exception e) {
                Log.w("SEAMLESS_PING", "Error: "+e.getMessage());
            }


            return null;
        }
    }

    // Calendar
    private TimerTask calendarLoggingTask = new TimerTask() {
        @Override
        public void run() {
            calendarLoggerHandler.post(new Runnable() {

                @Override
                public void run() {
                    Cursor cursor = getContentResolver().query(
                            Uri.parse("content://com.android.calendar/events"),
                            new String[] {
                                    "calendar_displayName",
                                    "title",
                                    "eventLocation",
                                    "dtstart",
                                    "dtend"
                            },
                            null,
                            null,
                            null);

                    cursor.moveToFirst();

                    while (cursor.moveToNext()){
                        if (cursor.getString(0).equals("Feiertage in Deutschland")) {
                            continue;
                        }
                        String cal = cursor.getString(0);
                        String title = cursor.getString(1);
                        String location = cursor.getString(2);
                        String startStr = cursor.getString(3);
                        String endStr = cursor.getString(4);
                        long start = 0;
                        long end = 0;
                        /* fail gracefully */
                        if(startStr != null) {
                            try {
                                start = Long.parseLong(startStr);
                            } catch (NumberFormatException ex) {
                                // nothing to do
                            }
                        }

                        if(endStr != null) {
                            try {
                                end = Long.parseLong(endStr);
                            } catch (NumberFormatException ex) {
                                // nothing to do
                            }
                        } 

                        addRecord(new CalendarData(cal, title, location, start, end));
                    }

                    cursor.close();
                }

            });

        }
    };



    /*********************************** SERVICE CONTROLLING **************************************/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Seamless Logger")
                .setTicker("Seamless Connectivity Service running.")
                .setContentText("Seamless Logger collects data in background.")
                .setSmallIcon(R.mipmap.athene)
                .setOngoing(true).build();

        startForeground(1, notification);
        Classifier.reset();

        if (sensorStringList != null) {
            Log.w("SEAMLESS", "Service was already running.");
            updateSensorList(sensorStringList);
            return START_STICKY;
        }

        activityIntent = new Intent(this, ActivityIntentService.class);

        ActivityRecognitionClient mActivityRecognitionClient = new ActivityRecognitionClient(this);

        activityTask = mActivityRecognitionClient.requestActivityUpdates(
                DETECTION_INTERVAL_IN_MILLISECONDS,
                PendingIntent.getService(this.getApplicationContext(), 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        sensorStringList = new ArrayList<>(32);

        // Simple sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor s;

        for (int sensorType : INTERESTING_SENSORS) {
            s = sensorManager.getDefaultSensor(sensorType);
            if (s == null) {
                Log.w("SEAMLESS", "Sensor "+sensorType+" is not available on this device.");
                continue;
            }

            sensorManager.registerListener(sensorListener, s, SensorManager.SENSOR_DELAY_NORMAL);
            sensorStringList.add(s.getName());
        }

        // Trigger sensors
        for (int sensorType : INTERESTING_TRIGGER_SENSORS) {
            s = sensorManager.getDefaultSensor(sensorType);
            if (s == null) {
                Log.w("SEAMLESS", "Sensor " + sensorType + " is not available on this device.");
                continue;
            }

            sensorManager.requestTriggerSensor(triggerSensorListener, s);
            sensorStringList.add(s.getName());
        }

        // Location
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager != null) {
                // Update at minimum every 1 minute / at an accuracy of 10 meters
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 10, locationListener);
                sensorStringList.add("GPS Location");
            } else {
                Log.d("SEAMLESS", "GPS: n/a");
            }
        } catch (SecurityException e) {
            Log.e("SEAMLESS", "GPS: permission missing, skipping sensor.");
        }

        // Battery
        sensorStringList.add("Battery and Power");
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, batteryFilter);

        // Audio
        sensorStringList.add("Audio");
        IntentFilter audioFilter = new IntentFilter();
        audioFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        audioFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        audioFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        audioFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(audioReceiver, audioFilter);


        // Bluetooth
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, bluetoothFilter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.startDiscovery();
                sensorStringList.add("Bluetooth Scan");
            } else {
                Log.d("SEAMLESS", "BLUETOOTH: disabled");
            }
        } else {
            Log.d("SEAMLESS", "BLUETOOTH: n/a");
        }


        // WiFi
        sensorStringList.add("WiFi: network changes");
        IntentFilter networkChangedFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, networkChangedFilter);


        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // WiFi Scan
        final int WIFI_SCAN_DELAY_MS = 10000;
        sensorStringList.add("WiFi: active scanning");
        IntentFilter wifiScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, wifiScanFilter);
        wifiScanTrigger.postDelayed(new Runnable(){
                public void run(){
                    if (stopHandlers) return;

                    if (wifiManager.isWifiEnabled()) {

                        wifiManager.startScan();
                        wifiScanTrigger.postDelayed(this, WIFI_SCAN_DELAY_MS);
                    }
                }
        }, 0);

        // WiFi continuous info
        final int WIFI_INFO_DELAY_MS = 1000;
        sensorStringList.add("WiFi: continuous info");
        wifiInfoTrigger.postDelayed(new Runnable(){
            public void run(){
                if (stopHandlers) return;

                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                addRecord(new WifiData(wifiInfo));

                wifiInfoTrigger.postDelayed(this, WIFI_INFO_DELAY_MS);
            }
        }, 0);


        // Network connectivity
        final int NETWORK_CONNECTIVITY_DELAY_MS = 1000;
        sensorStringList.add("WiFi: Gateway Ping");
        networkConnectivityTrigger.postDelayed(new Runnable(){
            public void run(){
                if (stopHandlers) return;
                new PingTask().execute();

                networkConnectivityTrigger.postDelayed(this, NETWORK_CONNECTIVITY_DELAY_MS);
            }
        }, 0);


        // Device State
        final int DEVICE_STATE_DELAY_MS = 1000;
        sensorStringList.add("Device State");
        deviceStateData.postDelayed(new Runnable(){
            public void run(){
                if (stopHandlers) return;

                PowerManager pwr = ((PowerManager) getSystemService(Context.POWER_SERVICE));
                addRecord(new DeviceStateData(pwr));

                deviceStateData.postDelayed(this, DEVICE_STATE_DELAY_MS);
            }
        }, 0);


        // Cellular activity
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(
                phoneStateListener,
                          PhoneStateListener.LISTEN_CELL_INFO
                        | PhoneStateListener.LISTEN_CELL_LOCATION
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
        );

        // Calendar
        sensorStringList.add("Calendar Events");
        if(calendarLoggerTimer == null) {
            calendarLoggerTimer = new Timer();
            calendarLoggerTimer.scheduleAtFixedRate(calendarLoggingTask, 0, CALENDAR_LOGGING_INTERVAL);
        }


        class AsyncClassificationTask extends AsyncTask<Void, Void, Void> {
            private Context context;

            public AsyncClassificationTask(Context context){
                this.context=context;
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Classifier.runClassification();

                    addRecord(new PredictionData((float) Classifier.prediction));
                    this.reportPrediction(Classifier.prediction);

                    MPTCPHelper.action = Classifier.proposeAction();

                } catch (Classifier.SensorMissingException e) {
                    Log.d("seamless", e.getMessage());
                } catch (Classifier.ObservationWindowException e) {
                    Log.d("seamless", "Observation window is not filled yet: "+e.getMessage());
                } catch (Classifier.ClassifierUninitializedException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                MPTCPHelper.handleTransition();
            }

            private void reportPrediction(double prediction) {
                Intent intent = new Intent(String.valueOf(context.getClass()));
                intent.putExtra("Prediction", prediction);

                context.sendBroadcast(intent);
            }
        }


        final int CLASSIFICATION_TASK_DELAY_MS = 1000;
        classificationTask.postDelayed(new Runnable(){
            public void run(){
                if (stopHandlers) {
                    Classifier.reset();
                    return;
                }

                classificationTask.postDelayed(this, CLASSIFICATION_TASK_DELAY_MS);

                new AsyncClassificationTask(EventLoggerService.this).execute();

            }
        }, 0);


        updateSensorList(sensorStringList);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


        @Override
    public void onDestroy() {
        stopHandlers = true;

        // Simple sensors
        sensorManager.unregisterListener(sensorListener);

        // Trigger sensors
        for (int sensorType : INTERESTING_TRIGGER_SENSORS) {
            Sensor s = sensorManager.getDefaultSensor(sensorType);
            if (s == null) {
                continue;
            }
            sensorManager.cancelTriggerSensor(triggerSensorListener, s);
        }

        // Location
        locationManager.removeUpdates(locationListener);

        // Battery
        this.unregisterReceiver(powerReceiver);

        // Audio
        /***** this.unregisterReceiver(audioReceiver); *****/

        // Bluetooth
        this.unregisterReceiver(bluetoothReceiver);

        // WiFi
        this.unregisterReceiver(wifiReceiver);

        // WiFi Scan
        this.unregisterReceiver(wifiScanReceiver);

        //
        this.stopService(activityIntent);


        // Cellular
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        // Calendar
        calendarLoggerTimer.cancel();

        updateSensorList(new ArrayList<String>(0));

        dumpRecords();
        super.onDestroy();
    }


    private void updateSensorList(ArrayList<String> sensorStringList) {
        Intent intent = new Intent(String.valueOf(this.getClass()));
        intent.putStringArrayListExtra("SensorStringList", sensorStringList);

        this.sendBroadcast(intent);
    }
}
