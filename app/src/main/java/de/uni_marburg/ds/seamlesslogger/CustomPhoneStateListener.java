package de.uni_marburg.ds.seamlesslogger;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.List;

import static de.uni_marburg.ds.seamlesslogger.DatabaseHelper.addRecord;

class CustomPhoneStateListener extends PhoneStateListener {
    private Context context;

    CustomPhoneStateListener(Context ctx) {
        context = ctx;
    }

    @Override
    public void onCellInfoChanged(List<CellInfo> cellInfo) {
        super.onCellInfoChanged(cellInfo);

        if (cellInfo == null) {
            Log.w("SEAMLESS", "No information about cellular networks.");
            return;
        }

        for (CellInfo cell : cellInfo) {
            if (cell instanceof CellInfoCdma) {
                addRecord(new CellCDMAData(((CellInfoCdma) cell).getCellIdentity(), ((CellInfoCdma) cell).getCellSignalStrength()));
            }

            if (cell instanceof CellInfoGsm) {
                addRecord(new CellGSMData(((CellInfoGsm) cell).getCellIdentity(), ((CellInfoGsm) cell).getCellSignalStrength()));
            }

            if (cell instanceof CellInfoWcdma) {
                addRecord(new CellWCDMAData(((CellInfoWcdma) cell).getCellIdentity(), ((CellInfoWcdma) cell).getCellSignalStrength()));
            }

            if (cell instanceof CellInfoLte) {
                addRecord(new CellLTEData(((CellInfoLte) cell).getCellIdentity(), ((CellInfoLte) cell).getCellSignalStrength()));
            }
        }
    }

    @Override
    public void onDataActivity(int direction) {
        super.onDataActivity(direction);
        TelephonyManager telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
        addRecord(new CellDataActivityData(direction, telephonyManager.getNetworkType(), telephonyManager.getNetworkOperatorName()));
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        addRecord(new ServiceStateData(serviceState));
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {
        super.onCellLocationChanged(location);

        if (location instanceof CdmaCellLocation) {
            addRecord(new CDMALocationData((CdmaCellLocation) location));
        }

        if (location instanceof GsmCellLocation) {
            addRecord(new GSMLocationData((GsmCellLocation) location));
        }
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        addRecord(new SignalStrengthData(signalStrength));
    }
}