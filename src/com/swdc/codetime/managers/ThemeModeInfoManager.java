package com.swdc.codetime.managers;

import swdc.java.ops.manager.ThemeModeInfoHandler;

public class ThemeModeInfoManager implements ThemeModeInfoHandler {
	
	private static ThemeModeInfoManager instance = null;

	
	public static ThemeModeInfoManager getInstance() {
		if (instance == null) {
			synchronized (ThemeModeInfoManager.class) {
				if (instance == null) {
					instance = new ThemeModeInfoManager();
				}
			}
		}
		return instance;
	}

	@Override
	public boolean isLightMode() {
		return false;
	}

}
