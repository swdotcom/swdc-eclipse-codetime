package com.swdc.codetime.managers;

import javax.swing.SwingUtilities;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;

public class ScreenManager {

	public static IWorkbench workbench = null;

	public static boolean isFullScreen() {
		if (workbench != null) {
			try {
				Shell shell = workbench.getActiveWorkbenchWindow().getShell();
				return shell.getFullScreen();
			} catch (Exception e) {
				//
			}
		}
		return false;
	}

	public static void toggleFullScreen() {
		if (workbench != null) {
			workbench.getDisplay().asyncExec(() -> {
				try {
					Shell shell = workbench.getActiveWorkbenchWindow().getShell();
					shell.setFullScreen(!shell.getFullScreen());
					SwingUtilities.invokeLater(() -> {
						WallClockManager.refreshTree();
					});
				} catch (Exception e) {
					//
				}
			});
		}
	}
}
