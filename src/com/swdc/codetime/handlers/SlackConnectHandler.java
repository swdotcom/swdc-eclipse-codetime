package com.swdc.codetime.handlers;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.UtilManager;

public class SlackConnectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SwingUtilities.invokeLater(() -> {
			UtilManager.launchUrl(ConfigManager.app_url + "/data_sources/integration_types/slack");
		});
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
}
