package de.uni_marburg.ds.seamlesslogger;


import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.SignalStrength;
import android.telephony.ServiceState;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.orm.SugarRecord;

import java.util.Calendar;

// TODO: Do we need CDMA?

class CellCDMAData extends SugarRecord {
    private long ts;
    private int stationId;
    private int lat;
    private int lon;
    private int netId;
    private int sysId;

    private int asu;
    private int rssiDbm;
    private int ecio;
    private int level;
    private int dbm;
    private int evdoDbm;
    private int evdoEcio;
    private int evdo;
    private int snr;
    private int signal;


    CellCDMAData(CellIdentityCdma cellId, CellSignalStrengthCdma strength) {
        ts = Calendar.getInstance().getTimeInMillis();
        stationId = cellId.getBasestationId();
        lat = cellId.getLatitude();
        lon = cellId.getLongitude();
        netId = cellId.getNetworkId();
        sysId = cellId.getSystemId();

        asu = strength.getAsuLevel();
        rssiDbm = strength.getCdmaDbm();
        ecio = strength.getCdmaEcio();
        level = strength.getCdmaLevel();
        dbm = strength.getDbm();
        evdoDbm = strength.getEvdoDbm();
        evdoEcio = strength.getEvdoEcio();
        evdo = strength.getEvdoLevel();
        snr = strength.getEvdoSnr();
        signal = strength.getLevel();
    }
}

class CellGSMData extends SugarRecord {
    private long ts;
    private int cid;
    private int lac;
    private int mcc;
    private int mnc;

    private int asu;
    private int dbm;
    private int level;

    CellGSMData(CellIdentityGsm cellId, CellSignalStrengthGsm strength) {
        ts = Calendar.getInstance().getTimeInMillis();
        cid = cellId.getCid();
        lac = cellId.getLac();
        mcc = cellId.getMcc();
        mnc = cellId.getMnc();

        asu = strength.getAsuLevel();
        dbm = strength.getDbm();
        level = strength.getLevel();
    }
}

class CellLTEData extends SugarRecord {
    private long ts;
    private int ci;
    private int mcc;
    private int mnc;
    private int pci;
    private int tac;

    private int asu;
    private int dbm;
    private int level;
    private int timing;

    CellLTEData(CellIdentityLte cellId, CellSignalStrengthLte strength) {
        ts = Calendar.getInstance().getTimeInMillis();
        ci = cellId.getCi();
        mcc = cellId.getMcc();
        mnc = cellId.getMnc();
        pci = cellId.getPci();
        tac = cellId.getTac();

        asu = strength.getAsuLevel();
        dbm = strength.getDbm();
        level = strength.getLevel();
        timing = strength.getTimingAdvance();



    }
}

class CellWCDMAData extends SugarRecord {
    private long ts;
    private int cid;
    private int lac;
    private int mcc;
    private int mnc;
    private int psc;

    private int asu;
    private int dbm;
    private int level;

    CellWCDMAData(CellIdentityWcdma cellId, CellSignalStrengthWcdma strength) {
        ts = Calendar.getInstance().getTimeInMillis();
        cid = cellId.getCid();
        lac = cellId.getLac();
        mcc = cellId.getMcc();
        mnc = cellId.getMnc();
        psc = cellId.getPsc();

        asu = strength.getAsuLevel();
        dbm = strength.getDbm();
        level = strength.getLevel();
    }
}

class CellDataActivityData extends SugarRecord {
    private long ts;
    private int direction;
    private int networkType;
    private String operatorName;

    CellDataActivityData(int dir, int type, String operator) {
        ts = Calendar.getInstance().getTimeInMillis();
        direction = dir;
        networkType = type;
        operatorName = operator;
    }
}

class ServiceStateData extends SugarRecord {
    private long ts;
    private boolean manual;
    private String operator;
    private boolean roaming;
    private int serviceState;

    ServiceStateData(ServiceState state) {
        ts = Calendar.getInstance().getTimeInMillis();
        manual = state.getIsManualSelection();
        operator = state.getOperatorAlphaLong();
        roaming = state.getRoaming();
        serviceState = state.getState();
    }
}

class GSMLocationData extends SugarRecord {
    private long ts;
    private int cid;
    private int lac;
    private int psc;

    GSMLocationData (GsmCellLocation loc) {
        ts = Calendar.getInstance().getTimeInMillis();
        cid = loc.getCid();
        lac = loc.getLac();
        psc = loc.getPsc();
    }
}

class CDMALocationData extends SugarRecord {
    private long ts;
    private int stationId;
    private int lat;
    private int lon;
    private int netId;
    private int sysId;

    CDMALocationData(CdmaCellLocation loc) {
        ts = Calendar.getInstance().getTimeInMillis();
        stationId = loc.getBaseStationId();
        lat = loc.getBaseStationLatitude();
        lon = loc.getBaseStationLongitude();
        netId = loc.getNetworkId();
        sysId = loc.getSystemId();
    }
}

class SignalStrengthData extends SugarRecord {
    private long ts;
    private int cdmaDbm;
    private int cdmaEcio;
    private int evdoDbm;
    private int evdoEcio;
    private int evdoSnr;
    private int gsmBitErrorRate;
    private int gsmSignalStrength;
    //private int level;
    private boolean isGsm;

    SignalStrengthData (SignalStrength s) {
        ts = Calendar.getInstance().getTimeInMillis();
        cdmaDbm = s.getCdmaDbm();
        cdmaEcio = s.getCdmaEcio();
        evdoDbm = s.getEvdoDbm();
        evdoEcio = s.getEvdoEcio();
        evdoSnr = s.getEvdoSnr();
        gsmBitErrorRate = s.getGsmBitErrorRate();
        gsmSignalStrength = s.getGsmSignalStrength();
        //level = s.getLevel();
        isGsm = s.isGsm();
    }
}