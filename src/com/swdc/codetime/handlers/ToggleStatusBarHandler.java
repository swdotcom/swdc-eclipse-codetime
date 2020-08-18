package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class ToggleStatusBarHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoUtils.toggleStatusBarText(UIInteractionType.keyboard);

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
