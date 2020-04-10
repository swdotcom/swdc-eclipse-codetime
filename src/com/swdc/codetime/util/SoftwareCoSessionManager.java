/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.http.client.methods.HttpGet;
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
import com.swdc.codetime.Activator;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.managers.WallClockManager;

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
	private static long lastAppAvailableCheck = 0;

	public static SoftwareCoSessionManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoSessionManager();
		}
		return instance;
	}

	public static boolean softwareSessionFileExists() {
		// don't auto create the file
		String file = FileManager.getSoftwareSessionFile(false);
		// check if it exists
		File f = new File(file);
		return f.exists();
	}

	public static boolean jwtExists() {
		String jwt = FileManager.getItem("jwt");
		return (jwt != null && !jwt.equals(""));
	}

	public synchronized static boolean isServerOnline() {
		long nowInSec = Math.round(System.currentTimeMillis() / 1000);
		boolean pastThreshold = (nowInSec - lastAppAvailableCheck > 60) ? true : false;
		if (pastThreshold) {
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
			SoftwareCoUtils.updateServerStatus(resp.isOk());
			lastAppAvailableCheck = nowInSec;
		}
		return SoftwareCoUtils.isAppAvailable();
	}

	public void storePayload(String payload) {
		if (payload == null || payload.length() == 0) {
			return;
		}
		if (SoftwareCoUtils.isWindows()) {
			payload += "\r\n";
		} else {
			payload += "\n";
		}
		String dataStoreFile = FileManager.getSoftwareDataStoreFile();
		File f = new File(dataStoreFile);
		try {
			Writer output;
			output = new BufferedWriter(new FileWriter(f, true)); // clears file every time
			output.append(payload);
			output.close();
		} catch (Exception e) {
			SWCoreLog.logErrorMessage("Code Time: Error appending to the Software data store file");

			SWCoreLog.logException(e);
		}
	}

	public static void fetchCodeTimeMetricsDashboard(JsonObject summary) {
		String summaryInfoFile = FileManager.getSummaryInfoFile(true);
		String dashboardFile = FileManager.getCodeTimeDashboardFile();

		Calendar cal = Calendar.getInstance();
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		Writer writer = null;

		if (lastDayOfMonth == 0 || lastDayOfMonth != dayOfMonth) {
			lastDayOfMonth = dayOfMonth;
			String api = "/dashboard?linux=" + SoftwareCoUtils.isLinux() + "&showToday=false";
			String dashboardSummary = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
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
		}

		// concat summary info with the dashboard file
		String dashboardContent = "";
		SimpleDateFormat formatDayTime = new SimpleDateFormat("EEE, MMM d h:mma");
		SimpleDateFormat formatDay = new SimpleDateFormat("EEE, MMM d");
		String lastUpdatedStr = formatDayTime.format(new Date());
		dashboardContent += "Code Time          (Last updated on " + lastUpdatedStr + ")";
		dashboardContent += "\n\n";
		String todayStr = formatDay.format(new Date());
		dashboardContent += SoftwareCoUtils.getSectionHeader("Today (" + todayStr + ")");

		if (summary != null) {
			int currentDayMinutes = 0;
			if (summary.has("currentDayMinutes")) {
				currentDayMinutes = summary.get("currentDayMinutes").getAsInt();
			}
			int averageDailyMinutes = 0;
			if (summary.has("averageDailyMinutes")) {
				averageDailyMinutes = summary.get("averageDailyMinutes").getAsInt();
			}

			String currentDayTimeStr = SoftwareCoUtils.humanizeMinutes(currentDayMinutes);
			String averageDailyMinutesTimeStr = SoftwareCoUtils.humanizeMinutes(averageDailyMinutes);

			dashboardContent += SoftwareCoUtils.getDashboardRow("Hours coded today", currentDayTimeStr);
			dashboardContent += SoftwareCoUtils.getDashboardRow("90-day avg", averageDailyMinutesTimeStr);
			dashboardContent += "\n";
		}

		// append the summary content
		String summaryInfoContent = SoftwareCoOfflineManager.getInstance().getSessionSummaryInfoFileContent();
		if (summaryInfoContent != null) {
			dashboardContent += summaryInfoContent;
		}

		// write the dashboard content to the dashboard file
		FileManager.saveFileContent(dashboardContent, dashboardFile);
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
		Bundle bundle = Platform.getBundle(SWCorePlugin.ID);
		URL fileURL = bundle.getEntry("files/README.html");
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
		String editorSupport = "org.eclipse.ui.browser.editorSupport";
		String genericEditor = "org.eclipse.ui.genericeditor.GenericEditor";
		String defaultEditor = "org.eclipse.ui.DefaultTextEditor";
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		try {
			IDE.openEditor(page, uri, editorSupport, true);
		} catch (Exception e) {
			try {
				IDE.openEditor(page, uri, defaultEditor, true);
			} catch (Exception e1) {
				try {
					IDE.openEditor(page, uri, genericEditor, true);
				} catch (Exception e2) {
					SWCoreLog.logErrorMessage(
							"Code Time: unable to launch editor to view code time metrics, error: " + e.getMessage());
				}
			}
		}
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

	public static void launchCodeTimeMetricsDashboard() {
		// fetch content and open it
		IProject p = SoftwareCoUtils.getActiveProject();
		if (p == null) {
			return;
		}

		JsonObject sessionSummary = FileManager.getSessionSummaryFileAsJson();
		fetchCodeTimeMetricsDashboard(sessionSummary);

		String codeTimeFile = FileManager.getCodeTimeDashboardFile();

		launchFile(codeTimeFile);
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
		SoftwareCoUtils.UserStatus userStatus = SoftwareCoUtils.getUserStatus();

		if (!userStatus.loggedIn && retryCount > 0) {
			final int newRetryCount = retryCount - 1;
			
			new Timer().schedule(new LazyUserStatusFetchTask(newRetryCount), 1000 * 10);
		} else if (userStatus.loggedIn) {
			
			// show the success prompt
			Activator.showLoginSuccessPrompt();
			
			// update the session summary info to update the statusbar and tree
			new Thread(() -> {
				try {
					Thread.sleep(1000 * 5);
					WallClockManager.getInstance().updateSessionSummaryFromServer();
				} catch (Exception e) {
					System.err.println(e);
				}
			}).start();
		}
	}

	public static void launchLogin(String loginType) {

		String jwt = FileManager.getItem("jwt");

		try {
			jwt = URLEncoder.encode(jwt, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			//
		}

		String url = "";
		if (loginType.equals("email")) {
			url = SoftwareCoUtils.launch_url + "/email-signup?token=" + jwt + "&plugin=codetime&auth=software";
		} else if (loginType.equals("google")) {
			url = SoftwareCoUtils.api_endpoint + "/auth/google?token=" + jwt + "&plugin=codetime&redirect="
					+ SoftwareCoUtils.launch_url;
		} else if (loginType.equals("github")) {
			url = SoftwareCoUtils.api_endpoint + "/auth/github?token=" + jwt + "&plugin=codetime&redirect="
					+ SoftwareCoUtils.launch_url;
		} else {
			url = SoftwareCoUtils.launch_url + "/onboarding?token=" + jwt;
		}

		FileManager.setItem("authType", loginType);

		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
			SWCoreLog.logException(e);
		}
		
		new Timer().schedule(new LazyUserStatusFetchTask(35), 1000 * 10);
	}

	public static void launchWebDashboard() {
		String url = SoftwareCoUtils.webui_login_url;
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
			SWCoreLog.logException(e);
		}
	}

}