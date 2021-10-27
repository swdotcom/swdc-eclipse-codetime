/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;

import com.google.gson.JsonObject;
import com.swdc.codetime.managers.AuthPromptManager;
import com.swdc.codetime.managers.EclipseProjectUtil;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Project;
import swdc.java.ops.snowplow.entities.UIElementEntity;
import swdc.java.ops.snowplow.events.UIInteractionType;

/**
 * 
 * Manages the plugin to software.com session
 *
 */
public class SoftwareCoSessionManager {

	public static final Logger LOG = Logger.getLogger("SoftwareCoSessionManager");

	private static SoftwareCoSessionManager instance = null;

	public static SoftwareCoSessionManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoSessionManager();
		}
		return instance;
	}

	public synchronized static boolean isServerOnline() {
		ClientResponse resp = OpsHttpClient.softwareGet("/ping", null);
		return resp.isOk();
	}

	public static void fetchCodeTimeMetricsDashboard() {
		ClientResponse resp = OpsHttpClient.appGet("/settings/dashboard");

		// write the dashboard content to the dashboard file
		String html = resp.isOk() ? resp.getJsonObj().get("html").getAsString() : "<html></html>";
		FileUtilManager.saveFileContent(FileUtilManager.getCodeTimeDashboardHtmlFile(), html);
	}

	public static void launchSoftwareTopForty() {
		String url = "https://api.software.com/music/top40";
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the software top 40: " + url);
			SWCoreLog.logException(e);
		}
	}

	public static void launchFile(String fsPath, boolean isHtml) {
		Project p = EclipseProjectUtil.getInstance().getFirstActiveProject();
		if (p == null) {
			return;
		}

		File f = new File(fsPath);
		if (f.exists() && f.isFile()) {
			URI uri = f.toURI();
			
			if (isHtml) {
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
				try {
					IWebBrowser browser = support.createBrowser("html_dashboard");
					browser.openURL(uri.toURL());
					return;
				} catch (Exception e3) {
					SWCoreLog
					.logErrorMessage("Code Time: unable to launch editor to view code time metrics, error: "
							+ e3.getMessage());
				}
			}

			String genericEditor = "org.eclipse.ui.genericeditor.GenericEditor";
			String defaultEditor = "org.eclipse.ui.DefaultTextEditor";
			
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			try {
				IDE.openEditor(page, uri, defaultEditor, true);
			} catch (Exception e) {
				try {
					IDE.openEditor(page, uri, genericEditor, true);
				} catch (Exception e1) {
					try {
						IDE.openEditor(page, uri, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
					} catch (Exception e2) {
						SWCoreLog
								.logErrorMessage("Code Time: unable to launch editor to view code time metrics, error: "
										+ e.getMessage());
					}
				}
			}
		}
	}

	public static void launchCodeTimeMetricsDashboard(UIInteractionType type) {
		
		// fetch the dashboard content and write it before displaying it
		fetchCodeTimeMetricsDashboard();

		String codeTimeFile = FileUtilManager.getCodeTimeDashboardHtmlFile();

		launchFile(codeTimeFile, true);
		
		UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = type.equals(UIInteractionType.click) ? "ct_summary_btn" : "ct_summary_cmd";
        elementEntity.element_location = type.equals(UIInteractionType.click) ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = type.equals(UIInteractionType.click) ? "white" : null;
        elementEntity.cta_text = "View summary";
        elementEntity.icon_name = type.equals(UIInteractionType.click) ? "guage" : null;
        EventTrackerManager.getInstance().trackUIInteraction(type, elementEntity);
	}

	public static void launchLogin(String loginType, boolean isSignup) {

		String auth_callback_state = FileUtilManager.getAuthCallbackState(true);

        FileUtilManager.setBooleanItem("switching_account", !isSignup);

        String plugin_uuid = FileUtilManager.getPluginUuid();

        JsonObject obj = new JsonObject();
        obj.addProperty("plugin", "codetime");
        obj.addProperty("plugin_uuid", plugin_uuid);
        obj.addProperty("pluginVersion", SoftwareCoUtils.getVersion());
        obj.addProperty("plugin_id", SoftwareCoUtils.pluginId);
        obj.addProperty("auth_callback_state", auth_callback_state);
        obj.addProperty("redirect", SoftwareCoUtils.app_url);

		String url = "";
		String element_name = "ct_sign_up_email_btn";
        String cta_text = "Sign up with email";
        String icon_name = "envelope";
        String icon_color = "blue";
        if (loginType == null || loginType.equalsIgnoreCase("software") || loginType.equalsIgnoreCase("email")) {
            element_name = "ct_sign_up_email_btn";
            cta_text = "Sign up with email";
            icon_name = "envelope";
            icon_color = "gray";
            if (isSignup) {
            	url = SoftwareCoUtils.app_url + "/email-signup";
            } else {
            	url = SoftwareCoUtils.app_url + "/onboarding";
            }
        } else if (loginType.equalsIgnoreCase("google")) {
            url = SoftwareCoUtils.api_endpoint + "/auth/google";
        } else if (loginType.equalsIgnoreCase("github")) {
            element_name = "ct_sign_up_github_btn";
            cta_text = "Sign up with GitHub";
            icon_name = "github";
            url = SoftwareCoUtils.api_endpoint + "/auth/github";
        }
		
		StringBuffer sb = new StringBuffer();
        Iterator<String> keys = obj.keySet().iterator();
        while(keys.hasNext()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            String key = keys.next();
            String val = obj.get(key).getAsString();
            try {
                val = URLEncoder.encode(val, "UTF-8");
            } catch (Exception e) {
                LOG.info("Unable to url encode value, error: " + e.getMessage());
            }
            sb.append(key).append("=").append(val);
        }
        url += "?" + sb.toString();

		FileUtilManager.setItem("authType", loginType);

		try {
			UtilManager.launchUrl(url);
		} catch (Exception e) {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
			} catch (Exception ex) {
				SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
				SWCoreLog.logException(ex);
			}
		}
		
		UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = element_name;
        elementEntity.element_location = "ct_menu_tree";
        elementEntity.color = icon_color;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = icon_name;
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
	}

	public static void launchWebDashboard(UIInteractionType type) {
		if (StringUtils.isBlank(FileUtilManager.getItem("name"))) {
            SwingUtilities.invokeLater(() -> {
                String msg = "Sign up or log in to see more data visualizations.";

                Object[] options = {"Sign up"};
                int choice = JOptionPane.showOptionDialog(
                        null, msg, "Sign up", JOptionPane.OK_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == 0) {
                    AuthPromptManager.initiateSignupFlow();
                }
            });
            return;
        }
		String url = SoftwareCoUtils.webui_login_url;
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
			
			UIElementEntity elementEntity = new UIElementEntity();
	        elementEntity.element_name = type.equals(UIInteractionType.click) ? "ct_web_metrics_btn" : "ct_web_metrics_cmd";
	        elementEntity.element_location = type.equals(UIInteractionType.click) ? "ct_menu_tree" : "ct_command_palette";
	        elementEntity.color = type.equals(UIInteractionType.click) ? "blue" : null;
	        elementEntity.cta_text = "See advanced metrics";
	        elementEntity.icon_name = type.equals(UIInteractionType.click) ? "paw" : null;
	        EventTrackerManager.getInstance().trackUIInteraction(type, elementEntity);
		} catch (PartInitException | MalformedURLException e) {
			SWCoreLog.logErrorMessage("Failed to launch the url: " + url);
			SWCoreLog.logException(e);
		}
	}

}