package com.swdc.codetime.managers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.swdc.codetime.CodeTimeActivator;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;

public class WallClockManager {

	public static final Logger log = Logger.getLogger("WallClockManager");

	private static WallClockManager instance = null;
	private Timer newDayCheckerTimer;

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

	private void init() {

		// check if the app is active
		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

			@Override
			public void windowOpened(IWorkbenchWindow arg0) {
			}

			// Unfocus event
			@Override
			public void windowDeactivated(IWorkbenchWindow arg0) {
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
				EventTrackerManager.getInstance().trackEditorAction("editor", "focus");
			}
		});

		long one_min = 1000 * 60;

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
			SessionDataManager.clearSessionSummaryData();

			// update the current day
			String day = UtilManager.getTodayInStandardFormat();
			FileUtilManager.setItem("currentDay", day);

			// update the last payload timestamp
			FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", 0);
		}
	}

}
