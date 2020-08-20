package com.swdc.codetime;


import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.swdc.codetime.managers.EventManager;
import com.swdc.codetime.managers.EventTrackerManager;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.KeystrokePayload;
import com.swdc.codetime.util.KeystrokePayload.FileInfo;
import com.swdc.codetime.util.SWCoreLog;
import com.swdc.codetime.util.SoftwareCoFileEditorListener;
import com.swdc.codetime.util.SoftwareCoKeystrokeManager;
import com.swdc.codetime.util.SoftwareCoRepoManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class CodeTimeActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.swdc.codetime"; //$NON-NLS-1$

	// The shared instance
	private static CodeTimeActivator plugin;

	public static final Logger LOG = Logger.getLogger("Software.com");

	public static JsonParser jsonParser = new JsonParser();

	public static Gson gson;

	// Listeners (used to listen to file
	// events such as opened, activated, input changed, etc
	private static SoftwareCoFileEditorListener editorListener;

	// managers used by the static processing method
	private static SoftwareCoKeystrokeManager keystrokeMgr = SoftwareCoKeystrokeManager.getInstance();
	
	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

	// private keystroke processor timer and client manager
	private static Timer keystrokesTimer;
	private Timer userStatusTimer;
	private Timer sendOfflineDataTimer;
	private Timer repoCommitsTimer;
	private Timer repoUserTimer;

	private static int retry_counter = 0;
	private static long check_online_interval_ms = 1000 * 60 * 10;

	private static String rootDir = null;
	private static IViewPart ctMetricsTreeView = null;

	public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

	private static final IWorkbench workbench = PlatformUI.getWorkbench();
	
	public static ProcessKeystrokePayloadTask task = null;

	/**
	 * The constructor
	 */
	public CodeTimeActivator() {
		gson = new Gson();
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
				initComponent();
			}

			protected void initComponent() {
				boolean serverIsOnline = SoftwareCoSessionManager.isServerOnline();
				boolean sessionFileExists = SoftwareCoSessionManager.softwareSessionFileExists();
				boolean hasJwt = SoftwareCoSessionManager.jwtExists();
				if (!sessionFileExists || !hasJwt) {
					if (!serverIsOnline) {
						// server isn't online, check again in 10 min
						if (retry_counter == 0) {
							showOfflinePrompt();
						}
						new Thread(() -> {
							try {
								Thread.sleep(check_online_interval_ms);
								initComponent();
							} catch (Exception e) {
								System.err.println(e);
							}
						}).start();
					} else {
						// create the anon user
						String jwt = SoftwareCoUtils.createAnonymousUser(serverIsOnline);
						if (jwt == null) {
							// it failed, try again later
							if (retry_counter == 0) {
								showOfflinePrompt();
							}
							new Thread(() -> {
								try {
									Thread.sleep(check_online_interval_ms);
									initComponent();
								} catch (Exception e) {
									System.err.println(e);
								}
							}).start();
						} else {
							initializePluginWhenReady(true);
						}
					}
				} else {
					// session json already exists, continue with plugin init
					initializePluginWhenReady(false);
				}
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
				
				EventTrackerManager.getInstance().trackEditorAction("editor", "activate");

				long one_min = 1000 * 60;
				long forty_min = one_min * 40;

				// run the hourly timer
				repoCommitsTimer = new Timer();
				repoCommitsTimer.scheduleAtFixedRate(new ProcessCommitJobsTask(), one_min * 3, one_min * 25);

				repoUserTimer = new Timer();
				repoUserTimer.scheduleAtFixedRate(new ProcessRepoUsersJobsTask(), one_min * 4, one_min * 30);

				userStatusTimer = new Timer();
				userStatusTimer.scheduleAtFixedRate(new ProcessUserStatusTask(), one_min * 15, forty_min);

				// send payloads every 15 minutes
				sendOfflineDataTimer = new Timer();
				sendOfflineDataTimer.scheduleAtFixedRate(new ProcessOfflineData(), 1000 * 30, one_min * 15);

				// start the wallclock
				WallClockManager wcMgr = WallClockManager.getInstance();
				wcMgr.updateSessionSummaryFromServer();

				// initialize the lastSavedKeystrokeStats
				FileManager.getLastSavedKeystrokeStats();

				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				try {
					ctMetricsTreeView = window.getActivePage().findView("com.swdc.codetime.tree.metricsTreeView");
					wcMgr.setTreeView(ctMetricsTreeView);
				} catch (Exception e) {
					System.err.println(e);
				}

				initializeUserInfo(initializedUser);
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

		if (repoCommitsTimer != null) {
			repoCommitsTimer.cancel();
			repoCommitsTimer = null;
		}

		if (userStatusTimer != null) {
			userStatusTimer.cancel();
			userStatusTimer = null;
		}

		if (sendOfflineDataTimer != null) {
			sendOfflineDataTimer.cancel();
			sendOfflineDataTimer = null;
		}

		if (repoUserTimer != null) {
			repoUserTimer.cancel();
			repoUserTimer = null;
		}
	}

	public static void displayCodeTimeMetricsTree() {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		try {
			window.getActivePage().showView("com.swdc.codetime.tree.metricsTreeView");
			if (ctMetricsTreeView != null) {
				ctMetricsTreeView.setFocus();
				window.getActivePage().activate(ctMetricsTreeView);
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
		int documentLineCount = SoftwareCoUtils.getLineCount(fileName);
		fileInfo.lines = documentLineCount;

		SWCoreLog.logInfoMessage("Code Time: file opened: " + fileName);
		
		EventTrackerManager.getInstance().trackEditorAction("file", "open", fileName);
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
		
		if (docEvent.getDocument() != null) {
			fileInfo.lines = docEvent.getDocument().getNumberOfLines();
		} else {
			int documentLineCount = SoftwareCoUtils.getLineCount(fileName);
			fileInfo.lines = documentLineCount;
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
	
	private static void updateFileInfoMetrics(DocumentEvent docEvent, FileInfo fileInfo, KeystrokePayload keystrokeCount) {
		
		String text = docEvent.getText();
		int new_line_count = 0;
		if (docEvent.getDocument() != null) {
			new_line_count = docEvent.getDocument().getNumberOfLines();
			fileInfo.length = docEvent.getDocument().getLength();
		}
		
		int numKeystrokes = (text.length() > 0)
				? text.length()
				: docEvent.getLength() / -1;
			
				
		// matches at least 1 newline character
		boolean hasNewLine = text.matches("[\r\n]");
		// contains newline characters within the text
		int linesAdded = getNewlineCount(text);
		if (linesAdded > 1) {
            // if it's 2, it's actually 3 lines as all we're doing is counting the \n chars
            linesAdded += 1;
        }
		boolean hasAutoIndent = text.matches("[\t]");
		
		// event updates
		if (hasAutoIndent) {
			// it's an auto indent action
			fileInfo.auto_indents += 1;
		} else if (hasNewLine && linesAdded == 0) {
			// it's a single new line action (single_adds)
			fileInfo.single_adds += 1;
			fileInfo.linesAdded += 1;
		} else if (linesAdded > 0) {
			// it's a multi line paste action (multi_adds)
			fileInfo.linesAdded += linesAdded;
			fileInfo.paste += 1;
			fileInfo.multi_adds += 1;
			fileInfo.characters_added += Math.abs(numKeystrokes - linesAdded);
		} else if (numKeystrokes > 1) {
			// pasted characters (multi_adds)
			fileInfo.paste += 1;
			fileInfo.multi_adds += 1;
			fileInfo.characters_added += numKeystrokes;
		} else if (numKeystrokes == 1) {
			// it's a single keystroke action (single_adds)
			fileInfo.add += 1;
			fileInfo.single_adds += 1;
			fileInfo.characters_added += 1;
		} else if (numKeystrokes < -1) {
			// it's a multi character delete action (multi_deletes)
			int linesDeleted = fileInfo.lines - new_line_count;
			if (linesDeleted > 0) {
				fileInfo.linesRemoved += fileInfo.lines - new_line_count;
			}
			fileInfo.multi_deletes += 1;
			fileInfo.characters_deleted += Math.abs(numKeystrokes);
		} else if (numKeystrokes == -1) {
			// it's a single character delete action (single_deletes)
			fileInfo.delete += 1;
			fileInfo.single_deletes += 1;
			fileInfo.characters_deleted += 1;
		}
		
		fileInfo.lines = new_line_count;
		fileInfo.keystrokes += 1;
		keystrokeCount.keystrokes += 1;
		
		System.out.println("Keystrokes incremented");
	}
	
	private static int getNewlineCount(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = NEW_LINE_PATTERN.matcher(text);
        int count = 0;
        while(matcher.find()) {
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

		// update the rootDir
		rootDir = (keystrokeCount != null && keystrokeCount.getProject() != null)
				? keystrokeCount.getProject().directory
				: null;
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

		SoftwareCoUtils.getUserStatus();

		if (initializedUser) {
			// send an initial payload
			sendInstallPayload();
		}

		SoftwareCoUtils.sendHeartbeat("INITIALIZED");
		
		String readmeDisplayed = FileManager.getItem("eclipse_CtReadme");
		
        if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
            try {
				SoftwareCoSessionManager.getInstance().launchReadmeFile();
			} catch (Exception e) {
				System.err.println(e);
			}
            FileManager.setItem("eclipse_CtReadme", "true");
            
            CodeTimeActivator.displayCodeTimeMetricsTree();
        }
	}

	private class ProcessUserStatusTask extends TimerTask {
		public void run() {
			if (!SoftwareCoUtils.isLoggedIn()) {
				SoftwareCoUtils.getUserStatus();
			}
		}
	}

	private class ProcessOfflineData extends TimerTask {
		public void run() {
			try {
				FileManager.sendBatchData(FileManager.getSoftwareDataStoreFile(), "/data/batch");
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	private class ProcessCommitJobsTask extends TimerTask {
		public void run() {
			SoftwareCoUtils.sendHeartbeat("HOURLY");

			SoftwareCoRepoManager.getInstance().getHistoricalCommits(rootDir);

			// send the events data
			EventManager.sendOfflineEvents();

		}
	}

	private class ProcessRepoUsersJobsTask extends TimerTask {
		public void run() {
			SoftwareCoUtils.sendHeartbeat("HOURLY");

			SoftwareCoRepoManager.getInstance().processRepoMembersInfo(rootDir);

		}
	}

	protected void sendInstallPayload() {
		// get filename
		String fileName = "Untitled";
		// get the project name
		String projectName = "Unnamed";
		// make sure keystrokeCount is available
		initializeKeystrokeObjectGraph(projectName, fileName);

		KeystrokePayload keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		// increment the specific file keystroke value
		fileInfo.keystrokes = 1;
		fileInfo.add = 1;
		keystrokeCount.keystrokes = 1;
		// send the initial payload
		keystrokeCount.processKeystrokes();
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

	protected static void showOfflinePrompt() {
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
						null, // dialogTitleImage
						"Successfully logged onto Code Time", // dialogMessage
						MessageDialog.INFORMATION, // dialogImageType
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
