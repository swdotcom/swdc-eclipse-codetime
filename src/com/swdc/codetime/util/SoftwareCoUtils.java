/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.google.gson.JsonParser;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.EclipseProjectUtil;
import com.swdc.codetime.models.FileDetails;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Project;
import swdc.java.ops.snowplow.entities.UIElementEntity;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class SoftwareCoUtils {

	private static int DASHBOARD_LABEL_WIDTH = 25;
	private static int DASHBOARD_VALUE_WIDTH = 25;

	public static final Logger LOG = Logger.getLogger("SoftwareCoUtils");

	public static String issues_url = "https://github.com/swdotcom/swdc-eclipse-codetime/issues";

	public final static String api_endpoint = "http://localhost:5000";//"https://api.software.com";
	public final static String app_url = "http://localhost:3000";//"https://app.software.com";
	public final static String webui_login_url = app_url + "/login";
	public final static String software_dir = ".software-local";//".software";

	public static JsonParser jsonParser = new JsonParser();

	// sublime = 1, vs code = 2, eclipse = 3, intellij = 4, visual studio = 6, atom = 7
	public final static int pluginId = 3;
	public final static String pluginName = "codetime";
	public static String IDE_VERSION = "";
	public static String IDE_NAME = "eclipse";

	private static String workspace_name = null;

	private static boolean showStatusText = true;
	private static String lastMsg = "";

	static {
		// discover ide info
		try {
			IDE_VERSION = Platform.getBundle("org.eclipse.platform").getVersion().toString();
		} catch (Exception e) {
			try {
				IDE_VERSION = Platform.getBundle("org.jkiss.dbeaver.core").getVersion().toString();
			} catch (Exception e2) {
				try {
					IDE_VERSION = Platform.getProduct().getDefiningBundle().getVersion().toString();
				} catch (Exception e3) {
					IDE_VERSION = "unknown";
				}
			}
		}

		try {
			IDE_NAME = Platform.getProduct().getName();
		} catch (Exception e) {
			IDE_NAME = "eclipse";
		}
	}

	public static String getWorkspaceName() {
		if (workspace_name == null) {
			workspace_name = SoftwareCoUtils.generateToken();
		}
		return workspace_name;
	}

	public static String getVersion() {
		String version = Platform.getBundle(CodeTimeActivator.PLUGIN_ID).getVersion().toString();
		return version;
	}

	public static FileDetails getFileDetails(String fullFileName) {
		FileDetails fileDetails = new FileDetails();
		if (StringUtils.isNotBlank(fullFileName)) {
			fileDetails.full_file_name = fullFileName;
			Project p = EclipseProjectUtil.getInstance().getProjectForPath(fullFileName);
			if (p != null) {
				fileDetails.project_directory = p.getDirectory();
				fileDetails.project_name = p.getName();
			}

			File f = new File(fullFileName);

			if (f.exists()) {
				fileDetails.character_count = f.length();
				fileDetails.file_name = f.getName();
				if (StringUtils.isNotBlank(fileDetails.project_directory)) {
					// strip out the project_file_name
					fileDetails.project_file_name = fullFileName.split(fileDetails.project_directory)[1];
				} else {
					fileDetails.project_file_name = fullFileName;
				}
				fileDetails.line_count = SoftwareCoUtils.getLineCount(fullFileName);

				fileDetails.syntax = EclipseProjectUtil.getInstance().getFileSyntax(new File(fullFileName));
			}
		}

		return fileDetails;
	}

	public static int getLineCount(String fileName) {
		Stream<String> stream = null;
		try {
			Path path = Paths.get(fileName);
			stream = Files.lines(path);
			return (int) stream.count();
		} catch (Exception e) {
			return 0;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	public static boolean showingStatusText() {
		return showStatusText;
	}

	public static void submitIssue() {
		try {
			UtilManager.launchUrl(issues_url);
		} catch (Exception e) {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(issues_url));
			} catch (Exception ex) {
				SWCoreLog.logErrorMessage("Failed to launch the url: " + issues_url);
				SWCoreLog.logException(ex);
			}
		}
	}

	public static void submitFeedback() {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
					.openURL(new URL("mailto:cody@software.com"));
			UIElementEntity elementEntity = new UIElementEntity();
			elementEntity.element_name = "ct_submit_feedback_btn";
			elementEntity.element_location = "ct_menu_tree";
			elementEntity.color = "green";
			elementEntity.cta_text = "Submit feedback";
			elementEntity.icon_name = "text-bubble";
			EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
		} catch (Exception e) {
			SWCoreLog.logException(e);
		}
	}

	public static void toggleStatusBarText(UIInteractionType type) {
		String cta_text = !showStatusText ? "Show status bar metrics" : "Hide status bar metrics";
		showStatusText = !showStatusText;

		UIElementEntity elementEntity = new UIElementEntity();
		elementEntity.element_name = type.equals(UIInteractionType.click) ? "ct_toggle_status_bar_metrics_btn"
				: "ct_toggle_status_bar_metrics_cmd";
		elementEntity.element_location = type.equals(UIInteractionType.click) ? "ct_menu_tree" : "ct_command_palette";
		elementEntity.color = type.equals(UIInteractionType.click) ? "blue" : null;
		elementEntity.cta_text = cta_text;
		elementEntity.icon_name = type.equals(UIInteractionType.click) ? "slash-eye" : null;
		EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
	}

	public static void setStatusLineMessage(final String statusMsg, final String iconName, final String tooltip) {
		String statusTooltip = tooltip;
		String name = FileUtilManager.getItem("name");

		if (showStatusText) {
			lastMsg = statusMsg;
		}

		if (statusTooltip == null) {
			statusTooltip = "Active code time today. Click to see more from Code Time.";
		}

		if (statusTooltip.lastIndexOf(".") != statusTooltip.length() - 1) {
			statusTooltip += ".";
		}

		if (name != null) {
			statusTooltip += " Logged in as " + name;
		}

		final String finalTooltip = statusTooltip;

		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (!workbench.getDisplay().isDisposed())
			workbench.getDisplay().asyncExec(new Runnable() {
				public void run() {
					String statusTooltip = finalTooltip;
					if (showStatusText) {
						SWCoreStatusBar.get().setText(statusMsg);
						SWCoreStatusBar.get().setIconName(iconName);
					} else {
						statusTooltip = lastMsg + " | " + tooltip;
						SWCoreStatusBar.get().setText("");
						SWCoreStatusBar.get().setIconName("clock.png");
					}

					SWCoreStatusBar.get().setTooltip(statusTooltip);
					SWCoreStatusBar.get().update();
				}
			});
	}

	public static List<String> getCommandResult(List<String> cmdList, String dir) {
		String[] args = Arrays.copyOf(cmdList.toArray(), cmdList.size(), String[].class);
		List<String> results = new ArrayList<>();
		String result = UtilManager.runCommand(args, dir);
		if (result == null || result.trim().length() == 0) {
			return results;
		}
		String[] contentList = result.split("\n");
		results = Arrays.asList(contentList);
		return results;
	}

	public static String generateToken() {
		String uuid = UUID.randomUUID().toString();
		return uuid.replace("-", "");
	}

	public static String getDashboardRow(String label, String value) {
		String content = getDashboardLabel(label) + " : " + getDashboardValue(value) + "\n";
		return content;
	}

	public static String getSectionHeader(String label) {
		String content = label + "\n";
		// add 3 to account for the " : " between the columns
		int dashLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH + 15;
		for (int i = 0; i < dashLen; i++) {
			content += "-";
		}
		content += "\n";
		return content;
	}

	public static String getDashboardLabel(String label) {
		return getDashboardDataDisplay(DASHBOARD_LABEL_WIDTH, label);
	}

	public static String getDashboardValue(String value) {
		String valueContent = getDashboardDataDisplay(DASHBOARD_VALUE_WIDTH, value);
		String paddedContent = "";
		for (int i = 0; i < 11; i++) {
			paddedContent += " ";
		}
		paddedContent += valueContent;
		return paddedContent;
	}

	public static String getDashboardDataDisplay(int widthLen, String data) {
		int len = widthLen - data.length();
		String content = "";
		for (int i = 0; i < len; i++) {
			content += " ";
		}
		return content + "" + data;
	}

	/**
	 * Replace byte order mark, new lines, and trim
	 * 
	 * @param data
	 * @return clean data
	 */
	public static String cleanJsonString(String data) {
		data = data.replace("\ufeff", "").replace("/\r\n/g", "").replace("/\n/g", "").trim();

		int braceIdx = data.indexOf("{");
		int bracketIdx = data.indexOf("[");

		// multi editor writes to the data.json file can cause an undefined string
		// before the json object, remove it
		if (braceIdx > 0 && (braceIdx < bracketIdx || bracketIdx == -1)) {
			// there's something before the 1st brace
			data = data.substring(braceIdx);
		} else if (bracketIdx > 0 && (bracketIdx < braceIdx || braceIdx == -1)) {
			// there's something before the 1st bracket
			data = data.substring(bracketIdx);
		}

		return data;
	}

}
