package com.swdc.codetime.models;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.swdc.codetime.managers.EclipseProjectUtil;
import com.swdc.codetime.managers.SessionDataManager;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.GitUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.CodeTime.FileInfo;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.Project;
import swdc.java.ops.model.ResourceInfo;

public class KeystrokeCountUtil {

	public static long cumulative_editor_seconds = 0;
	public static long elapsed_seconds = 0;
	public static String project_null_error = "";

	public static void processKeystrokes(CodeTime keystrokeCountInfo) {
		if (keystrokeCountInfo.hasData()) {

			// make sure we have a valid project
			Project activeProject = EclipseProjectUtil.getInstance().getFirstActiveProject();
			if (keystrokeCountInfo.getProject() == null
					|| StringUtils.isBlank(keystrokeCountInfo.getProject().getDirectory())) {
				keystrokeCountInfo.setProject(activeProject);
			}

			// get the resource identifier info
			if (!keystrokeCountInfo.getProject().getDirectory().equals(UtilManager.untitled_file_name)) {
				// get the resource information
				ResourceInfo resourceInfo = GitUtilManager.getResourceInfo(keystrokeCountInfo.getProject().getDirectory());
				if (resourceInfo != null) {
					keystrokeCountInfo.getProject().setResource(resourceInfo);
					keystrokeCountInfo.getProject().setIdentifier(resourceInfo.getIdentifier());
				}
			}

			ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

			// end the file end times.
			preProcessKeystrokeData(keystrokeCountInfo, eTime.sessionSeconds, eTime.elapsedSeconds);

			// send the event to the event tracker
			EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeCountInfo);

			// update the payload end timestmap
			UtilManager.TimesData timesData = UtilManager.getTimesData();
			FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
		}

		keystrokeCountInfo.resetData();
	}

	// end unended file payloads and add the cumulative editor seconds
	public static void preProcessKeystrokeData(CodeTime keystrokeCountInfo, long sessionSeconds, long elapsedSeconds) {

		UtilManager.TimesData timesData = UtilManager.getTimesData();
		Map<String, FileInfo> fileInfoDataSet = keystrokeCountInfo.getSource();
		for (FileInfo fileInfoData : fileInfoDataSet.values()) {
			// end the ones that don't have an end time
			if (fileInfoData.end == 0) {
				// set the end time for this file
				fileInfoData.end = timesData.now;
				fileInfoData.local_end = timesData.local_now;
			}
		}
	}
}
