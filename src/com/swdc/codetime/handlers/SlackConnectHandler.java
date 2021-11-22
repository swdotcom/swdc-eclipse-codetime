package com.swdc.codetime.handlers;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.webview.CodeTimeView;

import swdc.java.ops.manager.SlackManager;

public class SlackConnectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SwingUtilities.invokeLater(() -> {
			SlackManager.connectSlackWorkspace(() -> {
				CodeTimeView.refreshView();
			});
		});
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
}
