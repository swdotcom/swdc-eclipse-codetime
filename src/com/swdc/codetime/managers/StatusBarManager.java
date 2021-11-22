package com.swdc.codetime.managers;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.swdc.codetime.util.SWCoreStatusBar;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class StatusBarManager {
	
	private static boolean showStatusText = true;
	private static String lastMsg = "";
	
	public static boolean showingStatusText() {
		return showStatusText;
	}
	
	public static void toggleStatusBarText(UIInteractionType type) {
		showStatusText = !showStatusText;
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
}
