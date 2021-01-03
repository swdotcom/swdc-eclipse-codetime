package com.swdc.codetime.tree;


import com.swdc.codetime.models.CodeTimeSummary;
import com.swdc.codetime.models.SessionSummary;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

public class MetricLabels {
	
	public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHUB_SIGNUP_ID = "github";
    public static final String EMAIL_SIGNUP_ID = "email";
    public static final String LOGGED_IN_ID = "logged_in";
    public static final String LEARN_MORE_ID = "learn_more";
    public static final String SEND_FEEDBACK_ID = "send_feedback";
    public static final String ADVANCED_METRICS_ID = "advanced_metrics";
    public static final String TOGGLE_METRICS_ID = "toggle_metrics";
    public static final String VIEW_SUMMARY_ID = "view_summary";
    public static final String CODETIME_PARENT_ID = "codetime_parent";
    public static final String CODETIME_TODAY_ID = "codetime_today";
    public static final String ACTIVE_CODETIME_PARENT_ID = "active_codetime_parent";
    public static final String ACTIVE_CODETIME_TODAY_ID = "active_codetime_today";
    public static final String ACTIVE_CODETIME_AVG_TODAY_ID = "active_codetime_avg_today";
    public static final String ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID = "active_codetime_global_avg_today";
    
    public static final String LINES_ADDED_TODAY_ID = "lines_added_today";
    public static final String LINES_ADDED_AVG_TODAY_ID = "lines_added_avg_today";
    public static final String LINES_ADDED_GLOBAL_AVG_TODAY_ID = "lines_added_global_avg_today";
    
    public static final String LINES_DELETED_TODAY_ID = "lines_deleted_today";
    public static final String LINES_DELETED_AVG_TODAY_ID = "lines_deleted_avg_today";
    public static final String LINES_DELETED_GLOBAL_AVG_TODAY_ID = "lines_deleted_global_avg_today";
    
    public static final String KEYSTROKES_TODAY_ID = "keystrokes_today";
    public static final String KEYSTROKES_AVG_TODAY_ID = "keystrokes_avg_today";
    public static final String KEYSTROKES_GLOBAL_AVG_TODAY_ID = "keystrokes_global_avg_today";
    
    public static final String SWITCH_ACCOUNT_ID = "switch_account";
    
    public static final String SLACK_WORKSPACES_NODE_ID = "slack_workspaces_node";
    public static final String SWITCH_OFF_DARK_MODE_ID = "switch_off_dark_mode";
    public static final String SWITCH_ON_DARK_MODE_ID = "switch_ON_dark_mode";
    public static final String TOGGLE_DOCK_POSITION_ID = "toggle_dock_position";
    public static final String SWITCH_OFF_DND_ID = "switch_off_dnd";
    public static final String SWITCH_ON_DND_ID = "switch_on_dnd";
    public static final String CONNECT_SLACK_ID = "connect_slack";
    public static final String ADD_WORKSPACE_ID = "add_workspace";
    public static final String SET_PRESENCE_AWAY_ID = "set_presence_away";
    public static final String SET_PRESENCE_ACTIVE_ID = "set_presence_active";
	
	public static final String MENU_NODES_KEY = "menu_nodes";
	public static final String FLOW_NODES_KEY = "flow_nodes";
	public static final String KPM_NODES_KEY = "kpm_nodes";
    
    public String keystrokes = "";
    public String keystrokesReferenceAvg = "";
    public String keystrokesAvg = "";
    public String keystrokesGlobalAvg = "";
    public String keystrokesAvgIcon = "";
    public String linesAdded = "";
    public String linesAddedReferenceAvg = "";
    public String linesAddedAvg = "";
    public String linesAddedGlobalAvg = "";
    public String linesAddedAvgIcon = "";
    public String linesRemoved = "";
    public String linesRemovedReferenceAvg = "";
    public String linesRemovedAvg = "";
    public String linesRemovedGlobalAvg = "";
    public String linesRemovedAvgIcon = "";
    public String activeCodeTime = "";
    public String activeCodeTimeReferenceAvg = "";
    public String activeCodeTimeAvg = "";
    public String activeCodeTimeGlobalAvg = "";
    public String activeCodeTimeAvgIcon = "";
    public String codeTime = "";

    public void updateLabels(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        String refClass = FileUtilManager.getItem("reference-class", "user");
        long referenceValue = 0;

        if (sessionSummary != null) {
            referenceValue = refClass.equals("user") ? sessionSummary.averageDailyKeystrokes : sessionSummary.globalAverageDailyKeystrokes;
            keystrokesReferenceAvg = getPercentOfReferenceAvg(sessionSummary.currentDayKeystrokes, referenceValue);
            keystrokes = "Keystrokes: " + UtilManager.humanizeLongNumbers(sessionSummary.currentDayKeystrokes) + " " + keystrokesReferenceAvg;

            keystrokesAvgIcon = referenceValue < sessionSummary.currentDayKeystrokes ? "bolt.png" : "bolt-grey.png";

            referenceValue = refClass.equals("user") ? sessionSummary.averageLinesAdded : sessionSummary.globalAverageLinesAdded;
            linesAddedReferenceAvg = getPercentOfReferenceAvg(sessionSummary.currentDayLinesAdded, referenceValue);
            linesAdded = "Lines added: " + UtilManager.humanizeLongNumbers(sessionSummary.currentDayLinesAdded) + " " + linesAddedReferenceAvg;

            linesAddedAvgIcon = referenceValue < sessionSummary.currentDayLinesAdded ? "bolt.png" : "bolt-grey.png";

            referenceValue = refClass.equals("user") ? sessionSummary.averageLinesRemoved : sessionSummary.globalAverageLinesRemoved;
            linesRemovedReferenceAvg = getPercentOfReferenceAvg(sessionSummary.currentDayLinesRemoved, referenceValue);
            linesRemoved = "Lines removed: " + UtilManager.humanizeLongNumbers(sessionSummary.currentDayLinesRemoved) + " " + linesRemovedReferenceAvg;

            linesRemovedAvgIcon = referenceValue < sessionSummary.currentDayLinesRemoved ? "bolt.svg" : "bolt-grey.png";
        }

        if (codeTimeSummary != null && sessionSummary != null) {
            // Active code time
            referenceValue = refClass.equals("user") ? sessionSummary.averageDailyMinutes : sessionSummary.globalAverageDailyMinutes;
            activeCodeTimeReferenceAvg = getPercentOfReferenceAvg(codeTimeSummary.activeCodeTimeMinutes, referenceValue);
            
            activeCodeTime = "Active code time: " + UtilManager.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes) + " " + activeCodeTimeReferenceAvg;

            activeCodeTimeAvgIcon = referenceValue < sessionSummary.currentDayMinutes ? "bolt.png" : "bolt-grey.png";

            // Code Time
            codeTime = "Code time: " + UtilManager.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        }
    }
    
    private String getPercentOfReferenceAvg(long currentValue, long referenceValue) {
        if (currentValue == 0 && referenceValue == 0) {
            return "";
        }
        double quotient = 1;
        if (referenceValue > 0) {
            quotient = currentValue / referenceValue;
            if (currentValue > 0 && quotient < 0.01) {
                quotient = 0.01;
            }
        }
        return "(" + String.format("%.0f", (quotient * 100)) + "% of avg)";
    }
}
