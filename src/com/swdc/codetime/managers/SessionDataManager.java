package com.swdc.codetime.managers;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.KeystrokeAggregate;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.SessionSummary;

public class SessionDataManager {
    
    public static void refreshSessionDataAndTree() {
    	SessionDataManager.clearSessionSummaryData();
        TimeDataManager.clearTimeDataSummary();
        
    	// clear the auth callback state
        FileUtilManager.setBooleanItem("switching_account", false);
        FileUtilManager.setAuthCallbackState(null);
		
		// update the session summary info to update the statusbar and tree
		new Thread(() -> {
			try {
				// show the success prompt
				CodeTimeActivator.showLoginSuccessPrompt();
				
				WallClockManager wcMgr = WallClockManager.getInstance();
				wcMgr.updateSessionSummaryFromServer();
				
				WallClockManager.refreshTree();
			} catch (Exception e) {
				System.err.println(e);
			}
		}).start();
    }

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static SessionSummary getSessionSummaryData() {
    	String summaryFile = FileUtilManager.getSessionDataSummaryFile();
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(summaryFile);
        if (jsonObj == null) {
            clearSessionSummaryData();
            jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
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
        SessionSummary summary = UtilManager.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate, long sessionMinutes) {
        SessionSummary summary = getSessionSummaryData();

        summary.setCurrentDayMinutes(summary.getCurrentDayMinutes() + sessionMinutes);

        summary.setCurrentDayKeystrokes(summary.getCurrentDayKeystrokes() + aggregate.keystrokes);
        summary.setCurrentDayLinesAdded(summary.getCurrentDayLinesAdded() + aggregate.linesAdded);
        summary.setCurrentDayLinesRemoved(summary.getCurrentDayLinesRemoved() + aggregate.linesRemoved);

        // save the file
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
       
    }
    
    public static ElapsedTime getTimeBetweenLastPayload() {
        ElapsedTime eTime = new ElapsedTime();

        // default to 60 seconds
        long sessionSeconds = 60;
        long elapsedSeconds = 0;

        long lastPayloadEnd = FileUtilManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        if (lastPayloadEnd > 0) {
            UtilManager.TimesData timesData = UtilManager.getTimesData();
            elapsedSeconds = timesData.now - lastPayloadEnd;
            long sessionThresholdSeconds = 60 * 15;
            if (elapsedSeconds > 0 && elapsedSeconds <= sessionThresholdSeconds) {
                sessionSeconds = elapsedSeconds;
            }
            sessionSeconds = Math.max(60, sessionSeconds);
        }

        eTime.sessionSeconds = sessionSeconds;
        eTime.elapsedSeconds = elapsedSeconds;

        return eTime;
    }
}
