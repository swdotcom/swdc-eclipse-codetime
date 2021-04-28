package com.swdc.codetime.handlers;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoSessionManager;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class MetricsDashboardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchWebDashboard(UIInteractionType.keyboard);
		return null;
	}

	@Override
	public boolean isEnabled() {
		String name = FileUtilManager.getItem("name");
		return StringUtils.isNotBlank(name);
	}

}
