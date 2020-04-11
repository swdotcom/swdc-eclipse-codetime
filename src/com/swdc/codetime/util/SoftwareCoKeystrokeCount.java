/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.FileAggregateDataManager;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.managers.SessionDataManager;
import com.swdc.codetime.managers.TimeDataManager;
import com.swdc.codetime.managers.WallClockManager;
import com.swdc.codetime.models.ElapsedTime;
import com.swdc.codetime.models.FileChangeInfo;
import com.swdc.codetime.models.KeystrokeAggregate;
import com.swdc.codetime.models.TimeData;

public class SoftwareCoKeystrokeCount {

	// event type
	private String type = "Events";
	// sublime = 1, vs code = 2, eclipse = 3, intelliJ = 4,
	// visual studio = 6, atom = 7
	private int pluginId = SoftwareCoUtils.pluginId;

	// non-hardcoded attributes
	private Map<String, FileInfo> source = new HashMap<>();
	private int keystrokes = 0; // keystroke count
	private long start;
	private long local_start;
	private SoftwareCoProject project;
	private String version;
	private String os;
	private String timezone = "";
	private long cumulative_editor_seconds = 0;
	private long elapsed_seconds = 0;

	public SoftwareCoKeystrokeCount() {
		this.version = SoftwareCoUtils.getVersion();
		if (this.version.endsWith(".qualifier")) {
			this.version = this.version.substring(0, this.version.lastIndexOf(".qualifier"));
		}
		this.os = SoftwareCoUtils.getOs();
	}

	public void resetData() {
		this.keystrokes = 0;
		this.source = new HashMap<>();
		if (this.project != null) {
			this.project.resetData();
		}
		this.start = 0L;
		this.local_start = 0L;
		this.timezone = "";
		this.cumulative_editor_seconds = 0;
		this.elapsed_seconds = 0;
	}

	public static class FileInfo {
		public Integer add = 0;
		public Integer paste = 0;
		public Integer open = 0;
		public Integer close = 0;
		public Integer delete = 0;
		public Integer length = 0;
		public Integer netkeys = 0;
		public Integer lines = 0;
		public Integer linesAdded = 0;
		public Integer linesRemoved = 0;
		public Integer keystrokes = 0;
		public String syntax = "";
		public long start = 0;
		public long end = 0;
		public long local_start = 0;
		public long local_end = 0;
		public long duration_seconds = 0;
		public String fsPath = "";
		public String name = "";
	}

	public FileInfo getSourceByFileName(String fileName) {
		if (source.get(fileName) != null) {
			return source.get(fileName);
		}

		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

		if (this.start == 0) {
			this.start = timesData.now;
			this.local_start = timesData.local_now;
			this.timezone = timesData.timezone;

			// start the keystroke processor 1 minute timer
			new Timer().schedule(new ProcessKeystrokesTimer(this), 1000 * 60);
		}

		// create one and return the one just created
		FileInfo fileInfoData = new FileInfo();
		fileInfoData.start = timesData.now;
		fileInfoData.local_start = timesData.local_now;
		source.put(fileName, fileInfoData);

		return fileInfoData;
	}

	public void endPreviousModifiedFiles(String currentFileName) {
		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
		Map<String, FileInfo> fileInfoDataSet = this.source;

		for (FileInfo fileInfoData : fileInfoDataSet.values()) {
			if (fileInfoData.end == 0) {
				fileInfoData.end = timesData.now;
				fileInfoData.local_end = timesData.local_now;
			}
		}
		if (fileInfoDataSet.get(currentFileName) != null) {
			FileInfo fileInfoData = fileInfoDataSet.get(currentFileName);
			fileInfoData.end = 0;
			fileInfoData.local_end = 0;
		}
	}

	public void endUnendedFiles(long elapsedSeconds, long sessionSeconds) {
		// increment the session minutes for this project
		TimeDataManager.incrementSessionAndFileSeconds(this.project, sessionSeconds);

		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
		TimeData td = TimeDataManager.getTodayTimeDataSummary(this.project);

		long editorSeconds = 60;
		if (td != null) {
			editorSeconds = Math.max(td.editor_seconds, td.session_seconds);
		}

		this.cumulative_editor_seconds = editorSeconds;
		this.elapsed_seconds = elapsedSeconds;

		Map<String, FileInfo> fileInfoDataSet = this.source;
		for (FileInfo fileInfoData : fileInfoDataSet.values()) {
			// end the ones that don't have an end time
			if (fileInfoData.end == 0) {
				// set the end time for this file
				fileInfoData.end = timesData.now;
				fileInfoData.local_end = timesData.local_now;
			}
		}
	}

	// update each source with it's true amount of keystrokes
	public boolean hasData() {
		boolean foundKpmData = false;
		if (this.getKeystrokes() > 0 || this.hasOpenAndCloseMetrics()) {
			foundKpmData = true;
		}

		int keystrokesTally = 0;

		// tally the metrics to set the keystrokes for each source key
		Map<String, FileInfo> fileInfoDataSet = this.source;
		for (FileInfo data : fileInfoDataSet.values()) {
			data.keystrokes = data.add + data.paste + data.delete + data.linesAdded + data.linesRemoved;
			keystrokesTally += data.keystrokes;
		}

		if (keystrokesTally > this.getKeystrokes()) {
			this.setKeystrokes(keystrokesTally);
		}

		return foundKpmData;
	}

	private boolean hasOpenAndCloseMetrics() {
		Map<String, FileInfo> fileInfoDataSet = this.source;
		for (FileInfo fileInfoData : fileInfoDataSet.values()) {
			if (fileInfoData.open > 0 && fileInfoData.close > 0) {
				return true;
			}
		}
		return false;
	}
	
	protected static void processKeystrokes(SoftwareCoKeystrokeCount keystrokeCount) {
		if (keystrokeCount.hasData()) {

			SoftwareCoSessionManager sessionMgr = SoftwareCoSessionManager.getInstance();

			ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

			// end the file end times.
			keystrokeCount.endUnendedFiles(eTime.elapsedSeconds, eTime.sessionSeconds);

			// update the file aggregate info.
			keystrokeCount.updateAggregates(eTime.sessionSeconds);

			final String payload = CodeTimeActivator.gson.toJson(keystrokeCount);

			// store to send later
			sessionMgr.storePayload(payload);

			// update the payload end timestmap
			SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
			FileManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
		}

		keystrokeCount.resetData();
		
		WallClockManager.getInstance().dispatchStatusViewUpdate();
	}
	
	public static class ProcessKeystrokesTimer extends TimerTask {
		SoftwareCoKeystrokeCount keystrokeCount;
		
		public ProcessKeystrokesTimer(SoftwareCoKeystrokeCount keystrokeCountCls) {
			this.keystrokeCount = keystrokeCountCls;
		}
		public void run() {
			processKeystrokes(keystrokeCount);
		}
	}

	private void updateAggregates(long sessionSeconds) {
		Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
		KeystrokeAggregate aggregate = new KeystrokeAggregate();
		if (this.project != null) {
			aggregate.directory = this.project.directory;
		} else {
			aggregate.directory = "Unnamed";
		}
		for (String key : this.source.keySet()) {
			FileInfo fileInfo = this.source.get(key);
			fileInfo.duration_seconds = fileInfo.end - fileInfo.start;
			fileInfo.fsPath = key;

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
		}

		// update the aggregate info
		long sessionMinutes = sessionSeconds / 60;
		SessionDataManager.incrementSessionSummary(aggregate, sessionMinutes);

		// update the file info map
		FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);

		WallClockManager.getInstance().dispatchStatusViewUpdate();
	}

	public String getSource() {
		return CodeTimeActivator.gson.toJson(source);
	}

	public int getKeystrokes() {
		return keystrokes;
	}

	public void setKeystrokes(int keystrokes) {
		this.keystrokes = keystrokes;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getLocal_start() {
		return local_start;
	}

	public void setLocal_start(long local_start) {
		this.local_start = local_start;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public SoftwareCoProject getProject() {
		return project;
	}

	public void setProject(SoftwareCoProject project) {
		this.project = project;
	}

	public String getType() {
		return type;
	}

	public int getPluginId() {
		return pluginId;
	}

	public String getVersion() {
		return version;
	}

	public String getOs() {
		return os;
	}

	public long getCumulative_editor_seconds() {
		return cumulative_editor_seconds;
	}

	public void setCumulative_editor_seconds(long cumulative_editor_seconds) {
		this.cumulative_editor_seconds = cumulative_editor_seconds;
	}

	public long getElapsed_seconds() {
		return elapsed_seconds;
	}

	public void setElapsed_seconds(long elapsed_seconds) {
		this.elapsed_seconds = elapsed_seconds;
	}

	@Override
	public String toString() {
		return "SoftwareCoKeystrokeCount [type=" + type + ", pluginId=" + pluginId + ", source=" + source
				+ ", keystrokes=" + keystrokes + ", start=" + start + ", local_start=" + local_start + ", timezone="
				+ timezone + ", project=" + project + ", version=" + version + "]";
	}

}
