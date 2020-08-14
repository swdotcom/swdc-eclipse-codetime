package com.swdc.codetime.tree;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.swdc.codetime.managers.FileAggregateDataManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.TimeDataManager;
import com.swdc.codetime.models.CodeTimeSummary;
import com.swdc.codetime.models.CommitChangeStats;
import com.swdc.codetime.models.FileChangeInfo;
import com.swdc.codetime.models.ResourceInfo;
import com.swdc.codetime.models.SessionSummary;
import com.swdc.codetime.util.GitUtil;
import com.swdc.codetime.util.SoftwareCoProject;
import com.swdc.codetime.util.SoftwareCoUtils;

public class MetricsTreeContentProvider implements ITreeContentProvider {

	public static final String ROOT_KEY = "root";

	private Map<String, MetricsTreeNode[]> contentMap = new HashMap<String, MetricsTreeNode[]>();
	private SessionSummary summary = new SessionSummary();
	private CodeTimeSummary ctSummary = new CodeTimeSummary();
	private Map<String, FileChangeInfo> fileChangeInfoMap = new HashMap<>();
	private MetricsTreeNode[] initialExpandedElements = null;
	private ResourceInfo resourceInfo = null;
	private String dayStr = "";
	
	private boolean showingLoginButtons = false;

	public MetricsTreeContentProvider() {
		this.buildTreeNodes();
	}

	public MetricsTreeNode[] getInitialExpandedElements() {
		return initialExpandedElements;
	}
	
	private void updateSignupButtons() {
		if (SoftwareCoUtils.isLoggedIn() && showingLoginButtons) {
			// remove the login button nodes
			
			MetricsTreeNode[] nodes = contentMap.get(ROOT_KEY);
			List<MetricsTreeNode> tmp = new ArrayList<>();
			for (MetricsTreeNode node : nodes) {
				if (!node.getId().equals("googleSignupItem") &&
					!node.getId().equals("githubSignupItem") &&
					!node.getId().equals("emailSignupItem") &&
					!node.getId().equals("signupSeparator")) {
					tmp.add(node);
				}
			}
			MetricsTreeNode[] newRootNodes = Arrays.copyOf(tmp.toArray(), tmp.size(), MetricsTreeNode[].class);
			contentMap.put(ROOT_KEY, newRootNodes);
			showingLoginButtons = false;
		}
	}
	
	private void updateIdentifierButton() {
		SoftwareCoProject softwareProj = SoftwareCoUtils.getActiveKeystrokeProject();
		if (softwareProj != null) {
			resourceInfo = GitUtil.getResourceInfo(softwareProj.directory);
		} else {
			resourceInfo = null;
		}
		
		boolean hasIdentifier = (resourceInfo != null && resourceInfo.identifier != null && !resourceInfo.identifier.isEmpty())
				? true : false;
		
		MetricsTreeNode[] nodes = contentMap.get(ROOT_KEY);
		
		List<MetricsTreeNode> tmp = new ArrayList<>();
		
		// add the contributor summary information if it's available and
		// it wasn't in the previous view, or if it was and the label has changed
		// then change it, or if it's not longer available then remove it
		for (MetricsTreeNode node : nodes) {
			if (node.getId().equals("contributionSummary")) {
				if (hasIdentifier) {
					// check to see if the label should change?
					if (!node.getLabel().equals(resourceInfo.identifier)) {
						// it changed, change the label
						MetricsTreeNode contributionSummaryReportItem = new MetricsTreeNode(
								resourceInfo.identifier, "contributionSummary", "github.png");
						tmp.add(contributionSummaryReportItem);
					} else {
						// nothing's changed, add it back
						tmp.add(node);
					}
				}
			} else if (node.getId().equals("contrib-separator") || node.getId().equals("contributionSummaryTitle")) {
				if (hasIdentifier) {
					// still has an identifier, add the separator
					tmp.add(node);
				}
			} else {
				tmp.add(node);
			}
		}
		
		MetricsTreeNode[] newRootNodes = Arrays.copyOf(tmp.toArray(), tmp.size(), MetricsTreeNode[].class);
		contentMap.put(ROOT_KEY, newRootNodes);
	}

	public void refreshData() {
		
		summary = SessionDataManager.getSessionSummaryData();
		ctSummary = TimeDataManager.getCodeTimeSummary();
		fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
		SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
		dayStr = formatDay.format(new Date());
		
		// remove the sign up buttons if the user has logged on
		this.updateSignupButtons();
		
		// add or remove the identifier link
		this.updateIdentifierButton();
		
		
		MetricsTreeNode[] rootNodes = contentMap.get(ROOT_KEY);
		for (MetricsTreeNode node : rootNodes) {
			if (node.getId().equals("toggleStatusTextItem")) {
				if (SoftwareCoUtils.showingStatusText()) {
					node.setLabel("Hide status bar metrics");
				} else {
					node.setLabel("Show status bar metrics");
				}
			} else if (node.getId().equals("editor-time")) {
				// get the children
				MetricsTreeNode[] children = contentMap.get(node.getId());
				for (MetricsTreeNode child : children) {
					if (child.getId().equals("editortime-today-val")) {
						String val = "Today: " + SoftwareCoUtils.humanizeMinutes(ctSummary.codeTimeMinutes);
						child.setLabel(val);
					}
				}
			} else if (node.getId().equals("code-time")) {
				MetricsTreeNode[] children = contentMap.get(node.getId());
				for (MetricsTreeNode child : children) {
					if (child.getId().equals("codetime-today-val")) {
						String val = "Today: " + SoftwareCoUtils.humanizeMinutes(ctSummary.activeCodeTimeMinutes);
						child.setLabel(val);
					} else if (child.getId().equals("codetime-avg-val")) {
						String boltIcon = ctSummary.activeCodeTimeMinutes > summary.averageDailyMinutes ? "bolt.png"
								: "bolt-grey.png";
						String val = "Your average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeMinutes(summary.averageDailyMinutes);
						child.setLabel(val);
						child.setIconName(boltIcon);
					} else if (child.getId().equals("codetime-global-val")) {
						String val = "Global average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeMinutes(summary.globalAverageDailyMinutes);
						child.setLabel(val);
					}
				}
			} else if (node.getId().equals("lines-added")) {
				MetricsTreeNode[] children = contentMap.get(node.getId());
				for (MetricsTreeNode child : children) {
					if (child.getId().equals("linesadded-today-val")) {
						String val = "Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayLinesAdded);
						child.setLabel(val);
					} else if (child.getId().equals("linesadded-avg-val")) {
						String boltIcon = summary.currentDayLinesAdded > summary.averageLinesAdded ? "bolt.png"
								: "bolt-grey.png";
						String val = "Your average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.averageLinesAdded);
						child.setLabel(val);
						child.setIconName(boltIcon);
					} else if (child.getId().equals("linesadded-global-val")) {
						String val = "Global average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageLinesAdded);
						child.setLabel(val);
					}
				}
			} else if (node.getId().equals("lines-removed")) {
				MetricsTreeNode[] children = contentMap.get(node.getId());
				for (MetricsTreeNode child : children) {
					if (child.getId().equals("linesremoved-today-val")) {
						String val = "Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayLinesRemoved);
						child.setLabel(val);
					} else if (child.getId().equals("linesremoved-avg-val")) {
						String val = "Your average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.averageLinesRemoved);
						String boltIcon = summary.currentDayLinesRemoved > summary.averageLinesRemoved ? "bolt.png"
								: "bolt-grey.png";
						child.setLabel(val);
						child.setIconName(boltIcon);
					} else if (child.getId().equals("linesremoved-global-val")) {
						String val = "Global average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageLinesRemoved);
						child.setLabel(val);
					}
				}
			} else if (node.getId().equals("keystrokes")) {
				MetricsTreeNode[] children = contentMap.get(node.getId());
				for (MetricsTreeNode child : children) {
					if (child.getId().equals("keystrokes-today-val")) {
						String val = "Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayKeystrokes);
						child.setLabel(val);
					} else if (child.getId().equals("keystrokes-avg-val")) {
						String val = "Your average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.averageDailyKeystrokes);
						String boltIcon = summary.currentDayKeystrokes > summary.averageDailyKeystrokes ? "bolt.png"
								: "bolt-grey.png";
						child.setLabel(val);
						child.setIconName(boltIcon);
					} else if (child.getId().equals("keystrokes-global-val")) {
						String val = "Global average (" + dayStr + "): "
								+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageDailyKeystrokes);
						child.setLabel(val);
					}
				}
			} else if (node.getId().equals("uncommitted") || node.getId().equals("committed")) {
				MetricsTreeNode[] children = contentMap.get(node.getId());
				// these children are project folders
				if (children != null && children.length > 0) {
					for (MetricsTreeNode project : children) {
						String fsPath = (String) project.getData();
						CommitChangeStats stats = null;
						if (node.getId().equals("uncommitted")) {
							stats = GitUtil.getUncommitedChanges(fsPath);
						} else {
							stats = GitUtil.getTodaysCommits(fsPath, null);
						}
						// update it's child node
						MetricsTreeNode[] statChildren = contentMap.get(project.getId());
						for (MetricsTreeNode child : statChildren) {
							if (child.getId().equals("insertions-" + project.getId())) {
								String insertions = "insertion(s): "
										+ SoftwareCoUtils.humanizeLongNumbers(stats.insertions);
								child.setLabel(insertions);
							} else if (child.getId().equals("deletions-" + project.getId())) {
								String deletions = "deletion(s): "
										+ SoftwareCoUtils.humanizeLongNumbers(stats.deletions);
								child.setLabel(deletions);
							} else if (stats.committed && child.getId().equals("commits-" + project.getId())) {
								String commits = "commit(s): " + SoftwareCoUtils.humanizeLongNumbers(stats.commitCount);
								child.setLabel(commits);
							} else if (stats.committed && child.getId().equals("fileschanged-" + project.getId())) {
								String filesChanged = "files changed: "
										+ SoftwareCoUtils.humanizeLongNumbers(stats.fileCount);
								child.setLabel(filesChanged);
							}
						}
					}
				}
			}
		}
	}

	public void buildTreeNodes() {

		List<MetricsTreeNode> mNodeList = new ArrayList<>();
		SoftwareCoProject softwareProj = SoftwareCoUtils.getActiveKeystrokeProject();
		if (softwareProj != null) {
			resourceInfo = GitUtil.getResourceInfo(softwareProj.directory);
		} else {
			resourceInfo = null;
		}
		boolean hasIdentifier = (resourceInfo != null && resourceInfo.identifier != null && !resourceInfo.identifier.isEmpty())
				? true : false;
		contentMap = new HashMap<String, MetricsTreeNode[]>();
		summary = SessionDataManager.getSessionSummaryData();
		ctSummary = TimeDataManager.getCodeTimeSummary();
		fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
		SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
		dayStr = formatDay.format(new Date());

		List<MetricsTreeNode> rootNodes = new ArrayList<>();
		
		if (!SoftwareCoUtils.isLoggedIn()) {
			showingLoginButtons = true;
			MetricsTreeNode googleLoginItem = new MetricsTreeNode("Sign up with Google", "googleSignupItem", "google.png");
			mNodeList.add(googleLoginItem);
			MetricsTreeNode githubLoginItem = new MetricsTreeNode("Sign up with GitHub", "githubSignupItem", "github.png");
			mNodeList.add(githubLoginItem);
			MetricsTreeNode emailLoginItem = new MetricsTreeNode("Sign up using email", "emailSignupItem", "icons8-envelope-16.png");
			mNodeList.add(emailLoginItem);
			MetricsTreeNode signupMenuSepItem = new MetricsTreeNode("", "signupSeparator");
			signupMenuSepItem.setSeparator(true);
			mNodeList.add(signupMenuSepItem);
		}

		// menu and metric roots
		MetricsTreeNode webDashboardItem = new MetricsTreeNode("See advanced metrics", "webDashboardItem", "paw.png");
		mNodeList.add(webDashboardItem);
		MetricsTreeNode generateDashboardItem = new MetricsTreeNode("View summary", "generateDashboardItem",
				"dashboard.png");
		mNodeList.add(generateDashboardItem);
		MetricsTreeNode toggleStatusTextItem = new MetricsTreeNode("Hide status bar metrics", "toggleStatusTextItem",
				"visible.png");
		mNodeList.add(toggleStatusTextItem);
		MetricsTreeNode learnMoreItem = new MetricsTreeNode("Learn more", "learnMoreItem", "readme.png");
		mNodeList.add(learnMoreItem);
		MetricsTreeNode submitFeedbackItem = new MetricsTreeNode("Submit feedback", "submitFeedbackItem",
				"message.png");
		mNodeList.add(submitFeedbackItem);
		MetricsTreeNode menuSepItem = new MetricsTreeNode("", "menu-separator");
		menuSepItem.setSeparator(true);
		mNodeList.add(menuSepItem);

		MetricsTreeNode editorTimeItem = new MetricsTreeNode("Code time", "editor-time");
		mNodeList.add(editorTimeItem);
		MetricsTreeNode codeTimeItem = new MetricsTreeNode("Active code time", "code-time");
		mNodeList.add(codeTimeItem);
		MetricsTreeNode linesAddedItem = new MetricsTreeNode("Lines added", "lines-added");
		mNodeList.add(linesAddedItem);
		MetricsTreeNode linesRemovedItem = new MetricsTreeNode("Lines removed", "lines-removed");
		mNodeList.add(linesRemovedItem);
		MetricsTreeNode keystrokesItem = new MetricsTreeNode("Keystrokes", "keystrokes");
		mNodeList.add(keystrokesItem);
		MetricsTreeNode topKpmFilesItem = new MetricsTreeNode("Top files by KPM", "top-kpm-files");
		mNodeList.add(topKpmFilesItem);
		MetricsTreeNode topKeystrokesFilesItem = new MetricsTreeNode("Top files by keystrokes", "top-keystroke-files");
		mNodeList.add(topKeystrokesFilesItem);
		MetricsTreeNode topCodeTimeFilesItem = new MetricsTreeNode("Top files by code time", "top-codetime-files");
		mNodeList.add(topCodeTimeFilesItem);

		MetricsTreeNode metricsSepItem = new MetricsTreeNode("", "metrics-separator");
		metricsSepItem.setSeparator(true);
		mNodeList.add(metricsSepItem);

		MetricsTreeNode openChangesItem = new MetricsTreeNode("Open changes", "uncommitted");
		mNodeList.add(openChangesItem);
		MetricsTreeNode committedTodayItem = new MetricsTreeNode("Committed today", "committed");
		mNodeList.add(committedTodayItem);
		
		if (hasIdentifier) {
			MetricsTreeNode contributorSepItem = new MetricsTreeNode("", "contrib-separator");
			contributorSepItem.setSeparator(true);
			mNodeList.add(contributorSepItem);
			MetricsTreeNode projectSummaryTitle = new MetricsTreeNode("Project Summary", "contributionSummaryTitle");
			mNodeList.add(projectSummaryTitle);
			MetricsTreeNode contributionSummaryReportItem = new MetricsTreeNode(
					resourceInfo.identifier, "contributionSummary", "github.png");
			mNodeList.add(contributionSummaryReportItem);
		}

		MetricsTreeNode[] roots = Arrays.copyOf(mNodeList.toArray(), mNodeList.size(), MetricsTreeNode[].class);
		contentMap.put(ROOT_KEY, roots);

		// editor time
		String editorMinutes = SoftwareCoUtils.humanizeMinutes(ctSummary.codeTimeMinutes);
		MetricsTreeNode editorTimeTodayVal = new MetricsTreeNode("Today: " + editorMinutes, "editortime-today-val",
				"rocket.png");
		MetricsTreeNode[] editorTimeChildren = { editorTimeTodayVal };
		contentMap.put(editorTimeItem.getId(), editorTimeChildren);

		// code time
		MetricsTreeNode codeTimeTodayVal = new MetricsTreeNode(
				"Today: " + SoftwareCoUtils.humanizeMinutes(ctSummary.activeCodeTimeMinutes), "codetime-today-val",
				"rocket.png");
		String boltIcon = ctSummary.activeCodeTimeMinutes > summary.averageDailyMinutes ? "bolt.png" : "bolt-grey.png";
		MetricsTreeNode codeTimeAvgVal = new MetricsTreeNode(
				"Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeMinutes(summary.averageDailyMinutes),
				"codetime-avg-val", boltIcon);
		MetricsTreeNode codeTimeGlobalAvgVal = new MetricsTreeNode(
				"Global average (" + dayStr + "): "
						+ SoftwareCoUtils.humanizeMinutes(summary.globalAverageDailyMinutes),
				"codetime-global-val", "global-grey.png");
		MetricsTreeNode[] codeTimeChildren = { codeTimeTodayVal, codeTimeAvgVal, codeTimeGlobalAvgVal };
		contentMap.put(codeTimeItem.getId(), codeTimeChildren);

		// lines added
		MetricsTreeNode linesAddedTodayVal = new MetricsTreeNode(
				"Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayLinesAdded), "linesadded-today-val",
				"rocket.png");
		String laddedboltIcon = summary.currentDayLinesAdded > summary.averageLinesAdded ? "bolt.png" : "bolt-grey.png";
		MetricsTreeNode linesAddedAvgVal = new MetricsTreeNode(
				"Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(summary.averageLinesAdded),
				"linesadded-avg-val", laddedboltIcon);
		MetricsTreeNode linesAddedGlobalAvgVal = new MetricsTreeNode(
				"Global average (" + dayStr + "): "
						+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageLinesAdded),
				"linesadded-global-val", "global-grey.png");
		MetricsTreeNode[] linesAddedChildren = { linesAddedTodayVal, linesAddedAvgVal, linesAddedGlobalAvgVal };
		contentMap.put(linesAddedItem.getId(), linesAddedChildren);

		// lines removed
		MetricsTreeNode linesRemovedTodayVal = new MetricsTreeNode(
				"Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayLinesRemoved),
				"linesremoved-today-val", "rocket.png");
		String lremovedboltIcon = summary.currentDayLinesRemoved > summary.averageLinesRemoved ? "bolt.png"
				: "bolt-grey.png";
		MetricsTreeNode linesRemovedAvgVal = new MetricsTreeNode(
				"Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(summary.averageLinesRemoved),
				"linesremoved-avg-val", lremovedboltIcon);
		MetricsTreeNode linesRemovedGlobalAvgVal = new MetricsTreeNode(
				"Global average (" + dayStr + "): "
						+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageLinesRemoved),
				"linesremoved-global-val", "global-grey.png");
		MetricsTreeNode[] linesRemovedChildren = { linesRemovedTodayVal, linesRemovedAvgVal, linesRemovedGlobalAvgVal };
		contentMap.put(linesRemovedItem.getId(), linesRemovedChildren);

		// keystrokes
		MetricsTreeNode keystrokesTodayVal = new MetricsTreeNode(
				"Today: " + SoftwareCoUtils.humanizeLongNumbers(summary.currentDayKeystrokes), "keystrokes-today-val",
				"rocket.png");
		String keysrokeboltIcon = summary.currentDayKeystrokes > summary.averageDailyKeystrokes ? "bolt.png"
				: "bolt-grey.png";
		MetricsTreeNode keystrokesAvgVal = new MetricsTreeNode(
				"Your average (" + dayStr + "): " + SoftwareCoUtils.humanizeLongNumbers(summary.averageDailyKeystrokes),
				"keystrokes-avg-val", keysrokeboltIcon);
		MetricsTreeNode keystrokesGlobalAvgVal = new MetricsTreeNode(
				"Global average (" + dayStr + "): "
						+ SoftwareCoUtils.humanizeLongNumbers(summary.globalAverageDailyKeystrokes),
				"keystrokes-global-val", "global-grey.png");
		MetricsTreeNode[] keystrokesChildren = { keystrokesTodayVal, keystrokesAvgVal, keystrokesGlobalAvgVal };
		contentMap.put(keystrokesItem.getId(), keystrokesChildren);

		// top kpm files
		addTopFileNodes(topKpmFilesItem, "kpm");

		// top keystrokes files
		addTopFileNodes(topKeystrokesFilesItem, "keystrokes");

		// top code time files
		addTopFileNodes(topCodeTimeFilesItem, "codetime");

		// open changes
		addStatChangeNodes(openChangesItem, "uncommitted", rootNodes);

		// committed today
		addStatChangeNodes(committedTodayItem, "committed", rootNodes);

		if (initialExpandedElements == null) {
			initialExpandedElements = new MetricsTreeNode[2];
			initialExpandedElements[0] = editorTimeItem;
			initialExpandedElements[1] = codeTimeItem;
		}
	}

	private void addStatChangeNodes(MetricsTreeNode parent, String type, List<MetricsTreeNode> rootNodes) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<MetricsTreeNode> projectList = new ArrayList<>();
		for (IProject project : projects) {
			if (project.getLocation() != null) {

				String pathStr = project.getLocation().toString();
				String id = project.getName().replaceAll("\\s+", "") + "-" + type;
				MetricsTreeNode projectNode = new MetricsTreeNode(project.getName(), id);
				projectNode.setData(pathStr);

				rootNodes.add(projectNode);

				CommitChangeStats stats = null;
				if (type.equals("uncommitted")) {
					stats = GitUtil.getUncommitedChanges(pathStr);
				} else if (type.equals("committed")) {
					stats = GitUtil.getTodaysCommits(pathStr, null);
				}
				if (stats != null) {
					MetricsTreeNode[] projectMetricNodes = getChangeStatNodes(stats);
					contentMap.put(projectNode.getId(), projectMetricNodes);
					projectList.add(projectNode);
				}
			}
		}

		MetricsTreeNode[] children = new MetricsTreeNode[projectList.size()];
		children = projectList.toArray(children);
		contentMap.put(parent.getId(), children);
	}

	private void addTopFileNodes(MetricsTreeNode parent, String sortBy) {
		List<Map.Entry<String, FileChangeInfo>> entryList = null;
		if (sortBy.equals("kpm")) {
			entryList = sortByKpm();
		} else if (sortBy.equals("keystrokes")) {
			entryList = sortByKeystrokes();
		} else if (sortBy.equals("codetime")) {
			entryList = sortByFileSeconds();
		}

		String id = parent.getId() + "_" + sortBy;

		if (entryList != null && entryList.size() > 0) {
			// get the last one
			Map.Entry<String, FileChangeInfo> fileChangeInfoEntry = entryList.get(entryList.size() - 1);
			String name = fileChangeInfoEntry.getValue().name;

			if (name == null || name.length() == 0) {
				Path path = Paths.get(fileChangeInfoEntry.getKey());
				if (path != null) {
					Path fileName = path.getFileName();
					name = fileName.toString();
				}
			}
			String val = "";
			if (sortBy.equals("kpm")) {
				val = SoftwareCoUtils.humanizeLongNumbers(fileChangeInfoEntry.getValue().kpm);
			} else if (sortBy.equals("keystrokes")) {
				val = SoftwareCoUtils.humanizeLongNumbers(fileChangeInfoEntry.getValue().keystrokes);
			} else if (sortBy.equals("codetime")) {
				val = SoftwareCoUtils.humanizeMinutes(fileChangeInfoEntry.getValue().duration_seconds / 60);
			}
			String label = name + " | " + val;
			MetricsTreeNode childNode = new MetricsTreeNode(label, id, "files.png");
			childNode.setData(fileChangeInfoEntry.getValue().fsPath);
			MetricsTreeNode[] childNodes = { childNode };
			contentMap.put(parent.getId(), childNodes);
		} else {
			MetricsTreeNode childNode = new MetricsTreeNode("top files will appear here", id, "files.png");
			MetricsTreeNode[] childNodes = { childNode };
			contentMap.put(parent.getId(), childNodes);
		}
	}

	private MetricsTreeNode[] getChangeStatNodes(CommitChangeStats stats) {
		String parentId = stats.committed ? "committed" : "uncommitted";
		List<MetricsTreeNode> list = new ArrayList<>();
		String insertions = "insertion(s): " + SoftwareCoUtils.humanizeLongNumbers(stats.insertions);
		MetricsTreeNode insertionsNode = new MetricsTreeNode(insertions, "insertions-" + parentId, "insertion.png");
		list.add(insertionsNode);
		String deletions = "deletion(s): " + SoftwareCoUtils.humanizeLongNumbers(stats.deletions);
		MetricsTreeNode deletionsNode = new MetricsTreeNode(deletions, "deletions-" + parentId, "deletion.png");
		list.add(deletionsNode);
		if (stats.committed) {
			String commits = "commit(s): " + SoftwareCoUtils.humanizeLongNumbers(stats.commitCount);
			MetricsTreeNode commitsNode = new MetricsTreeNode(commits, "commits-" + parentId, "commit.png");
			list.add(commitsNode);
			String filesChanged = "files changed: " + SoftwareCoUtils.humanizeLongNumbers(stats.fileCount);
			MetricsTreeNode filesChangedNode = new MetricsTreeNode(filesChanged, "fileschanged-" + parentId,
					"files.png");
			list.add(filesChangedNode);
		}

		MetricsTreeNode[] arr = new MetricsTreeNode[list.size()];
		arr = list.toArray(arr);

		return arr;
	}

	@Override
	public MetricsTreeNode[] getChildren(Object node) {
		String id = ((MetricsTreeNode) node).getId();
		MetricsTreeNode[] nodes = contentMap.get(id);
		return nodes;
	}

	@Override
	public Object[] getElements(Object key) {
		Object[] elements = contentMap.get(key);
		return elements;
	}

	@Override
	public Object getParent(Object node) {
		String id = ((MetricsTreeNode) node).getId();
		String parentId = getParentIdOfElement(id);
		return getElement(parentId);
	}

	@Override
	public boolean hasChildren(Object node) {
		String id = ((MetricsTreeNode) node).getId();
		boolean flag = contentMap.containsKey(id);
		return flag;
	}

	private MetricsTreeNode getElement(String id) {
		for (String key : contentMap.keySet()) {
			MetricsTreeNode[] values = contentMap.get(key);
			for (MetricsTreeNode node : values) {
				if (node.getId().equals(id)) {
					return node;
				}
			}
		}
		return null;
	}

	private String getParentIdOfElement(String childId) {
		for (String key : contentMap.keySet()) {
			MetricsTreeNode[] values = contentMap.get(key);
			for (MetricsTreeNode node : values) {
				if (node.getId().equals(childId)) {
					return key;
				}
			}
		}
		return null;
	}

	private List<Map.Entry<String, FileChangeInfo>> sortByKpm() {
		List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(
				fileChangeInfoMap.entrySet());
		// natural ASC order
		Collections.sort(entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
			@Override
			public int compare(Map.Entry<String, FileChangeInfo> entryA, Map.Entry<String, FileChangeInfo> entryB) {

				Long a = entryA.getValue().kpm;
				Long b = entryB.getValue().kpm;
				return a.compareTo(b);
			}
		});
		return entryList;
	}

	private List<Map.Entry<String, FileChangeInfo>> sortByKeystrokes() {
		List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(
				fileChangeInfoMap.entrySet());
		// natural ASC order
		Collections.sort(entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
			@Override
			public int compare(Map.Entry<String, FileChangeInfo> entryA, Map.Entry<String, FileChangeInfo> entryB) {

				Long a = entryA.getValue().keystrokes;
				Long b = entryB.getValue().keystrokes;
				return a.compareTo(b);
			}
		});
		return entryList;
	}

	private List<Map.Entry<String, FileChangeInfo>> sortByFileSeconds() {
		List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(
				fileChangeInfoMap.entrySet());
		// natural ASC order
		Collections.sort(entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
			@Override
			public int compare(Map.Entry<String, FileChangeInfo> entryA, Map.Entry<String, FileChangeInfo> entryB) {
				Long a = entryA.getValue().duration_seconds;
				Long b = entryB.getValue().duration_seconds;
				return a.compareTo(b);
			}
		});
		return entryList;
	}

}
