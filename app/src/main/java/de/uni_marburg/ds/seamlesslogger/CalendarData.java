package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;

import java.util.Calendar;

class CalendarData extends SugarRecord {
    private long ts;
    private String cal;
    private String title;
    private String location;
    private long start;
    private long end;

    CalendarData(String c, String t, String loc, long s, long e) {
        ts = Calendar.getInstance().getTimeInMillis();
        cal = c;
        title = ""; // Don't save the title for now.
        location = ""; // Don't save the location for now.
        start = s;
        end = e;
    }
}
