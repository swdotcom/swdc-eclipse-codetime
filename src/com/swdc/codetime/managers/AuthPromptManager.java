package com.swdc.codetime.managers;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import com.swdc.codetime.util.SoftwareCoSessionManager;

public class AuthPromptManager {

	public static void initiateSwitchAccountFlow() {
		initiateAuthFlow("Switch account", "Switch to a different account?");
	}

	public static void initiateSignupFlow() {
		initiateAuthFlow("Sign up", "Sign up using...");
	}

	public static void initiateLoginFlow() {
		initiateAuthFlow("Log in", "Log in using...");
	}

	private static void initiateAuthFlow(String title, String message) {
		Object[] choices = new Object[] { "Google", "GitHub", "Email" };

		ListDialog ld = new ListDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		ld.setAddCancelButton(false);
		ld.setHeightInChars(0);
		ld.setWidthInChars(0);
		ld.setContentProvider(new ArrayContentProvider());
		ld.setLabelProvider(new LabelProvider());
		ld.setInput(choices);
		ld.setInitialSelections(choices);
		ld.setMessage(message);
		ld.setTitle(title);

		if (ld.open() != Window.OK || ld.getResult() == null || ld.getResult().length == 0) {
			return;
		}

		String selection = ld.getResult()[0].toString().toLowerCase();
		SoftwareCoSessionManager.launchLogin(selection, true);

	}
}
