/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;

import com.google.gson.JsonObject;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.EventTrackerManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.UserState;

/**
 * 
 * Manages the plugin to software.com session
 *
 */
public class SoftwareCoSessionManager {

	public static final Logger LOG = Logger.getLogger("SoftwareCoSessionManager");

	private static SoftwareCoSessionManager instance = null;

	private static int lastDayOfMonth = 0;

	private static String SERVICE_NOT_AVAIL = "Our service is temporarily unavailable.\n\nPlease try again later.\n";

	public static SoftwareCoSessionManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoSessionManager();
		}
		return instance;
	}

	public synchronized static boolean isServerOnline() {
		ClientResponse resp = OpsHttpClient.softwareGet("/ping", null);
		return resp.isOk();
	}

	public static void fetchCodeTimeMetricsDashboard() {
		String summaryInfoFile = FileUtilManager.getSummaryInfoFile();
		String dashboardFile = FileUtilManager.getCodeTimeDashboardFile();

		Calendar cal = Calendar.getInstance();
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		Writer writer = null;
		
		// append the summary content
		// Our service is temporarily unavailable
		String summaryInfoContent = SoftwareCoOfflineManager.getInstance().getSessionSummaryInfoFileContent();

		lastDayOfMonth = dayOfMonth;
		String api = "/dashboard?linux=" + UtilManager.isLinux() + "&showToday=true";
		String dashboardSummary = OpsHttpClient.softwareGet(api, FileUtilManager.getItem("jwt")).getJsonStr();
		if (dashboardSummary == null || dashboardSummary.trim().isEmpty()) {
			dashboardSummary = SERVICE_NOT_AVAIL;
		}

		// write the summary content
		try {
			writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(summaryInfoFile), StandardCharsets.UTF_8));
			writer.write(dashboardSummary);
		} catch (IOException ex) {
			// Report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}

		// concat summary info with the dashboard file
		String dashboardContent = "";
		
		if (summaryInfoContent != null) {
			dashboardContent += summaryInfoContent;
		}

		// write the dashboard content to the dashboard file
		FileUtilManager.saveFileContent(dashboardFile, dashboardContent);
	}

	public static void launchSoftwareTopForty() {
		String url = "https://api.software.com/music/top40";
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the software top 40: " + url);
			SWCoreLog.logException(e);
		}
	}

	protected File getReadmeFile() {
		Bundle bundle = Platform.getBundle(CodeTimeActivator.PLUGIN_ID);
		URL fileURL = bundle.getEntry("README.txt");
		File file = null;
		try {
			file = new File(FileLocator.resolve(fileURL).toURI());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return file;
	}

	public void launchReadmeFile() throws URISyntaxException, IOException {
		File f = getReadmeFile();
		if (f == null) {
			return;
		}

		IWorkbench workbench = PlatformUI.getWorkbench();

		URI uri = f.toURI();
		// String editorSupport = "org.eclipse.ui.browser.editorSupport";
		String genericEditor = "org.eclipse.ui.genericeditor.GenericEditor";
		String defaultEditor = "org.eclipse.ui.DefaultTextEditor";
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		try {
			IDE.openEditor(page, uri, defaultEditor, true);
		} catch (Exception e1) {
			try {
				IDE.openEditor(page, uri, genericEditor, true);
			} catch (Exception e2) {
				SWCoreLog.logErrorMessage(
						"Code Time: unable to launch editor to view code time metrics, error: " + e2.getMessage());
			}
		}
		
		UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = "ct_learn_more_btn";
        elementEntity.element_location = "ct_menu_tree";
        elementEntity.color = "yellow";
        elementEntity.cta_text = "Learn more";
        elementEntity.icon_name = "document";
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
	}

	public static void launchFile(String fsPath) {
		IProject p = SoftwareCoUtils.getActiveProject();
		if (p == null) {
			return;
		}

		File f = new File(fsPath);
		if (f.exists() && f.isFile()) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			URI uri = f.toURI();

			String genericEditor = "org.eclipse.ui.genericeditor.GenericEditor";
			String defaultEditor = "org.eclipse.ui.DefaultTextEditor";
			try {
				IDE.openEditor(page, uri, defaultEditor, true);
			} catch (Exception e) {
				try {
					IDE.openEditor(page, uri, genericEditor, true);
				} catch (Exception e1) {
					try {
						IDE.openEditor(page, uri, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
					} catch (Exception e2) {
						SWCoreLog
								.logErrorMessage("Code Time: unable to launch editor to view code time metrics, error: "
										+ e.getMessage());
					}
				}
			}
		}
	}

	public static void launchCodeTimeMetricsDashboard(UIInteractionType type) {
		
		// fetch the dashboard content and write it before displaying it
		fetchCodeTimeMetricsDashboard();

		String codeTimeFile = FileUtilManager.getCodeTimeDashboardFile();

		launchFile(codeTimeFile);
		
		UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = type.equals(UIInteractionType.click) ? "ct_summary_btn" : "ct_summary_cmd";
        elementEntity.element_location = type.equals(UIInteractionType.click) ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = type.equals(UIInteractionType.click) ? "white" : null;
        elementEntity.cta_text = "View summary";
        elementEntity.icon_name = type.equals(UIInteractionType.click) ? "guage" : null;
        EventTrackerManager.getInstance().trackUIInteraction(type, elementEntity);
	}
	
	public static class LazyUserStatusFetchTask extends TimerTask {
		private int retryCount = 0;
		public LazyUserStatusFetchTask(int count) {
			this.retryCount = count;
		}
		public void run() {
			lazilyFetchUserStatus(this.retryCount);
		}
	}

	protected static void lazilyFetchUserStatus(int retryCount) {
		UserState userState = AccountManager.getUserLoginState(false);

		if (!userState.loggedIn) {
			if (retryCount > 0) {
				final int newRetryCount = retryCount - 1;
			
				new Timer().schedule(new LazyUserStatusFetchTask(newRetryCount), 1000 * 10);
			} else {
                // clear the auth callback state
                FileUtilManager.setBooleanItem("switching_account", false);
                FileUtilManager.setAuthCallbackState(null);
			}
		} else {
			SessionDataManager.refreshSessionDataAndTree();
		}
	}

	public static void launchLogin(String loginType, boolean switching_account) {

		String auth_callback_state = FileUtilManager.getAuthCallbackState();

        FileUtilManager.setBooleanItem("switching_account", switching_account);

        String plugin_uuid = FileUtilManager.getPluginUuid();

        JsonObject obj = new JsonObject();
        obj.addProperty("plugin", "codetime");
        obj.addProperty("plugin_uuid", plugin_uuid);
        obj.addProperty("pluginVersion", SoftwareCoUtils.getVersion());
        obj.addProperty("plugin_id", SoftwareCoUtils.pluginId);
        obj.addProperty("auth_callback_state", auth_callback_state);
        obj.addProperty("redirect", SoftwareCoUtils.launch_url);

		String url = "";
		String element_name = "ct_sign_up_email_btn";
        String cta_text = "Sign up with email";
        String icon_name = "envelope";
        String icon_color = "blue";
        if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
            element_name = "ct_sign_up_email_btn";
            cta_text = "Sign up with email";
            icon_name = "envelope";
            icon_color = "gray";
            url = SoftwareCoUtils.launch_url + "/email-signup";
        } else if (loginType.equals("google")) {
            url = SoftwareCoUtils.api_endpoint + "/auth/google";
        } else if (loginType.equals("github")) {
            element_name = "ct_sign_up_github_btn";
            cta_text = "Sign up with GitHub";
            icon_name = "github";
            url = SoftwareCoUtils.api_endpoint + "/auth/github";
        }
		
		StringBuffer sb = new StringBuffer();
        Iterator<String> keys = obj.keySet().iterator();
        while(keys.hasNext()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            String key = keys.next();
            String val = obj.get(key).getAsString();
            try {
                val = URLEncoder.encode(val, "UTF-8");
            } catch (Exception e) {
                LOG.info("Unable to url encode value, error: " + e.getMessage());
            }
            sb.append(key).append("=").append(val);
        }
        url += "?" + sb.toString();

		FileUtilManager.setItem("authType", loginType);

		try {
			UtilManager.launchUrl(url);
		} catch (Exception e) {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
			} catch (Exception ex) {
				SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
				SWCoreLog.logException(ex);
			}
		}
		
		UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = element_name;
        elementEntity.element_location = "ct_menu_tree";
        elementEntity.color = icon_color;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = icon_name;
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
		
		new Timer().schedule(new LazyUserStatusFetchTask(40), 1000 * 10);
	}

	public static void launchWebDashboard(UIInteractionType type) {
		String url = SoftwareCoUtils.webui_login_url;
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
			
			UIElementEntity elementEntity = new UIElementEntity();
	        elementEntity.element_name = type.equals(UIInteractionType.click) ? "ct_web_metrics_btn" : "ct_web_metrics_cmd";
	        elementEntity.element_location = type.equals(UIInteractionType.click) ? "ct_menu_tree" : "ct_command_palette";
	        elementEntity.color = type.equals(UIInteractionType.click) ? "blue" : null;
	        elementEntity.cta_text = "See advanced metrics";
	        elementEntity.icon_name = type.equals(UIInteractionType.click) ? "paw" : null;
	        EventTrackerManager.getInstance().trackUIInteraction(type, elementEntity);
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
			SWCoreLog.logException(e);
		}
	}

}