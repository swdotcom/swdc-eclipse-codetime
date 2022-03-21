package com.swdc.codetime.managers;

import javax.swing.SwingUtilities;

import com.swdc.codetime.webview.CodeTimeView;

import swdc.java.ops.http.FlowModeClient;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.SlackManager;

public class FlowManager {
	public static boolean enabledFlow = false;
	private static boolean initialized = false;

	public static void initFlowStatus() {
		boolean originalState = enabledFlow;
		enabledFlow = FlowModeClient.isFlowModeOn();
		initialized = true;
		if (originalState != enabledFlow) {
			updateFlowStateDisplay();
		}
	}

	public static void toggleFlowMode(boolean automated) {
		if (!enabledFlow) {
			enterFlowMode(automated);
		} else {
			exitFlowMode();
		}
	}

	public static void enterFlowMode(boolean automated) {
		if (enabledFlow) {
			updateFlowStateDisplay();
			return;
		}

		boolean isRegistered = AccountManager.checkRegistration(false, null);
		if (!isRegistered) {
			// show the flow mode prompt
			AccountManager.showModalSignupPrompt("To use Flow Mode, please first sign up or login.", () -> {
				CodeTimeView.refreshView();
			});
			return;
		}

		FlowModeClient.enterFlowMode(automated);

		SlackManager.clearSlackCache();

		enabledFlow = true;

		updateFlowStateDisplay();
	}

	public static void exitFlowMode() {
		if (!enabledFlow) {
			updateFlowStateDisplay();
			return;
		}

		FlowModeClient.exitFlowMode();

		SlackManager.clearSlackCache();

		enabledFlow = false;

		updateFlowStateDisplay();
	}

	private static void updateFlowStateDisplay() {
		SwingUtilities.invokeLater(() -> {
			// at least update the status bar
			CodeTimeView.refreshView();
			SessionDataManager.updateFileSummaryAndStatsBar(null);
		});
	}

	public static boolean isFlowModeEnabled() {
		if (!initialized) {
			initFlowStatus();
		}
		return enabledFlow;
	}
}
