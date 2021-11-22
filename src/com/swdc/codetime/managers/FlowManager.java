package com.swdc.codetime.managers;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.swdc.codetime.webview.CodeTimeView;

import swdc.java.ops.http.FlowModeClient;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.AsyncManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.FlowMode;

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

		boolean eclipse_CtskipSlackConnect = FileUtilManager.getBooleanItem("eclipse_CtskipSlackConnect");
		boolean workspaces = SlackManager.hasSlackWorkspaces();
		if (!workspaces && !eclipse_CtskipSlackConnect) {
			String msg = "Connect a Slack workspace to pause\nnotifications and update your status?";

			Object[] options = { "Connect", "Skip" };
			Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", FlowManager.class.getClassLoader());

			SwingUtilities.invokeLater(() -> {
				int choice = JOptionPane.showOptionDialog(null, msg, "Slack connect", JOptionPane.OK_OPTION,
						JOptionPane.QUESTION_MESSAGE, icon, options, options[0]);

				if (choice == 0) {
					SlackManager.connectSlackWorkspace(() -> {
						CodeTimeView.refreshView();
					});
				} else {
					FileUtilManager.setBooleanItem("eclipse_CtskipSlackConnect", true);
					FlowManager.enterFlowMode(automated);
				}
			});
			return;
		}

		FlowModeClient.enterFlowMode(automated);

		if (fullScreeConfigured()) {
			ScreenManager.enterFullScreen();
		} else {
			ScreenManager.exitFullScreen();
		}

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

		ScreenManager.exitFullScreen();

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

	public static boolean fullScreeConfigured() {
		String flowModePreferences = FileUtilManager.getItem("flowMode");
		if (StringUtils.isNotBlank(flowModePreferences)) {
			FlowMode flowMode = UtilManager.gson.fromJson(flowModePreferences, FlowMode.class);

			return flowMode.editor.intellij.screenMode.contains("Full Screen") ? true : false;
		}
		return false;
	}
}
