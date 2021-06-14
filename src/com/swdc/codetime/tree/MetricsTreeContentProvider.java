package com.swdc.codetime.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.swdc.codetime.managers.FlowManager;
import com.swdc.codetime.managers.ScreenManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.TimeDataManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.SoftwareCoUtils;

import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTimeSummary;
import swdc.java.ops.model.ConfigSettings;
import swdc.java.ops.model.Integration;
import swdc.java.ops.model.MetricLabel;
import swdc.java.ops.model.SessionSummary;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;
import swdc.java.ops.model.SlackUserProfile;

public class MetricsTreeContentProvider implements ITreeContentProvider {

	public static final String ROOT_KEY = "root";

	public static final String SIGN_UP_ID = "signup";
	public static final String LOG_IN_ID = "login";
	public static final String GOOGLE_SIGNUP_ID = "google";
	public static final String GITHUB_SIGNUP_ID = "github";
	public static final String EMAIL_SIGNUP_ID = "email";
	public static final String LOGGED_IN_ID = "logged_in";
	public static final String LEARN_MORE_ID = "learn_more";
	public static final String SEND_FEEDBACK_ID = "send_feedback";
	public static final String SUBMIT_ISSUES_ID = "submit_issues";
	public static final String ADVANCED_METRICS_ID = "advanced_metrics";
	public static final String TOGGLE_METRICS_ID = "toggle_metrics";
	public static final String VIEW_SUMMARY_ID = "view_summary";
	public static final String CODETIME_PARENT_ID = "codetime_parent";
	public static final String CODETIME_TODAY_ID = "codetime_today";
	public static final String ACTIVE_CODETIME_PARENT_ID = "active_codetime_parent";
	public static final String ACTIVE_CODETIME_TODAY_ID = "active_codetime_today";
	public static final String ACTIVE_CODETIME_AVG_TODAY_ID = "active_codetime_avg_today";
	public static final String ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID = "active_codetime_global_avg_today";
	public static final String TODAY_VS_AVG_ID = "today_vs_average";

	public static final String LINES_ADDED_TODAY_ID = "lines_added_today";
	public static final String LINES_ADDED_AVG_TODAY_ID = "lines_added_avg_today";
	public static final String LINES_ADDED_GLOBAL_AVG_TODAY_ID = "lines_added_global_avg_today";

	public static final String LINES_DELETED_TODAY_ID = "lines_deleted_today";
	public static final String LINES_DELETED_AVG_TODAY_ID = "lines_deleted_avg_today";
	public static final String LINES_DELETED_GLOBAL_AVG_TODAY_ID = "lines_deleted_global_avg_today";

	public static final String KEYSTROKES_TODAY_ID = "keystrokes_today";
	public static final String KEYSTROKES_AVG_TODAY_ID = "keystrokes_avg_today";
	public static final String KEYSTROKES_GLOBAL_AVG_TODAY_ID = "keystrokes_global_avg_today";

	public static final String SWITCH_ACCOUNT_ID = "switch_account";

	public static final String ACCOUNT_ID = "account_node";
	public static final String SLACK_WORKSPACES_NODE_ID = "slack_workspaces_node";
    public static final String AUTOMATIONS_NODE_ID = "flow_mode_automations";
    public static final String FLOW_MODE_SETTINGS_ID = "flow_mode_settings";
    public static final String ENABLE_FLOW_MODE_ID = "enable_flow_mode";
    public static final String PAUSE_FLOW_MODE_ID = "pause_flow_mode";
	public static final String SWITCH_OFF_DARK_MODE_ID = "switch_off_dark_mode";
	public static final String SWITCH_ON_DARK_MODE_ID = "switch_on_dark_mode";
	public static final String TOGGLE_DOCK_POSITION_ID = "toggle_dock_position";
	public static final String SWITCH_OFF_DND_ID = "switch_off_dnd";
	public static final String SWITCH_ON_DND_ID = "switch_on_dnd";
	public static final String CONNECT_SLACK_ID = "connect_slack";
	public static final String ADD_WORKSPACE_ID = "add_workspace";
	public static final String SET_PRESENCE_AWAY_ID = "set_presence_away";
	public static final String SET_PRESENCE_ACTIVE_ID = "set_presence_active";
	public static final String SET_SLACK_STATUS_ID = "set_slack_status";
	public static final String ENTER_FULL_SCREEN_MODE_ID = "enter_full_screen_mode";
    public static final String EXIT_FULL_SCREEN_MODE_ID = "exit_full_screen_mode";
	
	public static final String PAUSE_NOTIFICATIONS_SETTING_ID = "pause_notifications_setting";
    public static final String SLACK_AWAY_STATUS_SETTING_ID = "slack_away_status_setting";
    public static final String SLACK_AWAY_STATUS_TEXT_SETTING_ID = "slack_away_status_text_setting";
    public static final String SCREEN_MODE_SETTING_ID = "screen_mode_setting";

	private MetricLabel mLabels = new MetricLabel();
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

		mLabels.updateLabels(codeTimeSummary, sessionSummary);
		
		mNodeList.add(buildAccountButtons());

		mNodeList.add(getEditorDashboardButton());
		mNodeList.add(getWebDashboardButton());
		mNodeList.add(getToggleStatusbarMetricsButton());

		// create the separator
		mNodeList.add(getSeparatorLine());

		// create the flow nodes
		mNodeList.add(getFlowModeNode());
		mNodeList.add(buildFlowModeSettingsNode());
		mNodeList.add(buildAutomationNode());
		mNodeList.add(buildSlackWorkspacesNode());

		// create the separator
		mNodeList.add(getSeparatorLine());

		mNodeList.add(buildTodayVsAverageNode());
		mNodeList.add(getCodeTimeStatsButton(mLabels));
		mNodeList.add(getActiveCodeTimeStatsButton(mLabels));
		mNodeList.add(getLinesAddedStatsButton(mLabels));
		mNodeList.add(getLinesRemovedStatsButton(mLabels));
		mNodeList.add(getKeystrokesStatsButton(mLabels));

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
		return new MetricTreeNode("Learn more", "readme.png", LEARN_MORE_ID);
	}

	public MetricTreeNode getSwitchAccountButton() {
		return new MetricTreeNode("Switch account", "paw.png", SWITCH_ACCOUNT_ID);
	}

	public MetricTreeNode getToggleStatusbarMetricsButton() {
		String buttonLabel = SoftwareCoUtils.showingStatusText() ? "Hide status bar metrics" : "Show status bar metrics";
		return new MetricTreeNode(buttonLabel, "visible.png", TOGGLE_METRICS_ID);
	}

	public MetricTreeNode getSubmitFeedbackButton() {
		return new MetricTreeNode("Submit feedback", "message.png", SEND_FEEDBACK_ID);
	}
	
	public MetricTreeNode getSubmitIssuesButton() {
		return new MetricTreeNode("Submit an issue", "message.png", SUBMIT_ISSUES_ID);
	}

	public List<MetricTreeNode> getSignupButtons() {
		MetricTreeNode signUpItem = new MetricTreeNode("Sign up", "paw.png", SIGN_UP_ID);
		MetricTreeNode logInItem = new MetricTreeNode("Log in", "paw.png", LOG_IN_ID);
		return new ArrayList<MetricTreeNode>(Arrays.asList(signUpItem, logInItem));
	}

	public MetricTreeNode buildSlackWorkspacesNode() {
		MetricTreeNode workspacesFolderNode = new MetricTreeNode("Slack workspaces", null, SLACK_WORKSPACES_NODE_ID);
		List<Integration> workspaces = SlackManager.getSlackWorkspaces();

		List<MetricTreeNode> children = new ArrayList<MetricTreeNode>();
		workspaces.forEach(workspace -> {
			MetricTreeNode workspaceNode = new MetricTreeNode(workspace.team_domain, "slack.png", workspace.authId);
			children.add(workspaceNode);
		});
		children.add(getAddSlackWorkspaceNode());
		MetricTreeNode[] childnodes = Arrays.copyOf(children.toArray(), children.size(), MetricTreeNode[].class);
		contentMap.put(SLACK_WORKSPACES_NODE_ID, childnodes);

		return workspacesFolderNode;
	}
	
	public MetricTreeNode buildAccountButtons() {
		MetricTreeNode accountFolderNode = new MetricTreeNode("Account", null, ACCOUNT_ID);

		List<MetricTreeNode> children = new ArrayList<MetricTreeNode>();
		
		String name = FileUtilManager.getItem("name");

		if (StringUtils.isEmpty(name)) {
			// add the sign up buttons
			children.addAll(getSignupButtons());
		} else {
			// add the logged in button and switch account button
			children.add(getLoggedInButton());
			children.add(getSwitchAccountButton());
		}

		children.add(getLearnMoreButton());
		children.add(getSubmitIssuesButton());


		MetricTreeNode[] childnodes = Arrays.copyOf(children.toArray(), children.size(), MetricTreeNode[].class);
		contentMap.put(ACCOUNT_ID, childnodes);

		return accountFolderNode;
	}

	public static MetricTreeNode getAddSlackWorkspaceNode() {
		return new MetricTreeNode("Add workspace", "add.png", ADD_WORKSPACE_ID);
	}
	
	public MetricTreeNode buildFlowModeSettingsNode() {
		MetricTreeNode settingsFolder = new MetricTreeNode("Settings", null, FLOW_MODE_SETTINGS_ID);
		
		List<MetricTreeNode> children = new ArrayList<MetricTreeNode>();
		
		ConfigSettings settings = ConfigManager.getConfigSettings();
        String notificationsVal = settings.pauseSlackNotifications ? "on" : "off";
        // Pause notifications
        MetricTreeNode pauseNode = new MetricTreeNode("Pause notifications (" + notificationsVal + ")", "profile.png", PAUSE_NOTIFICATIONS_SETTING_ID);
        children.add(pauseNode);
        
        String awayStatusVal = settings.slackAwayStatus ? "on" : "off";
        // Slack away status
        MetricTreeNode slackAwayNode = new MetricTreeNode("Slack away status (" + awayStatusVal + ")", "profile.png", SLACK_AWAY_STATUS_SETTING_ID);
        children.add(slackAwayNode);
        
        String awayStatusTextVal = (StringUtils.isNotBlank(settings.slackAwayStatusText)) ? " (" + settings.slackAwayStatusText + ")" : "";
        // Slackaway status text
        MetricTreeNode slackAwayTextNode = new MetricTreeNode("Slack away text" + awayStatusTextVal, "profile.png", SLACK_AWAY_STATUS_TEXT_SETTING_ID);
        children.add(slackAwayTextNode);
        
        // Screen mode
        MetricTreeNode screenModeNode = new MetricTreeNode("Screen mode (" + settings.screenMode + ")", "profile.png", SCREEN_MODE_SETTING_ID);
        children.add(screenModeNode);
        
        MetricTreeNode[] childnodes = Arrays.copyOf(children.toArray(), children.size(), MetricTreeNode[].class);
		contentMap.put(FLOW_MODE_SETTINGS_ID, childnodes);
        
		return settingsFolder;
	}

	public MetricTreeNode getLoggedInButton() {
		String authType = FileUtilManager.getItem("authType");
		String name = FileUtilManager.getItem("name");
		String iconName = "email.png";
		if ("google".equals(authType)) {
			iconName = "google.png";
		} else if ("github".equals(authType)) {
			iconName = "github.png";
		}
		return new MetricTreeNode(name, iconName, LOGGED_IN_ID);
	}

	//////////////////////////////////////
	// FLOW TREE VIEW BUTTONS
	//////////////////////////////////////
	public MetricTreeNode getFlowModeNode() {
        String label = "Enable flow mode";
        String icon = "dot-outlined.png";
        String id = ENABLE_FLOW_MODE_ID;
        if (FlowManager.isInFlowMode()) {
            label = "Pause Flow Mode";
            icon = "dot.png";
            id = PAUSE_FLOW_MODE_ID;
        }
        return new MetricTreeNode(label, icon, id);
    }
	
	public MetricTreeNode buildAutomationNode() {
		MetricTreeNode automationFolder = new MetricTreeNode("Automations", null, AUTOMATIONS_NODE_ID);
		
		List<MetricTreeNode> children = new ArrayList<>();
		
		// full screen toggle node
		children.add(getToggleFullScreenNode());
		
		// add the status update button
		children.add(getSetSlackStatusNode());

		SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();

		// snooze node
		if (slackDndInfo.snooze_enabled) {
			children.add(getUnPausenotificationsNode(slackDndInfo));
		} else {
			children.add(getPauseNotificationsNode());
		}
		// presence toggle
		SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();
		if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
			children.add(getSetAwayPresenceNode());
		} else {
			children.add(getSetActivePresenceNode());
		}

		if (UtilManager.isMac()) {
			if (AppleScriptManager.isDarkMode()) {
				children.add(getSwitchOffDarkModeNode());
			} else {
				children.add(getSwitchOnDarkModeNode());
			}

			children.add(new MetricTreeNode("Toggle dock position", "position.png", TOGGLE_DOCK_POSITION_ID));
		}
		
		MetricTreeNode[] childnodes = Arrays.copyOf(children.toArray(), children.size(), MetricTreeNode[].class);
		contentMap.put(AUTOMATIONS_NODE_ID, childnodes);

		return automationFolder;
	}
	
	public static MetricTreeNode getToggleFullScreenNode() {
        String label = "Enter full screen";
        String icon = "expand.png";
        String id = ENTER_FULL_SCREEN_MODE_ID;
        if (ScreenManager.isFullScreen()) {
            label = "Exit full screen";
            icon = "compress.png";
            id = EXIT_FULL_SCREEN_MODE_ID;
        }
        return new MetricTreeNode(label, icon, id);
    }
	
	public static MetricTreeNode getSetSlackStatusNode() {
        SlackUserProfile userProfile = SlackManager.getSlackStatus();
        String status = (userProfile != null && StringUtils.isNotBlank(userProfile.status_text)) ? " (" + userProfile.status_text + ")" : "";
        return new MetricTreeNode("Update profile status" + status, "profile.png", SET_SLACK_STATUS_ID);
    }

	public static MetricTreeNode getSwitchOffDarkModeNode() {
		return new MetricTreeNode("Turn off dark mode", "adjust.png", SWITCH_OFF_DARK_MODE_ID);
	}

	public static MetricTreeNode getSwitchOnDarkModeNode() {
		return new MetricTreeNode("Turn on dark mode", "adjust.png", SWITCH_ON_DARK_MODE_ID);
	}

	public static MetricTreeNode getPauseNotificationsNode() {
		return new MetricTreeNode("Pause notifications", "notifications-off.png", SWITCH_OFF_DND_ID);
	}

	public static MetricTreeNode getUnPausenotificationsNode(SlackDndInfo slackDndInfo) {
		String endTimeOfDay = UtilManager.getTimeOfDay(UtilManager.getJavaDateFromSeconds(slackDndInfo.snooze_endtime));
		return new MetricTreeNode("Turn on notifications (" + endTimeOfDay + ")", "notifications-on.png",
				SWITCH_ON_DND_ID);
	}

	public static MetricTreeNode getSetAwayPresenceNode() {
		return new MetricTreeNode("Set presence to away", "presence.png", SET_PRESENCE_AWAY_ID);
	}

	public static MetricTreeNode getSetActivePresenceNode() {
		return new MetricTreeNode("Set presence to active", "presence.png", SET_PRESENCE_ACTIVE_ID);
	}

	//////////////////////////////////////
	// KPM TREE VIEW BUTTONS
	//////////////////////////////////////
	public static MetricTreeNode buildTodayVsAverageNode() {
        String refClass = FileUtilManager.getItem("reference-class", "user");
        String labelExt = refClass.equals("user") ? " your daily average" : " the global daily average";
        return new MetricTreeNode("Today vs." + labelExt, "today.png", TODAY_VS_AVG_ID);
    }
	
	public MetricTreeNode getCodeTimeStatsButton(MetricLabel labels) {
		return new MetricTreeNode(labels.codeTime, labels.codeTimeIcon, CODETIME_TODAY_ID);
	}

	public MetricTreeNode getActiveCodeTimeStatsButton(MetricLabel labels) {
		return new MetricTreeNode(labels.activeCodeTime, labels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_TODAY_ID);
	}

	public MetricTreeNode getLinesAddedStatsButton(MetricLabel labels) {
		return new MetricTreeNode(labels.linesAdded, labels.linesAddedAvgIcon, LINES_ADDED_TODAY_ID);
	}

	public MetricTreeNode getLinesRemovedStatsButton(MetricLabel labels) {
		return new MetricTreeNode(labels.linesRemoved, labels.linesRemovedAvgIcon, LINES_DELETED_TODAY_ID);
	}

	public MetricTreeNode getKeystrokesStatsButton(MetricLabel labels) {
		return new MetricTreeNode(labels.keystrokes, labels.keystrokesAvgIcon, KEYSTROKES_TODAY_ID);
	}

	public MetricTreeNode getEditorDashboardButton() {
		return new MetricTreeNode("Dashboard", "dashboard.png", VIEW_SUMMARY_ID);
	}

	public MetricTreeNode getWebDashboardButton() {
		return new MetricTreeNode("More data at Software.com", "paw.png", ADVANCED_METRICS_ID);
	}
	
	public static void handleRightClickEvent(MetricTreeNode node, MouseEvent e) {
        String parentId = node.getParent() != null ? ((MetricTreeNode)node.getParent()).getId() : null;
        // handle the slack workspace selection
        if (parentId != null
                && parentId.equals(SLACK_WORKSPACES_NODE_ID)
                && !node.getId().equals(ADD_WORKSPACE_ID)) {
            JPopupMenu popupMenu = buildWorkspaceMenu(node.getId());
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
	
	public static JPopupMenu buildWorkspaceMenu(String authId) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem removeWorkspaceItem = new JMenuItem("Remove workspace");
        removeWorkspaceItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SlackManager.disconnectSlackAuth(SlackManager.getWorkspaceByAuthId(authId), () -> {WallClockManager.refreshTree();});
			}
		});
        menu.add(removeWorkspaceItem);
        return menu;
    }

}
