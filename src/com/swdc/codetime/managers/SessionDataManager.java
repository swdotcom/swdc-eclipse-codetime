package com.swdc.codetime.managers;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.ElapsedTime;
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
        SessionSummary summary = CodeTimeActivator.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate, long sessionMinutes) {
        SessionSummary summary = getSessionSummaryData();

        summary.currentDayMinutes = summary.currentDayMinutes + sessionMinutes;

        summary.currentDayKeystrokes = summary.currentDayKeystrokes + aggregate.keystrokes;
        summary.currentDayLinesAdded = summary.currentDayLinesAdded + aggregate.linesAdded;
        summary.currentDayLinesRemoved = summary.currentDayLinesRemoved + aggregate.linesRemoved;

        // save the file
        FileManager.writeData(getSessionDataSummaryFile(), summary);
       
    }
    
    public static ElapsedTime getTimeBetweenLastPayload() {
        ElapsedTime eTime = new ElapsedTime();

        long sessionSeconds = 0;
        long elapsedSeconds = 0;

        long lastPayloadEnd = FileManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        if (lastPayloadEnd > 0) {
            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
            elapsedSeconds = timesData.now - lastPayloadEnd;
            long sessionThresholdSeconds = 60 * 15;
            if (elapsedSeconds > 0 && elapsedSeconds <= sessionThresholdSeconds) {
                sessionSeconds = elapsedSeconds / 60;
            }
            sessionSeconds = Math.max(60, sessionSeconds);
        }

        eTime.sessionSeconds = sessionSeconds;
        eTime.elapsedSeconds = elapsedSeconds;

        return eTime;
    }
}
