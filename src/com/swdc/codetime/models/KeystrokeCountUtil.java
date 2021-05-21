package com.swdc.codetime.models;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.swdc.codetime.managers.EclipseProjectUtil;
import com.swdc.codetime.managers.FileAggregateDataManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.TimeDataManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.util.SoftwareCoUtils;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.GitUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.CodeTime.FileInfo;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.FileChangeInfo;
import swdc.java.ops.model.Project;
import swdc.java.ops.model.ResourceInfo;
import swdc.java.ops.model.TimeData;

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

			// update the file aggregate info.
			updateAggregates(keystrokeCountInfo, eTime.sessionSeconds);

			// send the event to the event tracker
			EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeCountInfo);

			// update the payload end timestmap
			UtilManager.TimesData timesData = UtilManager.getTimesData();
			FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
		}

		keystrokeCountInfo.resetData();

		WallClockManager.getInstance().dispatchStatusViewUpdate();
	}

	private static void validateAndUpdateCumulativeData(CodeTime keystrokeCountInfo, long sessionSeconds) {

		TimeData td = TimeDataManager.incrementSessionAndFileSeconds(keystrokeCountInfo.getProject(), sessionSeconds);

		// get the current payloads so we can compare our last cumulative seconds
		if (UtilManager.isNewDay()) {
			// clear out data from the previous day
			WallClockManager.getInstance().newDayChecker();
			if (td != null) {
				td = null;
			}
		}

		keystrokeCountInfo.workspace_name = SoftwareCoUtils.getWorkspaceName();

		keystrokeCountInfo.cumulative_session_seconds = 60;
		cumulative_editor_seconds = 60;

		if (td != null) {
			cumulative_editor_seconds = td.getEditor_seconds();
			keystrokeCountInfo.cumulative_session_seconds = td.getSession_seconds();
		}

		if (cumulative_editor_seconds < keystrokeCountInfo.cumulative_session_seconds) {
			cumulative_editor_seconds = keystrokeCountInfo.cumulative_session_seconds;
		}
	}

	// end unended file payloads and add the cumulative editor seconds
	public static void preProcessKeystrokeData(CodeTime keystrokeCountInfo, long sessionSeconds, long elapsedSeconds) {

		validateAndUpdateCumulativeData(keystrokeCountInfo, sessionSeconds);

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

	private static void updateAggregates(CodeTime keystrokeCountInfo, long sessionSeconds) {
		Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
		KeystrokeAggregate aggregate = new KeystrokeAggregate();
		if (keystrokeCountInfo.getProject() != null) {
			aggregate.directory = keystrokeCountInfo.getProject().getDirectory();
		} else {
			aggregate.directory = "Unnamed";
		}
		for (String key : keystrokeCountInfo.getSource().keySet()) {
			FileInfo fileInfo = keystrokeCountInfo.getSource().get(key);
			fileInfo.duration_seconds = fileInfo.end - fileInfo.start;
			fileInfo.fsPath = key;

			try {
				Path path = Paths.get(key);
				if (path != null) {
					Path fileName = path.getFileName();
					if (fileName != null) {
						fileInfo.name = fileName.toString();
					}
				}

				aggregate.aggregate(fileInfo);

				FileChangeInfo existingFileInfo = fileChangeInfoMap.get(key);
				if (existingFileInfo == null) {
					existingFileInfo = new FileChangeInfo();
					fileChangeInfoMap.put(key, existingFileInfo);
				}
				existingFileInfo.aggregate(fileInfo);
				existingFileInfo.kpm = existingFileInfo.keystrokes / existingFileInfo.update_count;
			} catch (Exception e) {
				//
			}
		}

		// update the aggregate info
		long sessionMinutes = sessionSeconds / 60;
		SessionDataManager.incrementSessionSummary(aggregate, sessionMinutes);

		// update the file info map
		FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);

		WallClockManager.getInstance().dispatchStatusViewUpdate();
	}
}
