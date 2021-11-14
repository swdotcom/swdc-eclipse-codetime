package com.swdc.codetime.managers;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import com.swdc.codetime.util.SoftwareCoSessionManager;

import swdc.java.ops.manager.UtilManager;

public class AuthPromptManager {

	public static void initiateSwitchAccountFlow() {
		initiateAuthFlow("Switch account", "Switch to a different account?", false);
	}

	public static void initiateSignupFlow() {
		initiateAuthFlow("Sign up", "Sign up using...", true);
	}

	public static void initiateLoginFlow() {
		initiateAuthFlow("Log in", "Log in using...", false);
	}

	private static void initiateAuthFlow(String title, String message, boolean isSignup) {
		Object[] choices = new Object[] { "Google", "GitHub", "Email" };
		
		Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", AuthPromptManager.class.getClassLoader());
		String input = (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                icon,
                choices, // Array of choices
                choices[0]); // Initial choice
        if (StringUtils.isNotBlank(input)) {
        	SoftwareCoSessionManager.launchLogin(input, isSignup);
        }
	}
}
