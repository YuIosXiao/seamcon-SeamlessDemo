package de.uni_marburg.ds.seamlesslogger;

import android.media.AudioDeviceInfo;
import android.media.AudioManager;

import com.orm.SugarRecord;

import java.util.Arrays;
import java.util.Calendar;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

class AudioData extends SugarRecord {
    private long ts;
    private int mode;
    private int ringerMode;
    private int callVolume;
    private int systemVolume;
    private int ringVolume;
    private int musicVolume;
    private int alarmVolume;
    private boolean bluetoothAvailOffCall;
    private boolean bluetoothOn;
    private boolean micMute;
    private boolean musicActive;
    private boolean speakerOn;
    private boolean fixedVolume;

    AudioData(AudioManager m) {
        ts = Calendar.getInstance().getTimeInMillis();
        mode = m.getMode();
        ringerMode = m.getRingerMode();
        callVolume = m.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        systemVolume = m.getStreamVolume(AudioManager.STREAM_SYSTEM);
        ringVolume = m.getStreamVolume(AudioManager.STREAM_RING);
        musicVolume = m.getStreamVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = m.getStreamVolume(AudioManager.STREAM_ALARM);
        bluetoothAvailOffCall = m.isBluetoothScoAvailableOffCall();
        bluetoothOn = m.isBluetoothScoOn();
        micMute = m.isMicrophoneMute();
        musicActive = m.isMusicActive();
        speakerOn = m.isSpeakerphoneOn();

        // API 19 disabled
        //fixedVolume = m.isVolumeFixed();

        Classifier.ffillSensor("AUDIO_DATA_MUSIC_VOLUME").add(new Double(musicVolume));
        Classifier.ffillSensor("AUDIO_DATA_RING_VOLUME").add(new Double(ringVolume));
        Classifier.ffillSensor("AUDIO_DATA_RINGER_MODE").add(new Double(ringerMode));
    }
}

class AudioDeviceData extends SugarRecord {
    private long ts;
    private String chanCnts;
    private String chanIndexMasks;
    private String chanMasks;
    private String encodings;
    private String sampleRates;
    private int devId;
    private int type;
    private String name;
    private boolean sink;
    private boolean source;

    AudioDeviceData(AudioDeviceInfo deviceInfo) {
        ts = Calendar.getInstance().getTimeInMillis();
        chanCnts = Arrays.toString(deviceInfo.getChannelCounts());
        chanIndexMasks = Arrays.toString(deviceInfo.getChannelIndexMasks());
        chanMasks = Arrays.toString(deviceInfo.getChannelMasks());
        encodings = Arrays.toString(deviceInfo.getEncodings());
        sampleRates = Arrays.toString(deviceInfo.getSampleRates());
        devId = deviceInfo.getId();
        type = deviceInfo.getType();
        name = deviceInfo.getProductName().toString();
        sink = deviceInfo.isSink();
        source = deviceInfo.isSource();
    }
}
