package com.swdc.codetime.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;

public class SoftwareLoginHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchLogin("email");
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
