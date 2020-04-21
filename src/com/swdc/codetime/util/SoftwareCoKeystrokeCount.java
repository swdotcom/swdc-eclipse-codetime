/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
	public long start;
	private long local_start;
	private SoftwareCoProject project;
	private String version;
	private String os;
	private String timezone = "";
	
	public long cumulative_editor_seconds = 0;
    public long cumulative_session_seconds = 0;
    public long elapsed_seconds = 0;
    public int new_day = 0; // 1 or zero to denote new day or not
    public String project_null_error = "";
    public String editor_seconds_error = "";
    public String session_seconds_error = "";

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
		this.cumulative_session_seconds = 0;
		this.new_day = 0;
		this.project_null_error = "";
		this.editor_seconds_error = "";
		this.session_seconds_error = "";
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
	
	private void validateAndUpdateCumulativeData(long sessionSeconds) {

        TimeData td = TimeDataManager.incrementSessionAndFileSeconds(this.project, sessionSeconds);

        // get the current payloads so we can compare our last cumulative seconds
        SoftwareCoKeystrokeCount lastPayload = FileManager.getLastSavedKeystrokeStats();
        if (SoftwareCoUtils.isNewDay()) {
        	lastPayload = null;
        	// clear out data from the previous day
            WallClockManager.getInstance().newDayChecker();
            if (td != null) {
            	this.project_null_error = "TimeData should be null as its a new day";
                td = null;
            }
        }
        
        // add the cumulative data
        long lastPayloadEnd = FileManager.getNumericItem("latestPayloadTimestampEndUtc", 0L);
        this.new_day = lastPayloadEnd == 0 ? 1 : 0;

        this.cumulative_session_seconds = 60;
        this.cumulative_editor_seconds = 60;

        if (td != null) {
            this.cumulative_editor_seconds = td.editor_seconds;
            this.cumulative_session_seconds = td.session_seconds;
        } else if (lastPayload != null) {
            // no time data found, project null error
            this.project_null_error = "TimeData not found using " + this.project.directory + " for editor and session seconds";
            cumulative_editor_seconds = lastPayload.cumulative_editor_seconds + 60;
            cumulative_session_seconds = lastPayload.cumulative_session_seconds + 60;
        }

        if (cumulative_editor_seconds < cumulative_session_seconds) {
            long diff = cumulative_session_seconds - cumulative_editor_seconds;
            if (diff > 30) {
                this.editor_seconds_error = "Cumulative editor seconds is behind session seconds by " + diff + " seconds";
            }
            cumulative_editor_seconds = cumulative_session_seconds;
        }
    }
	
	// end unended file payloads and add the cumulative editor seconds
    public void preProcessKeystrokeData(long sessionSeconds, long elapsedSeconds) {

        this.validateAndUpdateCumulativeData(sessionSeconds);

        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        Map<String, FileInfo> fileInfoDataSet = this.source;
        for ( FileInfo fileInfoData : fileInfoDataSet.values() ) {
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
	
	public void processKeystrokes() {
		if (this.hasData()) {
			
			// make sure a project is available
            if (this.project == null || this.project.directory == null || this.project.directory.equals("")) {
                this.project = new SoftwareCoProject("Unnamed", "Untitled");
            }

			ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

			// end the file end times.
			this.preProcessKeystrokeData(eTime.sessionSeconds, eTime.elapsedSeconds);

			// update the file aggregate info.
			this.updateAggregates(eTime.sessionSeconds);

			final String payload = CodeTimeActivator.gson.toJson(this);

			// store to send later
			SoftwareCoSessionManager.getInstance().storePayload(payload);

			// update the payload end timestmap
			SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
			FileManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
		}

		this.resetData();
		
		WallClockManager.getInstance().dispatchStatusViewUpdate();
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
