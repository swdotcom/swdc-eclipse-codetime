package com.swdc.codetime.tree;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

import com.swdc.codetime.managers.EventTrackerManager;
import com.swdc.codetime.managers.ReportManager;
import com.swdc.codetime.managers.SwitchAccountManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class MetricsTreeView extends ViewPart implements ISelectionListener {

	private MetricsTreeContentProvider contentProvider;
	private MetricsTreeLabelProvider labelProvider;
	private TreeViewer tv;
	
	private boolean refreshingTree = false;
	
	protected static List<String> toggleItems = Arrays.asList("ct_codetime_toggle_node",
            "ct_active_codetime_toggle_node",
            "ct_lines_added_toggle_node",
            "ct_lines_removed_toggle_node",
            "ct_keystrokes_toggle_node",
            "ct_files_changed_toggle_node",
            "ct_top_files_by_kpm_toggle_node",
            "ct_top_files_by_keystrokes_toggle_node",
            "ct_top_files_by_codetime_toggle_node",
            "ct_open_changes_toggle_node",
            "ct_committed_today_toggle_node");

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

		MetricsTreeNode[] initEls = contentProvider.getInitialExpandedElements();
		if (initEls != null) {
			try {
				tv.setExpandedElements(initEls);
			} catch (Exception e) {
				//
			}
		}

		tv.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				MetricsTreeNode node = (MetricsTreeNode) event.getElement();
				if (node != null) {
					String label = node.getLabel();
					String toggleName = getToggleItem(label);
					sendNodeToggleEvent(toggleName);
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				MetricsTreeNode node = (MetricsTreeNode) event.getElement();
				if (node != null) {
					String label = node.getLabel();
					String toggleName = getToggleItem(label);
					sendNodeToggleEvent(toggleName);
				}
			}
		});

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					TreeSelection selection = (TreeSelection) event.getSelection();
					if (selection.getFirstElement() != null) {
						MetricsTreeNode node = (MetricsTreeNode) selection.getFirstElement();
						String id = node.getId();
						if (id.equals("generateDashboardItem")) {
							SoftwareCoSessionManager.launchCodeTimeMetricsDashboard(UIInteractionType.click);
						} else if (id.equals("webDashboardItem")) {
							SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.click);
						} else if (id.equals("toggleStatusTextItem")) {
							SoftwareCoUtils.toggleStatusBarText(UIInteractionType.click);
						} else if (id.equals("submitFeedbackItem")) {
							SoftwareCoUtils.submitFeedback();
						} else if (id.equals("learnMoreItem")) {
							SoftwareCoSessionManager.getInstance().launchReadmeFile();
						} else if (id.equals("googleSignupItem")) {
							SoftwareCoSessionManager.launchLogin("google", false);
						} else if (id.equals("githubSignupItem")) {
							SoftwareCoSessionManager.launchLogin("github", false);
						} else if (id.equals("emailSignupItem")) {
							SoftwareCoSessionManager.launchLogin("email", false);
						} else if (id.equals("switchAccountItem")) {
							SwitchAccountManager.initiateSwitchAccountFlow();
						} else if (id.equals("contributionSummary")) {
							ReportManager.displayProjectContributorSummaryDashboard(node.getLabel());
						} else if (node.getData() != null && node.getData() instanceof String) {
							// this check is last since it doesn't go off of a unique id
							String fsPath = node.getData().toString();
							File f = new File(fsPath);
							if (f.isFile()) {
								SoftwareCoSessionManager.launchFile(fsPath);
							}
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
							contentProvider.refreshData();
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

	protected String getToggleItem(String label) {
		String normalizedLabel = label.replaceAll("\\s+", "");
        for (String toggleItem : toggleItems) {
            // strip off "ct_" and "_toggle_node" and replace the "_" with ""
            String normalizedToggleItem = toggleItem.replace("ct_", "").replace("_toggle_node", "").replaceAll("_", "");
            if (normalizedLabel.toLowerCase().indexOf(normalizedToggleItem) != -1) {
                return toggleItem;
            }
        }
        return null;
    }
	
	protected void sendNodeToggleEvent(String toggleItemName) {
		if (toggleItemName != null) {
            UIElementEntity uiElementEntity = new UIElementEntity();
            uiElementEntity.element_location = "ct_metrics_tree";
            uiElementEntity.element_name = toggleItemName;
            uiElementEntity.cta_text = toggleItemName;
            EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, uiElementEntity);
        }
	}

}
