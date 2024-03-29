package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.managers.StatusBarManager;

import swdc.java.ops.snowplow.events.UIInteractionType;

public class ToggleStatusBarHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		StatusBarManager.toggleStatusBarText(UIInteractionType.keyboard);

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
