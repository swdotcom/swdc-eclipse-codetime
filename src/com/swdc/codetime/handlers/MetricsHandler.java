package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.StatusBarManager;
import com.swdc.codetime.util.SoftwareCoUtils;

public class MetricsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CodeTimeActivator.SEND_TELEMTRY.set(true);
		StatusBarManager.setStatusLineMessage("Code Time", "paw.png", "Active code time today. Click to see more from Code Time");

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return !CodeTimeActivator.SEND_TELEMTRY.get();
	}

}
