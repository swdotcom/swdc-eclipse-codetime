package com.swdc.codetime;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.swdc.codetime.managers.EventManager;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.SWCoreLog;
import com.swdc.codetime.util.SoftwareCoFileEditorListener;
import com.swdc.codetime.util.SoftwareCoKeystrokeCount;
import com.swdc.codetime.util.SoftwareCoKeystrokeManager;
import com.swdc.codetime.util.SoftwareCoRepoManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.codetime.util.SoftwareCoKeystrokeCount.FileInfo;

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
	private static SoftwareCoKeystrokeManager keystrokeMgr;

	// private keystroke processor timer and client manager
	private Timer timer;
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

		// add the document listener
		editorListener = new SoftwareCoFileEditorListener();

		// create the keystroke manager
		keystrokeMgr = SoftwareCoKeystrokeManager.getInstance();

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
				// initialize plugin tasks
				initializePlugin(initializedUser);

				// initialize document listener
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

				// listen for file changes
				window.getPartService().addPartListener(editorListener);
			}

			protected void initializePlugin(boolean initializedUser) {
				String version = SoftwareCoUtils.getVersion();
				SWCoreLog.logInfoMessage("Code Time: Loaded v" + version + " on platform: " + SWT.getPlatform());

				SoftwareCoUtils.setStatusLineMessage("Code Time", "paw.png", "Loaded v" + version);

				// store the activate event
				EventManager.createCodeTimeEvent("resource", "load", "EditorActivate");

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

				// keystroke payload timer
				timer = new Timer();
				timer.scheduleAtFixedRate(new ProcessKeystrokePayloadTask(), one_min, one_min);

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

		EventManager.createCodeTimeEvent("resource", "unload", "EditorDeactivate");

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
		if (timer != null) {
			timer.cancel();
			timer = null;
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

	protected static int getLineCount(String fileName) {
		try {
			Path path = Paths.get(fileName);
			int count = 0;
			synchronized (path) {
				Stream<String> stream = Files.lines(path);
				count = (int) stream.count();
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}

			return count;
		} catch (Exception e) {
			return 0;
		}
	}

	public static void handleFileOpenedEvent(String fileName) {
		String projectName = getActiveProjectName(fileName);
		if (fileName == null) {
			return;
		}

		initializeKeystrokeObjectGraph(projectName, fileName);

		SoftwareCoKeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		fileInfo.open += 1;
		int documentLineCount = getLineCount(fileName);
		fileInfo.lines = documentLineCount;

		SWCoreLog.logInfoMessage("Code Time: file opened: " + fileName);
	}

	public static void handleFileClosedEvent(String fileName) {
		String projectName = getActiveProjectName(fileName);
		if (fileName == null) {
			return;
		}

		initializeKeystrokeObjectGraph(projectName, fileName);

		SoftwareCoKeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		fileInfo.close += 1;

		SWCoreLog.logInfoMessage("Code Time: file closed: " + fileName);
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
		// make sure keystrokeCount is available
		initializeKeystrokeObjectGraph(projectName, fileName);

		SoftwareCoKeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		if (keystrokeCount == null) {
			return;
		}
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		boolean isNewLine = (docEvent.getText().matches("\n.*") || docEvent.getText().matches("\r\n.*")) ? true : false;

		String syntax = fileInfo.syntax;
		if (syntax == null || syntax.equals("")) {
			// get the grammar
			try {
				File file = new File(fileName);
				if (file != null && file.getName().contains(".")) {
					String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
					fileInfo.syntax = ext;
				}
			} catch (Exception e) {
				//
			}
		}

		if (!isNewLine) {
			int numKeystrokes = (docEvent.getText() != null && docEvent.getText().length() > 0)
					? docEvent.getText().length()
					: docEvent.getLength() / -1;
			if (numKeystrokes > 1) {
				// It's a copy and paste event
				fileInfo.paste += 1;
				LOG.info("Code Time: Copy+Paste incremented");
			} else if (numKeystrokes < 0) {
				// It's a character delete event
				fileInfo.delete += 1;
				LOG.info("Code Time: Delete incremented");
			} else if (numKeystrokes == 1) {
				// increment the specific file keystroke value
				fileInfo.add += 1;
				LOG.info("Code Time: KPM incremented");
			}
		}

		keystrokeCount.setKeystrokes(keystrokeCount.getKeystrokes() + 1);

		int documentLineCount = -1;
		try {
			documentLineCount = docEvent.getDocument().getNumberOfLines();
			if (documentLineCount >= 0) {
				int savedLines = fileInfo.lines.intValue();
				if (savedLines > 0) {
					int diff = documentLineCount - savedLines;
					if (diff < 0) {
						fileInfo.linesRemoved += Math.abs(diff);
						LOG.info("Code Time: linesRemoved incremented");
					} else if (diff > 0) {
						fileInfo.linesAdded += diff;
						LOG.info("Code Time: linesAdded incremented");
					}
				}
				fileInfo.lines = documentLineCount;
			}
		} catch (Exception e) {
			//
		}
	}

	public static void initializeKeystrokeObjectGraph(String projectName, String fileName) {
		SoftwareCoKeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		if (keystrokeCount == null) {
			//
			// Create one since it hasn't been created yet
			// and set the start time (in seconds)
			//
			keystrokeCount = new SoftwareCoKeystrokeCount();

			//
			// Update the manager with the newly created KeystrokeCount object
			//
			keystrokeMgr.setKeystrokeCount(projectName, keystrokeCount, fileName);
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

	private void initializeUserInfo(boolean initializedUser) {

		SoftwareCoUtils.getUserStatus();

		if (initializedUser) {
			// send an initial payload
			sendInstallPayload();
		}

		SoftwareCoUtils.sendHeartbeat("INITIALIZED");
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

	private class ProcessKeystrokePayloadTask extends TimerTask {
		public void run() {
			if (keystrokeMgr != null) {
				List<SoftwareCoKeystrokeCount> list = keystrokeMgr.getKeystrokeCounts();
				for (SoftwareCoKeystrokeCount keystrokeCount : list) {
					keystrokeCount.processKeystrokes();
				}
			}
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

		SoftwareCoKeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount(projectName);
		FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);

		// increment the specific file keystroke value
		fileInfo.add += 1;
		keystrokeCount.setKeystrokes(1);
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
