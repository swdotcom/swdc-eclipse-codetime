package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class CodeTimeDashboardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchCodeTimeMetricsDashboard(UIInteractionType.keyboard);

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
