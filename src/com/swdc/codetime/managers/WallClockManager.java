package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.http.client.methods.HttpGet;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.CodeTimeSummary;
import com.swdc.codetime.models.SessionSummary;
import com.swdc.codetime.tree.MetricsTreeView;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.codetime.util.SoftwareResponse;

public class WallClockManager {

	public static final Logger log = Logger.getLogger("WallClockManager");

	private static final int SECONDS_INCREMENT = 30;

	private static WallClockManager instance = null;
	private Timer wallClockTimer;
	private Timer newDayCheckerTimer;

	private IViewPart treeView;

	private boolean isActive = true;

	private static boolean dispatching = false;

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

	public void setTreeView(IViewPart treeView) {
		this.treeView = treeView;
	}

	private void init() {

		// check if the app is active
		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

			@Override
			public void windowOpened(IWorkbenchWindow arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(IWorkbenchWindow arg0) {
				isActive = false;
			}

			@Override
			public void windowClosed(IWorkbenchWindow arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowActivated(IWorkbenchWindow arg0) {
				isActive = true;
			}
		});

		long one_min = 1000 * 60;

		wallClockTimer = new Timer();
		wallClockTimer.scheduleAtFixedRate(new UpdateWallClockTimeTask(), 1000 * 5, 1000 * SECONDS_INCREMENT);

		newDayCheckerTimer = new Timer();
		newDayCheckerTimer.scheduleAtFixedRate(new NewDayCheckerTask(), one_min, one_min * 10);
	}

	private class NewDayCheckerTask extends TimerTask {
		public void run() {
			newDayChecker();
		}
	}
	
	public void newDayChecker() {
		if (SoftwareCoUtils.isNewDay()) {
			// send the payloads
			FileManager.sendBatchData(FileManager.getSoftwareDataStoreFile(), "/data/batch");

			// send the time data
			TimeDataManager.sendOfflineTimeData();

			// send the events data
			EventManager.sendOfflineEvents();
			
			// clear the lastSavedKeystrokestats
			FileManager.clearLastSavedKeystrokestats();

			// clear the wc time and the session summary and the file change info summary
			clearWcTime();
			SessionDataManager.clearSessionSummaryData();
			TimeDataManager.clearTimeDataSummary();
			FileAggregateDataManager.clearFileChangeInfoSummaryData();

			// update the current day
			String day = SoftwareCoUtils.getTodayInStandardFormat();
			FileManager.setItem("currentDay", day);

			// update the last payload timestamp
			FileManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

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

		String jwt = FileManager.getItem("jwt");
		String api = "/sessions/summary?refresh=true";
		SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
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
			SessionSummary fetchedSummary = CodeTimeActivator.gson.fromJson(jsonObj, type);

			// clone all
			summary.clone(fetchedSummary);

			TimeDataManager.updateSessionFromSummaryApi(summary.currentDayMinutes);

			// save the file
			FileManager.writeData(SessionDataManager.getSessionDataSummaryFile(), summary);

			new Thread(() -> {
				try {
					Thread.sleep(1000 * 2);
					dispatchStatusViewUpdate();
				} catch (Exception e) {
					System.err.println(e);
				}
			}).start();
		}
	}

	private class UpdateWallClockTimeTask extends TimerTask {
		public void run() {
			if (isActive) {
				updateWcTime();
			}
		}
	}

	public void dispatchStatusViewUpdate() {
		if (!dispatching) {
			dispatching = true;

			// update the status bar
			SessionSummary summary = SessionDataManager.getSessionSummaryData();
			CodeTimeSummary ctSummary = TimeDataManager.getCodeTimeSummary();

			// String icon = SoftwareCoUtils.showingStatusText() ? "paw.png" : "clock.png";
			String msg = SoftwareCoUtils.humanizeMinutes(ctSummary.activeCodeTimeMinutes);
			String iconName = ctSummary.activeCodeTimeMinutes > summary.averageDailyMinutes ? "rocket.png" : "paw.png";
			SoftwareCoUtils.setStatusLineMessage(msg, iconName,
					"Active code time today. Click to see more from Code Time.");

			// refresh the tree
			if (treeView != null) {
				((MetricsTreeView) treeView).refreshTree();
			}
		}
		dispatching = false;
	}

	private void updateWcTime() {
		long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
		FileManager.setNumericItem("wctime", wctime);
		dispatchStatusViewUpdate();

		// update the json time data file
		TimeDataManager.updateEditorSeconds(SECONDS_INCREMENT);
	}

	private void clearWcTime() {
		setWcTime(0);
	}

	public long getWcTimeInSeconds() {
		return FileManager.getNumericItem("wctime", 0L);
	}

	public void setWcTime(long seconds) {
		FileManager.setNumericItem("wctime", seconds);
		updateWcTime();
	}

}
