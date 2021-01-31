package com.swdc.codetime.tree;

import javax.swing.SwingUtilities;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.swdc.codetime.managers.AuthPromptManager;
import com.swdc.codetime.managers.FlowManager;
import com.swdc.codetime.managers.ScreenManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;

public class MetricsTreeView extends ViewPart implements ISelectionListener {

	private MetricsTreeContentProvider contentProvider;
	private MetricsTreeLabelProvider labelProvider;
	private TreeViewer tv;
	Object[] expandedEls;

	private boolean refreshingTree = false;

	public MetricsTreeView() {
		super();
	}

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}

	@Override
	public void createPartControl(Composite parent) {
		contentProvider = new MetricsTreeContentProvider();

		labelProvider = new MetricsTreeLabelProvider();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		tv = new TreeViewer(composite);
		tv.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		tv.setContentProvider(contentProvider);
		tv.setLabelProvider(labelProvider);
		tv.setInput(MetricsTreeContentProvider.ROOT_KEY);

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					TreeSelection selection = (TreeSelection) event.getSelection();
					if (selection.getFirstElement() != null) {
						MetricTreeNode node = (MetricTreeNode) selection.getFirstElement();
						String id = node.getId();
						String parentId = node.getParent() != null ? ((MetricTreeNode)node.getParent()).getId() : null;
						if (id.equals(MetricsTreeContentProvider.SIGN_UP_ID)) {
							AuthPromptManager.initiateSignupFlow();
						} else if (id.equals(MetricsTreeContentProvider.LOG_IN_ID)) {
							AuthPromptManager.initiateLoginFlow();
						} else if (id.equals(MetricsTreeContentProvider.VIEW_SUMMARY_ID)) {
							SoftwareCoSessionManager.launchCodeTimeMetricsDashboard(UIInteractionType.click);
						} else if (id.equals(MetricsTreeContentProvider.ADVANCED_METRICS_ID)) {
							SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.click);
						} else if (id.equals(MetricsTreeContentProvider.TOGGLE_METRICS_ID)) {
							SoftwareCoUtils.toggleStatusBarText(UIInteractionType.click);
						} else if (id.equals(MetricsTreeContentProvider.SEND_FEEDBACK_ID)) {
							SoftwareCoUtils.submitFeedback();
						} else if (id.equals(MetricsTreeContentProvider.LEARN_MORE_ID)) {
							SoftwareCoSessionManager.getInstance().launchReadmeFile();
						} else if (id.equals(MetricsTreeContentProvider.GOOGLE_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("google", false);
						} else if (id.equals(MetricsTreeContentProvider.GITHUB_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("github", false);
						} else if (id.equals(MetricsTreeContentProvider.EMAIL_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("email", false);
						} else if (id.equals(MetricsTreeContentProvider.SWITCH_ACCOUNT_ID)) {
							AuthPromptManager.initiateSwitchAccountFlow();
						} else if (id.equals(MetricsTreeContentProvider.SWITCH_ON_DND_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.enableSlackNotifications(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SWITCH_OFF_DND_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.pauseSlackNotifications(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SET_PRESENCE_ACTIVE_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.toggleSlackPresence("auto", () -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SET_PRESENCE_AWAY_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.toggleSlackPresence("away", () -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.TOGGLE_DOCK_POSITION_ID)) {
							SwingUtilities.invokeLater(() -> {
								AppleScriptManager.toggleDock();
							});
						} else if (id.equals(MetricsTreeContentProvider.CONNECT_SLACK_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.connectSlackWorkspace(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SWITCH_ON_DARK_MODE_ID)) {
							SwingUtilities.invokeLater(() -> {
								AppleScriptManager.toggleDarkMode(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SWITCH_OFF_DARK_MODE_ID)) {
							SwingUtilities.invokeLater(() -> {
								AppleScriptManager.toggleDarkMode(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.ADD_WORKSPACE_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.connectSlackWorkspace(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.SET_SLACK_STATUS_ID)) {
							SwingUtilities.invokeLater(() -> {
								SlackManager.setProfileStatus(() -> {
									WallClockManager.refreshTree();
								});
							});
						} else if (id.equals(MetricsTreeContentProvider.TODAY_VS_AVG_ID)) {
							String refClass = FileUtilManager.getItem("reference-class", "user");
							if (refClass.equals("user")) {
								refClass = "global";
							} else {
								refClass = "user";
							}
							FileUtilManager.setItem("reference-class", refClass);
							SwingUtilities.invokeLater(() -> {
								WallClockManager.refreshTree();
							});
						} else if (parentId != null
								&& parentId.equals(MetricsTreeContentProvider.SLACK_WORKSPACES_NODE_ID)
								&& !id.equals(MetricsTreeContentProvider.ADD_WORKSPACE_ID)) {
							// show the right click menu
						} else if (id.equals(MetricsTreeContentProvider.ENTER_FULL_SCREEN_MODE_ID)) {
			                SwingUtilities.invokeLater(() -> {
			                    ScreenManager.enterFullScreenMode();
			                });
						} else if (id.equals(MetricsTreeContentProvider.EXIT_FULL_SCREEN_MODE_ID)) {
			                SwingUtilities.invokeLater(() -> {
			                    ScreenManager.exitFullScreenMode();
			                });
						} else if (id.equals(MetricsTreeContentProvider.ENABLE_FLOW_MODE_ID)) {
							FlowManager.initiateFlow();
						} else if (id.equals(MetricsTreeContentProvider.PAUSE_FLOW_MODE_ID)) {
							FlowManager.pauseFlowInitiate();
						} else if (id.equals(MetricsTreeContentProvider.SCREEN_MODE_SETTING_ID)) {
							ConfigManager.modifyScreenMode(() -> {WallClockManager.refreshTree();});
						} else if (id.equals(MetricsTreeContentProvider.PAUSE_NOTIFICATIONS_SETTING_ID)) {
							ConfigManager.modifyPauseNotifications(() -> {WallClockManager.refreshTree();});
						} else if (id.equals(MetricsTreeContentProvider.SLACK_AWAY_STATUS_SETTING_ID)) {
							ConfigManager.modifySlackAwayStatus(() -> {WallClockManager.refreshTree();});
						} else if (id.equals(MetricsTreeContentProvider.SLACK_AWAY_STATUS_TEXT_SETTING_ID)) {
							ConfigManager.modifySlackStatusText(() -> {WallClockManager.refreshTree();});
						}
					}
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		});

		getViewSite().getPage().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		//
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		//
	}

	public void refreshTree() {
		if (contentProvider != null && !refreshingTree) {

			try {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						refreshingTree = true;
						try {
							// get the expanded elements before we rebuild the tree nodes
							Object[] expandedEls = tv.getExpandedElements();
							
							// rebuild
							contentProvider.buildTreeNodes();
							
							// update the tree with the expanded elements
							if (expandedEls != null && expandedEls.length > 0) {
								tv.setExpandedElements(expandedEls);
							}
							
							tv.refresh();
							
						} catch (Exception e) {
							//
						} finally {
							refreshingTree = false;
						}
					}
				});
			} catch (Exception e) {
				//
			}

		}
	}

}
