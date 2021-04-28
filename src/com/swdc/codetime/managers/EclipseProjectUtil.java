package com.swdc.codetime.managers;

import java.io.File;

import swdc.java.ops.model.Project;

public class EclipseProjectUtil {
	
	private static EclipseProjectUtil instance = null;
	
	private EclipseProject proj;

	
	public static EclipseProjectUtil getInstance() {
		if (instance == null) {
			synchronized (EclipseProjectUtil.class) {
				if (instance == null) {
					instance = new EclipseProjectUtil();
				}
			}
		}
		return instance;
	}
	
	private EclipseProjectUtil() {
		proj = new EclipseProject();
	}
	
	public String getFileSyntax(File file) {
		return this.proj.getFileSyntax(file);
	}

	public Project getFirstActiveProject() {
		return this.proj.getFirstActiveProject();
	}

	public Project getProjectForPath(String fileName) {
		return this.proj.getProjectForPath(fileName);
	}
	
}
