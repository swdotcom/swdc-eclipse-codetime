package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.util.SoftwareCoUtils;

public class StopMetricsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CodeTimeActivator.SEND_TELEMTRY.set(false);
        SoftwareCoUtils.setStatusLineMessage("Paused", "paw.png", "Enable metrics to resume");
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return CodeTimeActivator.SEND_TELEMTRY.get();
	}

}
