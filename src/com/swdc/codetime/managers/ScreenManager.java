package com.swdc.codetime.managers;


import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class ScreenManager {

	private static final IWorkbench workbench = PlatformUI.getWorkbench();

	private static Shell ideWindow = null;
	private static boolean isFullScreen = false;

	public static void init(Runnable callback) {
		Display.getDefault().asyncExec(() -> {
			if (ideWindow == null && workbench.getWorkbenchWindows().length > 0) {
				try {
					if (workbench.getActiveWorkbenchWindow() == null) {
						ideWindow = workbench.getWorkbenchWindows()[0].getShell();
					} else {
						ideWindow = workbench.getActiveWorkbenchWindow().getShell();
					}
					if (callback != null) {
						callback.run();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static boolean isFullScreen() {
		return isFullScreen;
	}

	public static void enterFullScreen() {
		Display.getDefault().asyncExec(() -> {
			if (ideWindow != null && !ideWindow.getFullScreen()) {
				try {
	                ideWindow.setFullScreen(true);
	                isFullScreen = true;
	            } catch (Exception e) {
	                //
	            }
			} else {
				// try one more time
				init(() -> {ScreenManager.enterFullScreen();});

			}
		});
    }

	public static void exitFullScreen() {
		Display.getDefault().asyncExec(() -> {
			if (ideWindow != null && ideWindow.getFullScreen()) {
				try {
					ideWindow.setFullScreen(false);
					isFullScreen = false;
				} catch (Exception e) {
					//
				}
			} else {
				// try one more time
				init(() -> {ScreenManager.exitFullScreen();});

			}
		});
	}

}
