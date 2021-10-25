package com.swdc.codetime.managers;


import javax.swing.SwingUtilities;

import swdc.java.ops.manager.StatusBarUpdateHandler;
import swdc.java.ops.model.SessionSummary;

public class SessionStatusUpdateManager implements StatusBarUpdateHandler {
    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        SwingUtilities.invokeLater(() -> {
        	SessionDataManager.updateFileSummaryAndStatsBar(sessionSummary);
        });
    }
}
