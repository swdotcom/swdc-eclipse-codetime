package com.swdc.codetime.managers;

import java.lang.reflect.Type;

import javax.swing.SwingUtilities;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.SessionSummary;

public class SessionDataManager {

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static SessionSummary getSessionSummaryFileData() {
    	try {
        	JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        	Type type = new TypeToken<SessionSummary>() {}.getType();
        	return UtilManager.gson.fromJson(jsonObj, type);
    	} catch (Exception e) {
    		return new SessionSummary();
    	}
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
    

    public static void updateSessionSummaryFromServer() {
        SessionSummary summary = SessionDataManager.getSessionSummaryFileData();

        String api = "/api/v1/user/session_summary";
        ClientResponse resp = OpsHttpClient.appGet(api);
        if (resp.isOk()) {
            try {
                Type type = new TypeToken<SessionSummary>() {}.getType();
                summary = UtilManager.gson.fromJson(resp.getJsonObj(), type);
            } catch (Exception e) {
                //
            }
        }

        updateFileSummaryAndStatsBar(summary);
    }
    
    public static void updateFileSummaryAndStatsBar(SessionSummary sessionSummary) {
    	
        if (sessionSummary == null) {
        	sessionSummary = getSessionSummaryFileData();
        } else {
        	// save the file
            FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), sessionSummary);
        }
        
        final SessionSummary summary = sessionSummary;

        SwingUtilities.invokeLater(() -> {
        	// update the status bar

    		// String icon = SoftwareCoUtils.showingStatusText() ? "paw.png" : "clock.png";
    		String msg = UtilManager.humanizeMinutes(summary.currentDayMinutes);
    		String iconName = summary.currentDayMinutes > summary.averageDailyMinutes ? "rocket.png" : "paw.png";
    		StatusBarManager.setStatusLineMessage(msg, iconName,
    				"Active code time today. Click to see more from Code Time.");
        });
    }
}
