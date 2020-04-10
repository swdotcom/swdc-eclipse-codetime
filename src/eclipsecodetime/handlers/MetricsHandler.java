package eclipsecodetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipsecodetime.Activator;

import eclipsecodetime.util.SoftwareCoUtils;

public class MetricsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Activator.SEND_TELEMTRY.set(true);
		SoftwareCoUtils.setStatusLineMessage("Code Time", "paw.png", "Active code time today. Click to see more from Code Time");

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return !Activator.SEND_TELEMTRY.get();
	}

}
