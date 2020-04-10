package eclipsecodetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipsecodetime.util.SoftwareCoSessionManager;
import eclipsecodetime.util.SoftwareCoUtils;

public class SoftwareLoginHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchLogin("onboard");
		return null;
	}

	@Override
	public boolean isEnabled() {
		boolean sessionFileExists = SoftwareCoSessionManager.softwareSessionFileExists();
		boolean hasJwt = SoftwareCoSessionManager.jwtExists();
		if (!sessionFileExists || !hasJwt) {
			return true;
		}
		return !SoftwareCoUtils.isLoggedIn();
	}

}
