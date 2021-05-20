package com.swdc.codetime.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

import com.swdc.codetime.managers.EclipseProjectUtil;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.GitEventsManager;
import swdc.java.ops.model.Project;

public class SoftwareCoDeltaPrinter implements IResourceDeltaVisitor {
	
	private Project project = null;
	
	private static GitEventsManager gitEvtMgr = new GitEventsManager();
	// this needs to persist across instance creation
	private static Map<String, Long> lastSaveMap = new HashMap<String, Long>();
	
	public SoftwareCoDeltaPrinter() {
		project = EclipseProjectUtil.getInstance().getFirstActiveProject();
	}
	
    public boolean visit(IResourceDelta delta) {
        IResource res = delta.getResource();
        if (project != null && delta.getKind() == IResourceDelta.CHANGED && res != null && res.getType() == IResource.FILE && !res.getFileExtension().equals("class")) {
        	
        	long threshold = System.currentTimeMillis() - 1000;
        	long modTimestamp = res.getLocalTimeStamp();
        	String filePath = res.getFullPath().toFile().toString();
        	Long existingModTimestamp = lastSaveMap.get(filePath);
        	
        	if (modTimestamp > threshold && (existingModTimestamp == null || existingModTimestamp < threshold)) {
        		lastSaveMap.put(filePath, modTimestamp);
        		EventTrackerManager.getInstance().trackEditorAction("file", "save", filePath);
        		gitEvtMgr.trackUncommittedChanges(filePath);
        	}
        }
        return true; // visit the children
    }
}