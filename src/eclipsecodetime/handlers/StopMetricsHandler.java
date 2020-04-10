package eclipsecodetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipsecodetime.Activator;

import eclipsecodetime.util.SoftwareCoUtils;

public class StopMetricsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Activator.SEND_TELEMTRY.set(false);
        SoftwareCoUtils.setStatusLineMessage("Paused", "paw.png", "Enable metrics to resume");
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return Activator.SEND_TELEMTRY.get();
	}

}
