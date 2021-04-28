package com.swdc.codetime.managers;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Project;
import swdc.java.ops.providers.IdeProject;

public class EclipseProject implements IdeProject {
	
	public static String lastOpenFile = "";

	@Override
	public Project buildKeystrokeProject(Object iProj) {
		return createProject((IProject) iProj);
	}

	@Override
	public String getFileSyntax(File file) {
		if (file == null || !file.exists()) {
			return "";
		}
		return com.google.common.io.Files.getFileExtension(file.getAbsolutePath());
	}

	@Override
	public Project getFirstActiveProject() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null && projects.length > 0) {
			if (lastOpenFile != null && !lastOpenFile.isEmpty()) {
				for (IProject proj : projects) {
					IPath locationPath = proj.getLocation();
					String pathStr = locationPath.toString();
					if (lastOpenFile.indexOf(pathStr) != -1) {
						return createProject(proj);
					}
				}
			}
			// not found, just return the 1st proj
			return createProject(projects[0]);
		}
		return null;
	}

	@Override
	public Project getOpenProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Project getProjectForPath(String fileName) {
		if (StringUtils.isBlank(fileName)) {
			return null;
		}
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null && projects.length > 0) {
			for (IProject project : projects) {
				IPath locationPath = project.getLocation();
				String pathStr = locationPath.toString();
				if (pathStr != null && fileName.indexOf(pathStr) != -1) {
					return createProject(project);
				}
			}
		}
		return getFirstActiveProject();
	}
    
    private Project createProject(IProject iproj) {
        if (iproj != null) {
        	String directory = iproj.getLocationURI().getPath();
			if (directory == null || directory.equals("")) {
				directory = iproj.getLocation().toString();
			}
        	return new Project(directory, iproj.getName());
            // return new Project(p.getProjectDirectory().getName(), p.getProjectDirectory().getPath());
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

}
