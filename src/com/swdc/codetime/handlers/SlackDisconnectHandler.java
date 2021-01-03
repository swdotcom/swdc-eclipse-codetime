package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.managers.WallClockManager;

import swdc.java.ops.manager.SlackManager;

public class SlackDisconnectHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SlackManager.disconnectSlackWorkspace(() -> {
			WallClockManager.refreshTree();
		});
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return SlackManager.hasSlackWorkspaces();
	}

}
