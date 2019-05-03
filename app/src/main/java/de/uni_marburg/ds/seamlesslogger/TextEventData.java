package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;
import java.util.Calendar;

class TextEventData extends SugarRecord {
    private long ts;
    private String event;

    TextEventData(String event) {
        this.ts = Calendar.getInstance().getTimeInMillis();
        this.event = event;
    }
}
