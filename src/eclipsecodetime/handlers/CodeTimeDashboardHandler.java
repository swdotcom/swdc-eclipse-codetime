package eclipsecodetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipsecodetime.util.SoftwareCoSessionManager;

public class CodeTimeDashboardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchCodeTimeMetricsDashboard();

		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
