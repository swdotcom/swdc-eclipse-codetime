package com.swdc.codetime.managers;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.Activator;
import com.swdc.codetime.models.KeystrokeAggregate;
import com.swdc.codetime.models.SessionSummary;
import com.swdc.codetime.util.SoftwareCoUtils;

public class SessionDataManager {

    public static String getSessionDataSummaryFile() {
        String file = FileManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\sessionSummary.json";
        } else {
            file += "/sessionSummary.json";
        }
        return file;
    }

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileManager.writeData(getSessionDataSummaryFile(), summary);
    }

    public static SessionSummary getSessionSummaryData() {
    	String summaryFile = getSessionDataSummaryFile();
        JsonObject jsonObj = FileManager.getFileContentAsJson(summaryFile);
        if (jsonObj == null) {
            clearSessionSummaryData();
            jsonObj = FileManager.getFileContentAsJson(getSessionDataSummaryFile());
        }

        JsonElement lastUpdatedToday = jsonObj.get("lastUpdatedToday");
        if (lastUpdatedToday != null) {
            // make sure it's a boolean and not a number
            if (!lastUpdatedToday.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = lastUpdatedToday.getAsInt() == 0 ? false : true;
                jsonObj.addProperty("lastUpdatedToday", newVal);
            }
        }
        
        JsonElement inFlow = jsonObj.get("inFlow");
        if (inFlow != null) {
            // make sure it's a boolean and not a number
            if (!inFlow.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = inFlow.getAsInt() == 0 ? false : true;
                jsonObj.addProperty("inFlow", newVal);
            }
        }
        
        Type type = new TypeToken<SessionSummary>() {}.getType();
        SessionSummary summary = Activator.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate) {
        SessionSummary summary = getSessionSummaryData();

        long incrementMinutes = Math.max(1, getMinutesSinceLastPayload());
        summary.currentDayMinutes = summary.currentDayMinutes + incrementMinutes;

        summary.currentDayKeystrokes = summary.currentDayKeystrokes + aggregate.keystrokes;
        summary.currentDayLinesAdded = summary.currentDayLinesAdded + aggregate.linesAdded;
        summary.currentDayLinesRemoved = summary.currentDayLinesRemoved + aggregate.linesRemoved;

        // save the file
        FileManager.writeData(getSessionDataSummaryFile(), summary);
       
    }

    public static long getMinutesSinceLastPayload() {
        long minutesSinceLastPayload = 1;
        long lastPayloadEnd = FileManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        if (lastPayloadEnd > 0) {
            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
            long diffInSec = timesData.now - lastPayloadEnd;
            long sessionThresholdSeconds = 60 * 15;
            if (diffInSec > 0 && diffInSec <= sessionThresholdSeconds) {
                minutesSinceLastPayload = diffInSec / 60;
            }
        }

        return Math.max(1, minutesSinceLastPayload);
    }
}
