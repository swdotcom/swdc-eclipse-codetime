package com.swdc.codetime.handlers;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.swdc.codetime.util.SoftwareCoSessionManager;

import swdc.java.ops.manager.FileUtilManager;

public class SoftwareLoginHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SoftwareCoSessionManager.launchLogin("email", false);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return (StringUtils.isBlank(FileUtilManager.getItem("name")));
	}

}
