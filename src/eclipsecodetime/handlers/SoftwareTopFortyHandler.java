package eclipsecodetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipsecodetime.util.SoftwareCoSessionManager;

public class SoftwareTopFortyHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchSoftwareTopForty();

		return null;
	}

}
