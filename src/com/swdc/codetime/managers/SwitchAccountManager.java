package com.swdc.codetime.managers;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import com.swdc.codetime.util.SoftwareCoSessionManager;

public class SwitchAccountManager {

	public static void initiateSwitchAccountFlow() {

		Object[] choices = new Object[] { "Google", "GitHub", "Email" };

		ListDialog ld = new ListDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		ld.setAddCancelButton(true);
		ld.setHeightInChars(0);
		ld.setWidthInChars(0);
		ld.setContentProvider(new ArrayContentProvider());
		ld.setLabelProvider(new LabelProvider());
		ld.setInput(choices);
		ld.setInitialSelections(choices);
		ld.setMessage("Switch to a different account?");
		ld.setTitle("Switch account");

		if (ld.open() != Window.OK) {
			return;
		}

		String selection = ld.getResult()[0].toString().toLowerCase();
		SoftwareCoSessionManager.launchLogin(selection, true);
	}

	public static void initiateSignupFlow() {

		Object[] choices = new Object[] { "Google", "GitHub", "Email" };

		ListDialog ld = new ListDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		ld.setAddCancelButton(true);
		ld.setHeightInChars(0);
		ld.setWidthInChars(0);
		ld.setContentProvider(new ArrayContentProvider());
		ld.setLabelProvider(new LabelProvider());
		ld.setInput(choices);
		ld.setInitialSelections(choices);
		ld.setMessage("Sign up using...");
		ld.setTitle("Sign up");

		if (ld.open() != Window.OK) {
			return;
		}

		String selection = ld.getResult()[0].toString().toLowerCase();
		SoftwareCoSessionManager.launchLogin(selection, true);
	}
}
