package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class MetricsDashboardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.keyboard);
		return null;
	}

	@Override
	public boolean isEnabled() {
		boolean sessionFileExists = SoftwareCoSessionManager.softwareSessionFileExists();
		boolean hasJwt = SoftwareCoSessionManager.jwtExists();
		if (!sessionFileExists || !hasJwt) {
			return false;
		}
		return SoftwareCoUtils.isLoggedIn();
	}

}
