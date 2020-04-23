/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class SoftwareCoKeystrokeManager {

	private static SoftwareCoKeystrokeManager instance = null;
	
	// KeystrokeCount cache metadata
	List<KeystrokeCountWrapper> keystrokeCountWrapperList = new ArrayList<KeystrokeCountWrapper>();

	/**
	 * Protected constructor to defeat instantiation
	 */
	protected SoftwareCoKeystrokeManager() {
		//
	}

	public static SoftwareCoKeystrokeManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoKeystrokeManager();
		}
		return instance;
	}

    public void resetData(String projectName) {
        for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
            if (wrapper.getProjectName() != null && projectName.equals(wrapper.getProjectName())) {
                wrapper.getKeystrokeCount().resetData();
                break;
            }
        }
    }

    public SoftwareCoKeystrokeCount getKeystrokeCount(String projectName) {
        for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
            if (wrapper.getProjectName() != null && projectName.equals(wrapper.getProjectName())) {
                return wrapper.getKeystrokeCount();
            }
        }
        return null;
    }
    
    public List<SoftwareCoKeystrokeCount> getKeystrokeCounts() {
    	List<SoftwareCoKeystrokeCount> list = new ArrayList<>();
    	if (keystrokeCountWrapperList != null && keystrokeCountWrapperList.size() > 0) {
    		for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
                list.add(wrapper.getKeystrokeCount());
            }
    	}
    	return list;
    }

    public void setKeystrokeCount(String projectName, SoftwareCoKeystrokeCount keystrokeCount, String fileName) {
        for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
            if (wrapper.getProjectName() != null && projectName.equals(wrapper.getProjectName())) {
                wrapper.setKeystrokeCount(keystrokeCount);
                return;
            }
        }
        
        // not found by project
        // create the project based on the incoming projectName and fileName
        IProject iproj = SoftwareCoUtils.getFileProject(fileName);
        
        SoftwareCoProject project = null;
        if (iproj != null) {
        	// awesome, we found the workspace project. use it
        	String directory = iproj.getLocationURI().getPath();
			if (directory == null || directory.equals("")) {
				directory = iproj.getLocation().toString();
			}
			String name = iproj.getName();
			project = new SoftwareCoProject(name, directory);
        } else {
        	project = new SoftwareCoProject("Unnamed", "Untitled");
        }
        keystrokeCount.setProject(project);
        

        // didn't find it, time to create a wrapper
        KeystrokeCountWrapper wrapper = new KeystrokeCountWrapper();
        wrapper.setKeystrokeCount(keystrokeCount);
        wrapper.setLastUpdateTime(System.currentTimeMillis());
        wrapper.setProjectName(project.name);
        keystrokeCountWrapperList.add(wrapper);
    }

    public KeystrokeCountWrapper getKeystrokeWrapper(String projectName) {
        for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
            if (wrapper.getProjectName() != null && projectName.equals(wrapper.getProjectName())) {
                return wrapper;
            }
        }
        return null;
    }

    public void processKeystrokeCountForTermination() {
        for (KeystrokeCountWrapper wrapper : keystrokeCountWrapperList) {
            // this will ensure we process the latest keystroke updates
            wrapper.setLastUpdateTime(0l);
        }
    }

    public List<KeystrokeCountWrapper> getKeystrokeCountWrapperList() {
        return this.keystrokeCountWrapperList;
    }
	
	public class KeystrokeCountWrapper {
		// KeystrokeCount cache metadata
		protected SoftwareCoKeystrokeCount keystrokeCount;
		protected String projectName;
		protected long lastUpdateTime = 0; // in millis
		protected int currentTextLength = 0;
		
		public SoftwareCoKeystrokeCount getKeystrokeCount() {
            return keystrokeCount;
        }

        public void setKeystrokeCount(SoftwareCoKeystrokeCount keystrokeCount) {
            this.keystrokeCount = keystrokeCount;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public int getCurrentTextLength() {
            return currentTextLength;
        }

        public void setCurrentTextLength(int currentTextLength) {
            this.currentTextLength = currentTextLength;
        }

	}
}
