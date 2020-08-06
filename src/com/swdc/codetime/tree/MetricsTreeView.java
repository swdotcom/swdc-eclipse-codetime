package com.swdc.codetime.tree;

import java.io.File;

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

import com.swdc.codetime.managers.ReportManager;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;

public class MetricsTreeView extends ViewPart implements ISelectionListener {

	private MetricsTreeContentProvider contentProvider;
	private MetricsTreeLabelProvider labelProvider;
	private TreeViewer tv;

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
				// MetricsTreeNode node = (MetricsTreeNode) event.getElement();
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				// MetricsTreeNode node = (MetricsTreeNode) event.getElement();
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
							SoftwareCoSessionManager.launchCodeTimeMetricsDashboard();
						} else if (id.equals("webDashboardItem")) {
							SoftwareCoSessionManager.launchWebDashboard();
						} else if (id.equals("toggleStatusTextItem")) {
							SoftwareCoUtils.toggleStatusBarText();
						} else if (id.equals("submitFeedbackItem")) {
							SoftwareCoUtils.submitFeedback();
						} else if (id.equals("learnMoreItem")) {
							SoftwareCoSessionManager.getInstance().launchReadmeFile();
						} else if (id.equals("googleSignupItem")) {
							SoftwareCoSessionManager.launchLogin("google");
						} else if (id.equals("githubSignupItem")) {
							SoftwareCoSessionManager.launchLogin("github");
						} else if (id.equals("emailSignupItem")) {
							SoftwareCoSessionManager.launchLogin("email");
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
		if (contentProvider != null) {

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
					}
				}
			});

		}
	}

}
