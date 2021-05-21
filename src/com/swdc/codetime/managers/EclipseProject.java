package com.swdc.codetime.managers;

import java.io.File;

import org.apache.commons.lang.NotImplementedException;
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
			
			if (StringUtils.isNotBlank(lastOpenFile)) {
				for (IProject proj : projects) {
					IPath locationPath = proj.getLocation();
					String projectPath = locationPath.makeAbsolute().toString();
					if (proj.isOpen() && lastOpenFile.indexOf(projectPath) != -1) {
						return createProject(proj);
					}
				}
			}
			
			for (IProject proj : projects) {
				if (proj.isOpen()) {
					return createProject(proj);
				}
			}
			// not found, just return the 1st proj
			return createProject(projects[0]);
		}
		return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
	}

	@Override
	public Project getOpenProject() {
		throw new NotImplementedException();
	}

	@Override
	public Project getProjectForPath(String fileName) {
		if (StringUtils.isBlank(fileName)) {
			return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
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
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

}
