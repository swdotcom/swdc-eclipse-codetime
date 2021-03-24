package com.swdc.codetime;

import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.gson.JsonParser;
import com.swdc.codetime.managers.EventTrackerManager;
import com.swdc.codetime.managers.ScreenManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.KeystrokePayload;
import com.swdc.codetime.util.KeystrokePayload.FileInfo;
import com.swdc.codetime.util.SWCoreImages;
import com.swdc.codetime.util.SWCoreLog;
import com.swdc.codetime.util.SoftwareCoFileEditorListener;
import com.swdc.codetime.util.SoftwareCoKeystrokeManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;

import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.websockets.WebsocketClient;

/**
 * The activator class controls the plug-in life cycle
 */
public class CodeTimeActivator extends AbstractUIPlugin implements IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.swdc.codetime"; //$NON-NLS-1$

	// The shared instance
	private static CodeTimeActivator plugin;

	public static final Logger LOG = Logger.getLogger("Software.com");

	public static JsonParser jsonParser = new JsonParser();

	public static ProcessKeystrokePayloadTask task = null;

	// Listeners (used to listen to file
	// events such as opened, activated, input changed, etc
	private static SoftwareCoFileEditorListener editorListener;

	// managers used by the static processing method
	private static SoftwareCoKeystrokeManager keystrokeMgr = SoftwareCoKeystrokeManager.getInstance();

	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");
	private static final Pattern NEW_LINE_TAB_PATTERN = Pattern.compile("\n\t");
	private static final Pattern TAB_PATTERN = Pattern.compile("\t");

	// private keystroke processor timer and client manager
	private static Timer keystrokesTimer;
	private Timer sendOfflineDataTimer;

	private static IViewPart ctMetricsTreeView = null;

	public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

	private static final IWorkbench workbench = PlatformUI.getWorkbench();
	
	/**
	 * The constructor
	 */
	public CodeTimeActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// initialize the plugin features
		earlyStartup();
	}

	public void earlyStartup() {
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {

				ConfigManager.init(SoftwareCoUtils.api_endpoint, SoftwareCoUtils.launch_url, SoftwareCoUtils.pluginId,
						SoftwareCoUtils.pluginName, SoftwareCoUtils.getVersion(), SoftwareCoUtils.IDE_NAME,
						SoftwareCoUtils.IDE_VERSION, ()-> {SessionDataManager.refreshSessionDataAndTree();});

				String jwt = FileUtilManager.getItem("jwt");
				if (StringUtils.isBlank(jwt)) {
					jwt = AccountManager.createAnonymousUser(false);
					if (StringUtils.isBlank(jwt)) {
						boolean serverIsOnline = SoftwareCoSessionManager.isServerOnline();
						if (!serverIsOnline) {
							showOfflinePrompt();
						}
					} else {
						initializePluginWhenReady(true);
					}
				}
				initializePluginWhenReady(false);
			}

			protected void initializePluginWhenReady(boolean initializedUser) {
				activateListener();

				// initialize plugin tasks
				initializePlugin(initializedUser);
			}

			protected void activateListener() {
				// initialize document listener
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window != null && window.getPartService().getActivePart() != null) {
					IWorkbenchPartReference partRef = window.getPartService().getActivePartReference();
					// listen for file changes
					window.getPartService().addPartListener(new SoftwareCoFileEditorListener(partRef));
				} else {
					// call again in a few seconds
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							activateListener();
						}
					}, 3000);
				}
			}

			protected void initializePlugin(boolean initializedUser) {
				String version = SoftwareCoUtils.getVersion();
				SWCoreLog.logInfoMessage("Code Time: Loaded v" + version + " on platform: " + SWT.getPlatform());

				SoftwareCoUtils.setStatusLineMessage("Code Time", "paw.png", "Loaded v" + version);

				// initialize the tracker
				EventTrackerManager.getInstance().init();
				// send the 1st event: activate
				EventTrackerManager.getInstance().trackEditorAction("editor", "activate");

				ScreenManager.init(null);

				// start the wallclock
				WallClockManager.getInstance().updateSessionSummaryFromServer();

				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				try {
					ctMetricsTreeView = window.getActivePage().findView("com.swdc.codetime.tree.metricsTreeView");
					WallClockManager.setTreeView(ctMetricsTreeView);
				} catch (Exception e) {
					System.err.println(e);
				}

				initializeUserInfo(initializedUser);

				try {
					WebsocketClient.connect();
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Websocket connect error: " + e.getMessage());
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		keystrokeMgr = null;

		EventTrackerManager.getInstance().trackEditorAction("editor", "deactivate");

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getPartService() != null) {
			//
			// Remove the editor listener
			//
			window.getPartService().removePartListener(editorListener);
		}

		//
		// Kill the timers
		//
		if (keystrokesTimer != null) {
			keystrokesTimer.cancel();
			keystrokesTimer = null;
		}

		if (sendOfflineDataTimer != null) {
			sendOfflineDataTimer.cancel();
			sendOfflineDataTimer = null;
		}
	}

	public static void displayCodeTimeMetricsTree() {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		try {
			if (window != null) {
				window.getActivePage().showView("com.swdc.codetime.tree.metricsTreeView");
				if (ctMetricsTreeView != null) {
					ctMetricsTreeView.setFocus();
					window.getActivePage().activate(ctMetricsTreeView);
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void handleFileOpenedEvent(String fileName) {
		String projectName = getActiveProjectName(fileName);
		if (fileName == null) {
			return;
		}

		initializeKeystrokeObjectGraph(projectName, fileName);

		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		fileInfo.open += 1;

		SWCoreLog.logInfoMessage("Code Time: file opened: " + fileName);

	}

	public static void handleFileClosedEvent(String fileName) {
		String projectName = getActiveProjectName(fileName);
		if (fileName == null) {
			return;
		}

		initializeKeystrokeObjectGraph(projectName, fileName);

		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		fileInfo.close += 1;

		SWCoreLog.logInfoMessage("Code Time: file closed: " + fileName);

		EventTrackerManager.getInstance().trackEditorAction("file", "close", fileName);
	}

	public static void handleBeforeChangeEvent(DocumentEvent docEvent) {
		// get filename
		String fileName = getCurrentFileName();
		// get the project name
		String projectName = getActiveProjectName(fileName);
		// make sure keystrokeCount is available
		initializeKeystrokeObjectGraph(projectName, fileName);

		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		if (keystrokeCount == null || docEvent == null || docEvent.getText() == null) {
			return;
		}
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		if (fileInfo.lines == 0) {
			if (docEvent.getDocument() != null) {
				fileInfo.lines = docEvent.getDocument().getNumberOfLines();
			} else {
				int documentLineCount = SoftwareCoUtils.getLineCount(fileName);
				fileInfo.lines = documentLineCount;
			}
		}
	}

	/**
	 * Take the changed document metadata and process them.
	 * 
	 * @param docEvent
	 */
	public static void handleChangeEvents(DocumentEvent docEvent) {
		// get filename
		String fileName = getCurrentFileName();
		// get the project name
		String projectName = getActiveProjectName(fileName);

		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		if (keystrokeCount == null || docEvent == null || docEvent.getText() == null) {
			return;
		}

		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		if (StringUtils.isBlank(fileInfo.syntax)) {
			fileInfo.syntax = SoftwareCoUtils.getSyntax(fileName);
		}

		updateFileInfoMetrics(docEvent, fileInfo, keystrokeCount);
	}

	private static void updateFileInfoMetrics(DocumentEvent docEvent, FileInfo fileInfo,
			KeystrokePayload keystrokeCount) {

		String text = docEvent.getText();
		int new_line_count = 0;
		if (docEvent.getDocument() != null) {
			new_line_count = docEvent.getDocument().getNumberOfLines();
			fileInfo.length = docEvent.getDocument().getLength();
		}

		// this will be the positive number of chars that were added
		int numKeystrokes = text.length();
		// if docEvent has a length then it's the number of chars that were deleted
		int numDeleteKeystrokes = Math.abs(docEvent.getLength() / -1);

		// contains newline characters within the text
		int linesAdded = getNewlineCount(text);
		int linesRemoved = 0;
		if (fileInfo.lines - new_line_count > 0) {
			linesRemoved = fileInfo.lines - new_line_count;
		}
		boolean hasAutoIndent = text.matches("^\\s{2,4}$") || TAB_PATTERN.matcher(text).find();
		boolean newLineAutoIndent = text.matches("^\n\\s{2,4}$") || NEW_LINE_TAB_PATTERN.matcher(text).find();

		// update the deletion keystrokes if there are lines removed
		numDeleteKeystrokes = numDeleteKeystrokes >= linesRemoved ? numDeleteKeystrokes - linesRemoved
				: numDeleteKeystrokes;

		// event updates
		if (newLineAutoIndent) {
			// it's a new line with auto-indent
			fileInfo.auto_indents += 1;
			fileInfo.linesAdded += 1;
		} else if (hasAutoIndent) {
			// it's an auto indent action
			fileInfo.auto_indents += 1;
		} else if (linesAdded == 1) {
			// it's a single new line action (single_adds)
			fileInfo.single_adds += 1;
			fileInfo.linesAdded += 1;
		} else if (linesAdded > 1) {
			// it's a multi line paste action (multi_adds)
			fileInfo.linesAdded += linesAdded;
			fileInfo.paste += 1;
			fileInfo.multi_adds += 1;
			fileInfo.is_net_change = true;
			fileInfo.characters_added += Math.abs(numKeystrokes - linesAdded);
		} else if (numDeleteKeystrokes > 0 && numKeystrokes > 0) {
			// it's a replacement
			fileInfo.replacements += 1;
			fileInfo.characters_added += numKeystrokes;
			fileInfo.characters_deleted += numDeleteKeystrokes;
		} else if (numKeystrokes > 1) {
			// pasted characters (multi_adds)
			fileInfo.paste += 1;
			fileInfo.multi_adds += 1;
			fileInfo.is_net_change = true;
			fileInfo.characters_added += numKeystrokes;
		} else if (numKeystrokes == 1) {
			// it's a single keystroke action (single_adds)
			fileInfo.add += 1;
			fileInfo.single_adds += 1;
			fileInfo.characters_added += 1;
		} else if (linesRemoved == 1) {
			// it's a single line deletion
			fileInfo.linesRemoved += 1;
			fileInfo.single_deletes += 1;
			fileInfo.characters_deleted += numDeleteKeystrokes;
		} else if (linesRemoved > 1) {
			// it's a multi line deletion and may contain characters
			fileInfo.characters_deleted += numDeleteKeystrokes;
			fileInfo.multi_deletes += 1;
			fileInfo.is_net_change = true;
			fileInfo.linesRemoved += linesRemoved;
		} else if (numDeleteKeystrokes == 1) {
			// it's a single character deletion action
			fileInfo.delete += 1;
			fileInfo.single_deletes += 1;
			fileInfo.characters_deleted += 1;
		} else if (numDeleteKeystrokes > 1) {
			// it's a multi character deletion action
			fileInfo.multi_deletes += 1;
			fileInfo.is_net_change = true;
			fileInfo.characters_deleted += numDeleteKeystrokes;
		}

		fileInfo.lines = new_line_count;
		fileInfo.keystrokes += 1;
		keystrokeCount.keystrokes += 1;
	}

	private static int getNewlineCount(String text) {
		if (text == null) {
			return 0;
		}
		Matcher matcher = NEW_LINE_PATTERN.matcher(text);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	public static void initializeKeystrokeObjectGraph(String projectName, String fileName) {
		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		if (keystrokeCount == null) {
			if (task != null) {
				// cancel the previous timer
				task.cancel();
				task = null;
			}
			//
			// Create one since it hasn't been created yet
			// and set the start time (in seconds)
			//
			keystrokeCount = new KeystrokePayload();

			//
			// Update the manager with the newly created KeystrokeCount object
			//
			keystrokeMgr.setKeystrokeCount(projectName, keystrokeCount, fileName);

			// keystroke payload timer
			keystrokesTimer = new Timer();
			task = new ProcessKeystrokePayloadTask();
			keystrokesTimer.schedule(task, 1000 * 60);
		} else {
			//
			// update the end time for files that don't match the incoming fileName
			//
			keystrokeCount.endPreviousModifiedFiles(fileName);
		}
	}

	public static class ProcessKeystrokePayloadTask extends TimerTask {
		public void run() {
			if (keystrokeMgr != null) {
				List<KeystrokePayload> list = keystrokeMgr.getKeystrokeCounts();
				for (KeystrokePayload keystrokeCount : list) {
					keystrokeCount.processKeystrokes();
				}

				keystrokeMgr.resetData();
			}
		}
	}

	private void initializeUserInfo(boolean initializedUser) {

		String readmeDisplayed = FileUtilManager.getItem("eclipse_CtReadme");

		if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
			try {
				SoftwareCoSessionManager.getInstance().launchReadmeFile();
			} catch (Exception e) {
				System.err.println(e);
			}
			FileUtilManager.setItem("eclipse_CtReadme", "true");

			CodeTimeActivator.displayCodeTimeMetricsTree();
		}
	}

	public static String getActiveProjectName(String fileName) {
		IProject project = SoftwareCoUtils.getFileProject(fileName);
		if (project != null) {
			return project.getName();
		}
		return "Unnamed";
	}

	private static String getCurrentFileName() {
		IEditorInput input = null;
		try {
			input = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart().getSite()
					.getPage().getActiveEditor().getEditorInput();
		} catch (NullPointerException e) {
			SWCoreLog.logInfoMessage(
					"Code Time: Unable to retrieve the IEditorInput from workbench window. " + e.getMessage());
		}
		if (input == null) {
			return null;
		}

		if (input instanceof IURIEditorInput) {

			URI uri = ((IURIEditorInput) input).getURI();

			//
			// Set the current file
			//
			if (uri != null && uri.getPath() != null) {
				String currentFile = uri.getPath();
				return currentFile;
			}
		}
		return null;
	}

	public static void showOfflinePrompt() {
		String infoMsg = "Our service is temporarily unavailable. We will try to reconnect again "
				+ "in 10 minutes. Your status bar will not update at this time.";

		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				MessageDialog dialog = new MessageDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), // parentShell
						"Code Time", // dialogTitle
						null, // dialogTitleImage
						infoMsg, // dialogMessage
						MessageDialog.INFORMATION, // dialogImageType
						new String[] { "Ok" }, // dialogButtonLabels
						0 // defaultIndex
				);
				dialog.close();
			}
		});
	}

	public static void showLoginSuccessPrompt() {
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				MessageDialog dialog = new MessageDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), // parentShell
						"Code Time Setup Complete", // dialogTitle
						SWCoreImages.findImage("paw.png"), // dialogTitleImage
						"Successfully logged onto Code Time", // dialogMessage
						MessageDialog.NONE, // dialogImageType
						new String[] { "Ok" }, // dialogButtonLabels
						0 // defaultIndex
				);
				dialog.open();
			}
		});
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CodeTimeActivator getDefault() {
		return plugin;
	}

}
