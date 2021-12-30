package com.swdc.codetime.webview;

import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.AuthPromptManager;
import com.swdc.codetime.managers.FlowManager;
import com.swdc.codetime.managers.StatusBarManager;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.IntegrationConnection;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class CodeTimeView extends ViewPart implements ISelectionListener {

	public static final Logger LOG = Logger.getLogger("CodeTimeView");

	private static Browser browser = null;
	private static Composite composite = null;
	public static boolean reloadData = true;

	public CodeTimeView() {
		super();
	}

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}
	
	public static void refreshView() {
		refreshView(false);
	}

	public static void refreshView(boolean reload) {
		reloadData = reload;
		if (browser != null) {
			browser.getDisplay();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						String html = buildHtml();
						browser.setText(html);
						composite.layout();
					} catch (Exception e) {
						System.err.println(e);
					}
				}
	
			});
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	}

	@Override
	public void createPartControl(Composite parent) {
		if (!CodeTimeActivator.initializedOps()) {
			CodeTimeActivator.initializeConfig();
		}
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		browser = new Browser(composite, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setText(buildHtml());
		
		// create the protocol function js will use to send messages to java
		new CustomFunction(browser, "postMessage");
	}

	static class CustomFunction extends BrowserFunction {
		CustomFunction(Browser browser, String name) {
			super(browser, name);
		}

		private WebviewMessage getWebviewMessage(Object[] arguments) {
			WebviewMessage message = new WebviewMessage();
			if (arguments.length == 1) {
				Type type = new TypeToken<WebviewMessage>() {
				}.getType();
				message = UtilManager.gson.fromJson(arguments[0].toString(), type);
			} else {
				message.cmd = arguments[0].toString();
				if (arguments[1] instanceof Integer || arguments[1] instanceof Long) {
					message.id = Long.valueOf(arguments[1].toString());
				} else {
					message.action = arguments[1].toString();
				}
			}
			return message;
		}

		@Override
		public Object function(Object[] arguments) {
			// [{"cmd":"login","payload":{}}]
			// [{"cmd":"registerAccount","payload":{}}]
			try {
				final WebviewMessage msg = getWebviewMessage(arguments);
				switch (msg.cmd) {
				case "showOrgDashboard":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/devops_performance?organization_slug="
								+ msg.action);
					});
					break;
				case "switchAccount":
					SwingUtilities.invokeLater(() -> {
						AuthPromptManager.initiateSwitchAccountFlow();
					});
					break;
				case "displayReadme":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl("https://github.com/swdotcom/swdc-eclipse-codetime");
					});
					break;
				case "viewProjectReports":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl(ConfigManager.app_url + "/reports");
					});
					break;
				case "submitAnIssue":
					SwingUtilities.invokeLater(() -> {
						UtilManager.submitIntellijIssue();
					});
					break;
				case "toggleStatusBar":
					SwingUtilities.invokeLater(() -> {
						StatusBarManager.toggleStatusBarText(UIInteractionType.click);
					});
					break;
				case "viewDashboard":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/code_time?view=summary");
					});
					break;
				case "enableFlowMode":
					SwingUtilities.invokeLater(() -> {
						FlowManager.enterFlowMode(false);
					});
					break;
				case "exitFlowMode":
					SwingUtilities.invokeLater(() -> {
						FlowManager.exitFlowMode();
					});
					break;
				case "manageSlackConnection":
					SwingUtilities.invokeLater(() -> {
						SlackManager.manageSlackConnections();
					});
					break;
				case "connectSlack":
					SwingUtilities.invokeLater(() -> {
						SlackManager.connectSlackWorkspace(() -> {
							CodeTimeView.refreshView();
						});
					});
					break;
				case "disconnectSlackWorkspace":
					SwingUtilities.invokeLater(() -> {
						IntegrationConnection integration = SlackManager.getSlackWorkspaceById(msg.id);
						SlackManager.disconnectSlackAuth(integration, () -> {
							CodeTimeView.refreshView();
						});
					});
					break;
				case "registerAccount":
					SwingUtilities.invokeLater(() -> {
						AuthPromptManager.initiateSignupFlow();
					});
					break;
				case "login":
					SwingUtilities.invokeLater(() -> {
						AuthPromptManager.initiateLoginFlow();
					});
					break;
				case "createOrg":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl(ConfigManager.create_org_url);
					});
					break;
				case "skipSlackConnect":
					SwingUtilities.invokeLater(() -> {
						FileUtilManager.setBooleanItem("eclipse_CtskipSlackConnect", true);
						CodeTimeView.refreshView();
					});
					break;
				case "refreshCodeTimeView":
					SwingUtilities.invokeLater(() -> {
						CodeTimeView.refreshView();
					});
					break;
				case "configureSettings":
					SwingUtilities.invokeLater(() -> {
						UtilManager.launchUrl(ConfigManager.app_url + "/preferences");
					});
					break;
				}
			} catch (Exception e) {

			}
			return null;
		}
	}

	@Override
	public void setFocus() {
	}

	protected static class CodeTimeContentViewer extends ContentViewer {

		@Override
		public Control getControl() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ISelection getSelection() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void refresh() {
			// TODO Auto-generated method stub

		}

		@Override
		public void setSelection(ISelection selection, boolean reveal) {
			// TODO Auto-generated method stub

		}
	}

	private static String buildHtml() {
		JsonObject obj = new JsonObject();
		obj.addProperty("showing_statusbar", true);
		obj.addProperty("skip_slack_connect", FileUtilManager.getBooleanItem("eclipse_CtskipSlackConnect"));
		String qStr = UtilManager.buildQueryString(obj, true);
		String api = "/plugin/sidebar" + qStr;
		ClientResponse resp = OpsHttpClient.appGet(api);
		if (resp.isOk()) {
			return CssUtil.updateBodyCss(resp.getJsonStr());
		}

		return LoadError.get404Html();
	}

}
