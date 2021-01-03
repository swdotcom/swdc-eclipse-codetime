package com.swdc.codetime.tree;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
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

import com.swdc.codetime.managers.SwitchAccountManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.SlackManager;

public class MetricsTreeView extends ViewPart implements ISelectionListener {

	private MetricsTreeContentProvider contentProvider;
	private MetricsTreeLabelProvider labelProvider;
	private TreeViewer tv;

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

		tv.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				MetricTreeNode node = (MetricTreeNode) event.getElement();
				if (node != null) {
					node.getLabel();
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				MetricTreeNode node = (MetricTreeNode) event.getElement();
				if (node != null) {
					node.getLabel();
				}
			}
		});

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					TreeSelection selection = (TreeSelection) event.getSelection();
					if (selection.getFirstElement() != null) {
						MetricTreeNode node = (MetricTreeNode) selection.getFirstElement();
						String id = node.getId();
						if (id.equals(MetricLabels.VIEW_SUMMARY_ID)) {
							SoftwareCoSessionManager.launchCodeTimeMetricsDashboard(UIInteractionType.click);
						} else if (id.equals(MetricLabels.ADVANCED_METRICS_ID)) {
							SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.click);
						} else if (id.equals(MetricLabels.TOGGLE_METRICS_ID)) {
							SoftwareCoUtils.toggleStatusBarText(UIInteractionType.click);
						} else if (id.equals(MetricLabels.SEND_FEEDBACK_ID)) {
							SoftwareCoUtils.submitFeedback();
						} else if (id.equals(MetricLabels.LEARN_MORE_ID)) {
							SoftwareCoSessionManager.getInstance().launchReadmeFile();
						} else if (id.equals(MetricLabels.GOOGLE_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("google", false);
						} else if (id.equals(MetricLabels.GITHUB_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("github", false);
						} else if (id.equals(MetricLabels.EMAIL_SIGNUP_ID)) {
							SoftwareCoSessionManager.launchLogin("email", false);
						} else if (id.equals(MetricLabels.SWITCH_ACCOUNT_ID)) {
							SwitchAccountManager.initiateSwitchAccountFlow();
						} else if (id.equals(MetricLabels.SWITCH_ON_DND_ID)) {
							SlackManager.enableSlackNotifications(() -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.SWITCH_OFF_DND_ID)) {
							SlackManager.pauseSlackNotifications(() -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.SET_PRESENCE_ACTIVE_ID)) {
							SlackManager.toggleSlackPresence("auto", () -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.SET_PRESENCE_AWAY_ID)) {
							SlackManager.toggleSlackPresence("away", () -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.TOGGLE_DOCK_POSITION_ID)) {
							AppleScriptManager.toggleDock();
						} else if (id.equals(MetricLabels.CONNECT_SLACK_ID)) {
							SlackManager.connectSlackWorkspace(() -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.SWITCH_ON_DARK_MODE_ID)) {
							AppleScriptManager.toggleDarkMode(() -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.SWITCH_OFF_DARK_MODE_ID)) {
							AppleScriptManager.toggleDarkMode(() -> {
								WallClockManager.refreshTree();
							});
						} else if (id.equals(MetricLabels.ADD_WORKSPACE_ID)) {
							SlackManager.connectSlackWorkspace(() -> {
								WallClockManager.refreshTree();
							});
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
			refreshingTree = true;

			try {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						try {
							Object[] expandedEls = tv.getExpandedElements();
							contentProvider.buildTreeNodes();
							if (expandedEls != null) {
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
