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
	
	public static boolean enterFullScreenMode() {
		boolean shouldExpand = false;
        Shell shell = null;
        try {
        	shell = workbench.getActiveWorkbenchWindow().getShell();
            if (shell != null) {
                if (!shell.getFullScreen()) {
                	shouldExpand = true;
                }
            }
        } catch (Exception e) {
            //
        }
        if (shell != null && shouldExpand) {
            final Shell winShell = shell;
            try {
                SwingUtilities.invokeLater(() -> {
                    winShell.setFullScreen(false);
                });
            } catch (Exception e) {
                //
            }
        }
        return shouldExpand;
    }
    
    public static boolean exitFullScreenMode() {
        boolean shouldCollapse = false;
        Shell shell = null;
        try {
        	shell = workbench.getActiveWorkbenchWindow().getShell();
            if (shell != null) {
                if (shell.getFullScreen()) {
                    shouldCollapse = true;
                }
            }
        } catch (Exception e) {
            //
        }
        if (shell != null && shouldCollapse) {
            final Shell winShell = shell;
            try {
                SwingUtilities.invokeLater(() -> {
                    winShell.setFullScreen(false);
                });
            } catch (Exception e) {
                //
            }
        }
        return shouldCollapse;
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
