package com.swdc.codetime.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.TimeDataManager;
import com.swdc.codetime.models.CodeTimeSummary;
import com.swdc.codetime.models.SessionSummary;

import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Integration;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;

public class MetricsTreeContentProvider implements ITreeContentProvider {

	public static final String ROOT_KEY = "root";

	private MetricLabels mLabels = new MetricLabels();
	private Map<String, MetricTreeNode[]> contentMap = new HashMap<String, MetricTreeNode[]>();
	private MetricTreeNode[] initialExpandedElements = null;

	public MetricsTreeContentProvider() {
		this.buildTreeNodes();
	}

	public MetricTreeNode[] getInitialExpandedElements() {
		return initialExpandedElements;
	}

	public void buildTreeNodes() {

		List<MetricTreeNode> mNodeList = new ArrayList<>();
		SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
		CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
		String name = FileUtilManager.getItem("name");
		
		mLabels.updateLabels(codeTimeSummary, sessionSummary);
		
		if (StringUtils.isEmpty(name)) {
			// add the sign up buttons
			mNodeList.addAll(getSignupButtons());
		} else {
			// add the logged in button and switch account button
			mNodeList.add(getLoggedInButton());
			mNodeList.add(getSwitchAccountButton());
		}
		
		// create the menu nodes
		// to create a parent tree do this...
		// MetricsTreeNode[] codeTimeChildren = { switchAccountItem, toggleStatusTextItem, learnMoreItem, submitFeedbackItem };
		// contentMap.put(signedUpAsItem.getId(), codeTimeChildren);
		
		mNodeList.add(getLearnMoreButton());
		mNodeList.add(getSubmitFeedbackButton());
		mNodeList.add(getToggleStatusbarMetricsButton());
		
		mNodeList.add(buildSlackWorkspacesNode());
		
		// create the separator
		mNodeList.add(getSeparatorLine());
		
		// create the flow nodes
		mNodeList.addAll(buildTreeFlowNodes());

		// create the separator
		mNodeList.add(getSeparatorLine());

		mNodeList.add(getCodeTimeStatsButton(mLabels));
		mNodeList.add(getActiveCodeTimeStatsButton(mLabels));
		mNodeList.add(getLinesAddedStatsButton(mLabels));
		mNodeList.add(getLinesRemovedStatsButton(mLabels));
		mNodeList.add(getKeystrokesStatsButton(mLabels));
		mNodeList.add(getEditorDashboardButton());
		mNodeList.add(getWebDashboardButton());

		MetricTreeNode[] roots = Arrays.copyOf(mNodeList.toArray(), mNodeList.size(), MetricTreeNode[].class);
		contentMap.put(ROOT_KEY, roots);
	}


	@Override
	public MetricTreeNode[] getChildren(Object node) {
		String id = ((MetricTreeNode) node).getId();
		MetricTreeNode[] nodes = contentMap.get(id);
		return nodes;
	}

	@Override
	public Object[] getElements(Object key) {
		Object[] elements = contentMap.get(key);
		return elements;
	}

	@Override
	public Object getParent(Object node) {
		String id = ((MetricTreeNode) node).getId();
		String parentId = getParentIdOfElement(id);
		return getElement(parentId);
	}

	@Override
	public boolean hasChildren(Object node) {
		String id = ((MetricTreeNode) node).getId();
		boolean flag = contentMap.containsKey(id);
		return flag;
	}

	private MetricTreeNode getElement(String id) {
		for (String key : contentMap.keySet()) {
			MetricTreeNode[] values = contentMap.get(key);
			for (MetricTreeNode node : values) {
				if (node.getId().equals(id)) {
					return node;
				}
			}
		}
		return null;
	}

	private String getParentIdOfElement(String childId) {
		for (String key : contentMap.keySet()) {
			MetricTreeNode[] values = contentMap.get(key);
			for (MetricTreeNode node : values) {
				if (node.getId().equals(childId)) {
					return key;
				}
			}
		}
		return null;
	}
	
	public MetricTreeNode getSeparatorLine() {
		return new MetricTreeNode(true);
	}
	
	
	//////////////////////////////////////
	// MENU TREE VIEW BUTTONS
	//////////////////////////////////////
	public MetricTreeNode getLearnMoreButton() {
		return new MetricTreeNode("Learn more", "readme.png", MetricLabels.LEARN_MORE_ID);
	}
	
	
	public MetricTreeNode getSwitchAccountButton() {
		return new MetricTreeNode("Switch account", "paw.png", MetricLabels.SWITCH_ACCOUNT_ID);
	}
	
	public MetricTreeNode getToggleStatusbarMetricsButton() {
		return new MetricTreeNode("Hide status bar metrics", "visible.png", MetricLabels.TOGGLE_METRICS_ID);
	}
	
	public MetricTreeNode getSubmitFeedbackButton() {
		return new MetricTreeNode("Submit feedback", "message.png", MetricLabels.SEND_FEEDBACK_ID);
	}
	
	public List<MetricTreeNode> getSignupButtons() {
		MetricTreeNode googleLoginItem = new MetricTreeNode("Sign up with Google", "google.png", MetricLabels.GOOGLE_SIGNUP_ID);
		MetricTreeNode githubLoginItem = new MetricTreeNode("Sign up with GitHub", "github.png", MetricLabels.GITHUB_SIGNUP_ID);
		MetricTreeNode emailLoginItem = new MetricTreeNode("Sign up using email", "icons8-envelope-16.png", MetricLabels.EMAIL_SIGNUP_ID);
		return new ArrayList<MetricTreeNode>(Arrays.asList(googleLoginItem, githubLoginItem, emailLoginItem));
	}
	
	public MetricTreeNode buildSlackWorkspacesNode() {
        List<Integration> workspaces = SlackManager.getSlackWorkspaces();
        
    	List<MetricTreeNode> children = new ArrayList<MetricTreeNode>();
        workspaces.forEach(workspace -> {
            children.add(new MetricTreeNode(workspace.team_domain, "icons8-slack-new-16.png", workspace.authId));
        });
        children.add(getAddSlackWorkspaceNode());
        MetricTreeNode[] childnodes = Arrays.copyOf(children.toArray(), children.size(), MetricTreeNode[].class);
        contentMap.put(MetricLabels.SLACK_WORKSPACES_NODE_ID, childnodes);
    
        return new MetricTreeNode("Slack workspaces", null, MetricLabels.SLACK_WORKSPACES_NODE_ID);
    }
	
	public static MetricTreeNode getAddSlackWorkspaceNode() {
        return new MetricTreeNode("Add workspace", "add.png", MetricLabels.ADD_WORKSPACE_ID);
    }
	
	public MetricTreeNode getLoggedInButton() {
		String authType = FileUtilManager.getItem("authType");
        String name = FileUtilManager.getItem("name");
        String iconName = "icons8-envelope-16.png";
        if ("google".equals(authType)) {
            iconName = "google.png";
        } else if ("github".equals(authType)) {
            iconName = "github.png";
        }
		return new MetricTreeNode(name, iconName, MetricLabels.LOGGED_IN_ID);
	}
	
	//////////////////////////////////////
	// FLOW TREE VIEW BUTTONS
	//////////////////////////////////////
	public static List<MetricTreeNode> buildTreeFlowNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        
        if (SlackManager.hasSlackWorkspaces()) {
            SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();
            
            // snooze node
            if (slackDndInfo.snooze_enabled) {
                list.add(getUnPausenotificationsNode(slackDndInfo));
            } else {
                list.add(getPauseNotificationsNode());
            }
            // presence toggle
            SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();
            if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
                list.add(getSetAwayPresenceNode());
            } else {
                list.add(getSetActivePresenceNode());
            }
        } else {
            // show the connect slack node
            list.add(getConnectSlackNode());
        }
        
        if (UtilManager.isMac()) {
            if (AppleScriptManager.isDarkMode()) {
                list.add(getSwitchOffDarkModeNode());
            } else {
                list.add(getSwitchOnDarkModeNode());
            }
            
            list.add(new MetricTreeNode("Toggle dock position", "settings.png", MetricLabels.TOGGLE_DOCK_POSITION_ID));
        }
        
        return list;
    }
	
	public static MetricTreeNode getSwitchOffDarkModeNode() {
        return new MetricTreeNode("Turn off dark mode", "light-mode.png", MetricLabels.SWITCH_OFF_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getSwitchOnDarkModeNode() {
        return new MetricTreeNode("Turn on dark mode", "dark-mode.png", MetricLabels.SWITCH_ON_DARK_MODE_ID);
    }
	
	public static MetricTreeNode getConnectSlackNode() {
        return new MetricTreeNode("Connect to set your status and pause notifications", "icons8-slack-new-16.png", MetricLabels.CONNECT_SLACK_ID);
    }
    
    public static MetricTreeNode getPauseNotificationsNode() {
        return new MetricTreeNode("Pause notifications", "icons8-slack-new-16.png", MetricLabels.SWITCH_OFF_DND_ID);
    }
    
    public static MetricTreeNode getUnPausenotificationsNode(SlackDndInfo slackDndInfo) {
        String endTimeOfDay = UtilManager.getTimeOfDay(UtilManager.getJavaDateFromSeconds(slackDndInfo.snooze_endtime));
        return new MetricTreeNode("Turn on notifications (" + endTimeOfDay + ")", "icons8-slack-new-16.png", MetricLabels.SWITCH_ON_DND_ID);
    }
    
    public static MetricTreeNode getSetAwayPresenceNode() {
        return new MetricTreeNode("Set presence to away", "icons8-slack-new-16.png", MetricLabels.SET_PRESENCE_AWAY_ID);
    }
    
    public static MetricTreeNode getSetActivePresenceNode() {
        return new MetricTreeNode("Set presence to active", "icons8-slack-new-16.png", MetricLabels.SET_PRESENCE_ACTIVE_ID);
    }
	
	
	//////////////////////////////////////
	// KPM TREE VIEW BUTTONS
	//////////////////////////////////////
	public MetricTreeNode getCodeTimeStatsButton(MetricLabels labels) {
		return new MetricTreeNode(labels.codeTime, "rocket.png", MetricLabels.CODETIME_TODAY_ID);
	}
	
	public MetricTreeNode getActiveCodeTimeStatsButton(MetricLabels labels) {
		return new MetricTreeNode(labels.activeCodeTime, labels.activeCodeTimeAvgIcon, MetricLabels.ACTIVE_CODETIME_TODAY_ID);
	}
	
	public MetricTreeNode getLinesAddedStatsButton(MetricLabels labels) {
		return new MetricTreeNode(labels.linesAdded, labels.linesAddedAvgIcon, MetricLabels.LINES_ADDED_TODAY_ID);
	}
	
	public MetricTreeNode getLinesRemovedStatsButton(MetricLabels labels) {
		return new MetricTreeNode(labels.linesRemoved, labels.linesRemovedAvgIcon, MetricLabels.LINES_DELETED_TODAY_ID);
	}
	
	public MetricTreeNode getKeystrokesStatsButton(MetricLabels labels) {
		return new MetricTreeNode(labels.keystrokes, labels.keystrokesAvgIcon, MetricLabels.KEYSTROKES_TODAY_ID);
	}
	
	public MetricTreeNode getEditorDashboardButton() {
		return new MetricTreeNode("Dashboard", "dashboard.png", MetricLabels.VIEW_SUMMARY_ID);
	}
	
	public MetricTreeNode getWebDashboardButton() {
		return new MetricTreeNode("More data at Software.com", "paw.png", MetricLabels.ADVANCED_METRICS_ID);
	}

}
