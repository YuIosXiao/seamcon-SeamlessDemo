package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;

import java.util.Calendar;

class TCPProbeData extends SugarRecord {
    private long ts;
    private long duration_ms;

    TCPProbeData(long ts, long duration_ms) {
        this.ts = ts;
        this.duration_ms = duration_ms;
    }
}

class PingData extends SugarRecord {
    private long ts;
    double rtt;
    int ttl;
    String default_gw;

    PingData(double rtt, String default_gw, int ttl) {
        this.ts = Calendar.getInstance().getTimeInMillis();
        this.rtt = rtt;
        this.default_gw = default_gw;
        this.ttl = ttl;
    }
}
