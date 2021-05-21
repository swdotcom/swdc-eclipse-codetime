package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.tree.MetricsTreeView;
import com.swdc.codetime.util.SoftwareCoUtils;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.CodeTimeSummary;
import swdc.java.ops.model.SessionSummary;

public class WallClockManager {

	public static final Logger log = Logger.getLogger("WallClockManager");

	private static final int SECONDS_INCREMENT = 30;

	private static WallClockManager instance = null;
	private Timer wallClockTimer;
	private Timer newDayCheckerTimer;

	private static IViewPart treeView;

	private boolean isCurrentlyActive = true;

	public static WallClockManager getInstance() {
		if (instance == null) {
			synchronized (log) {
				if (instance == null) {
					instance = new WallClockManager();
				}
			}
		}
		return instance;
	}

	private WallClockManager() {
		// initialize the timer
		this.init();
	}

	public static void setTreeView(IViewPart treeViewPart) {
		treeView = treeViewPart;
	}

	private void init() {

		// check if the app is active
		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

			@Override
			public void windowOpened(IWorkbenchWindow arg0) {
			}

			// Unfocus event
			@Override
			public void windowDeactivated(IWorkbenchWindow arg0) {
				log.info("Window deactivated");
				isCurrentlyActive = false;
				if (CodeTimeActivator.task != null) {
					// process the keystrokes
					CodeTimeActivator.task.run();
				}
				// send the unfocus event
				EventTrackerManager.getInstance().trackEditorAction("editor", "unfocus");
			}

			@Override
			public void windowClosed(IWorkbenchWindow arg0) {
			}

			// Focus event
			@Override
			public void windowActivated(IWorkbenchWindow arg0) {
				isCurrentlyActive = true;
				EventTrackerManager.getInstance().trackEditorAction("editor", "focus");
			}
		});

		long one_min = 1000 * 60;

		wallClockTimer = new Timer();
		wallClockTimer.scheduleAtFixedRate(new UpdateWallClockTimeTask(), 1000 * 5, 1000 * SECONDS_INCREMENT);

		newDayCheckerTimer = new Timer();
		newDayCheckerTimer.scheduleAtFixedRate(new NewDayCheckerTask(), one_min, one_min * 10);
	}

	public static void activeStateChangeHandler(CodeTime keystrokeCount) {
		//
	}

	private class NewDayCheckerTask extends TimerTask {
		public void run() {
			newDayChecker();
		}
	}

	public void newDayChecker() {
		if (UtilManager.isNewDay()) {

			// clear the wc time and the session summary and the file change info summary
			clearWcTime();
			SessionDataManager.clearSessionSummaryData();
			TimeDataManager.clearTimeDataSummary();
			FileAggregateDataManager.clearFileChangeInfoSummaryData();

			// update the current day
			String day = UtilManager.getTodayInStandardFormat();
			FileUtilManager.setItem("currentDay", day);

			// update the last payload timestamp
			FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

			dispatchStatusViewUpdate();

			new Thread(() -> {
				try {
					Thread.sleep(5000);
					updateSessionSummaryFromServer();
				} catch (Exception e) {
					System.err.println(e);
				}
			}).start();
		}
	}

	public void updateSessionSummaryFromServer() {
		SessionSummary summary = SessionDataManager.getSessionSummaryData();

		String jwt = FileUtilManager.getItem("jwt");
		String api = "/sessions/summary";
		ClientResponse resp = OpsHttpClient.softwareGet(api, jwt);
		try {
			if (resp.isOk()) {
				JsonObject jsonObj = resp.getJsonObj();

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

				Type type = new TypeToken<SessionSummary>() {
				}.getType();
				SessionSummary fetchedSummary = UtilManager.gson.fromJson(jsonObj, type);

				// clone all
				summary.clone(fetchedSummary);

				TimeDataManager.updateSessionFromSummaryApi(summary.getCurrentDayMinutes());

				// save the file
				FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
			}
		} catch (Exception e) {
			log.info("Error fetching averages: " + e.getMessage());
		}

		new Thread(() -> {
			try {
				dispatchStatusViewUpdate();
			} catch (Exception e) {
				System.err.println(e);
			}
		}).start();
	}

	private class UpdateWallClockTimeTask extends TimerTask {
		public void run() {
			if (isCurrentlyActive) {
				updateWcTime();
			}
		}
	}

	public void dispatchStatusViewUpdate() {
		// update the status bar
		SessionSummary summary = SessionDataManager.getSessionSummaryData();
		CodeTimeSummary ctSummary = TimeDataManager.getCodeTimeSummary();

		// String icon = SoftwareCoUtils.showingStatusText() ? "paw.png" : "clock.png";
		String msg = UtilManager.humanizeMinutes(ctSummary.activeCodeTimeMinutes);
		String iconName = ctSummary.activeCodeTimeMinutes > summary.getAverageDailyMinutes() ? "rocket.png" : "paw.png";
		SoftwareCoUtils.setStatusLineMessage(msg, iconName,
				"Active code time today. Click to see more from Code Time.");
		
	}
	
	public static void refreshTree() {
		if (treeView == null) {
			try {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IViewPart tv = window.getActivePage().findView("com.swdc.codetime.tree.metricsTreeView");
				setTreeView(tv);
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		// refresh the tree
		if (treeView != null) {
			((MetricsTreeView) treeView).refreshTree();
		}
	}

	private void updateWcTime() {
		long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
		FileUtilManager.setNumericItem("wctime", wctime);
		dispatchStatusViewUpdate();

		// update the json time data file
		TimeDataManager.updateEditorSeconds(SECONDS_INCREMENT);
	}

	private void clearWcTime() {
		setWcTime(0);
	}

	public long getWcTimeInSeconds() {
		return FileUtilManager.getNumericItem("wctime", 0L);
	}

	public void setWcTime(long seconds) {
		FileUtilManager.setNumericItem("wctime", seconds);
		updateWcTime();
	}

}
