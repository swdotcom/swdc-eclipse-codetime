package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.CodeTimeSummary;
import com.swdc.codetime.models.TimeData;
import com.swdc.codetime.util.SoftwareCoProject;
import com.swdc.codetime.util.SoftwareCoUtils;

public class TimeDataManager {

	private static String getTimeDataSummaryFile() {
		String file = FileManager.getSoftwareDir(true);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\projectTimeData.json";
		} else {
			file += "/projectTimeData.json";
		}
		return file;
	}

	public static void clearTimeDataSummary() {
		FileManager.writeData(getTimeDataSummaryFile(), new JsonArray());
	}

	public static void updateEditorSeconds(long editorSeconds) {
		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
		// get the current active project
		IProject project = SoftwareCoUtils.getActiveProject();
		if (project != null) {
			// build the keystroke project
			SoftwareCoProject keystrokeProj = new SoftwareCoProject(project.getName(),
					project.getFullPath().toString());

			TimeData td = getTodayTimeDataSummary(keystrokeProj);

			if (td != null) {
				td.editor_seconds = td.editor_seconds + editorSeconds;
				td.timestamp_local = timesData.local_now;
				td.editor_seconds = Math.max(td.editor_seconds, td.session_seconds);

				saveTimeDataSummaryToDisk(td);
			}
		}
	}

	public static TimeData incrementSessionAndFileSeconds(SoftwareCoProject keystrokeProj, long sessionSeconds) {
		TimeData td = getTodayTimeDataSummary(keystrokeProj);
		if (td != null) {
			td.session_seconds += sessionSeconds;
			td.file_seconds += 60;

			td.editor_seconds = Math.max(td.editor_seconds, td.session_seconds);
			td.file_seconds = Math.min(td.file_seconds, td.session_seconds);

			saveTimeDataSummaryToDisk(td);
		}
		return td;
	}

	public static void updateSessionFromSummaryApi(long currentDayMinutes) {
		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
		String day = SoftwareCoUtils.getTodayInStandardFormat();

		CodeTimeSummary ctSummary = getCodeTimeSummary();
		// find out if there's a diff
		long diffActiveCodeMinutesToAdd = ctSummary.activeCodeTimeMinutes < currentDayMinutes
				? currentDayMinutes - ctSummary.activeCodeTimeMinutes
				: 0;

		IProject activeProject = SoftwareCoUtils.getActiveProject();
		TimeData td = null;
		if (activeProject != null) {
			SoftwareCoProject project = new SoftwareCoProject(activeProject.getName(),
					activeProject.getFullPath().toString());
			td = getTodayTimeDataSummary(project);
		} else {
			// find the 1st one
			List<TimeData> timeDataList = getTimeDataList();
			if (timeDataList != null && timeDataList.size() > 0) {
				for (TimeData timeData : timeDataList) {
					if (timeData != null && timeData.day != null && timeData.day.equals(day)) {
						// use this one
						td = timeData;
						break;
					}
				}
			}
		}

		if (td == null) {
			SoftwareCoProject project = new SoftwareCoProject("Unnamed", "Untitled");
			td = new TimeData();
			td.day = day;
			td.timestamp = timesData.now;
			td.timestamp_local = timesData.local_now;
			td.project = project;
		}

		long secondsToAdd = diffActiveCodeMinutesToAdd * 60;
		td.session_seconds += secondsToAdd;
		td.editor_seconds += secondsToAdd;

		// save the info to disk
		// make sure editor seconds isn't less
		saveTimeDataSummaryToDisk(td);
	}

	private static List<TimeData> getTimeDataList() {
		JsonArray jsonArr = FileManager.getFileContentAsJsonArray(getTimeDataSummaryFile());
		Type listType = new TypeToken<List<TimeData>>() {
		}.getType();
		List<TimeData> timeDataList = CodeTimeActivator.gson.fromJson(jsonArr, listType);
		if (timeDataList == null) {
			timeDataList = new ArrayList<>();
		}
		return timeDataList;
	}

	/**
	 * Get the current time data info that is saved on disk. If not found create an
	 * empty one.
	 * 
	 * @return
	 */
	public static TimeData getTodayTimeDataSummary(SoftwareCoProject p) {
		if (p == null || p.directory == null) {
			return null;
		}
		String day = SoftwareCoUtils.getTodayInStandardFormat();

		List<TimeData> timeDataList = getTimeDataList();

		if (timeDataList != null && timeDataList.size() > 0) {
			for (TimeData timeData : timeDataList) {
				if (timeData != null && timeData.day != null && timeData.project != null &&
						timeData.project.directory != null &&
						timeData.day.equals(day) && timeData.project.directory.equals(p.directory)) {
					// return it
					return timeData;
				}
			}
		}

		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();

		TimeData td = new TimeData();
		td.day = day;
		td.timestamp_local = timesData.local_now;
		td.timestamp = timesData.now;
		td.project = p.clone();

		if (timeDataList == null) {
			timeDataList = new ArrayList<>();
		}

		timeDataList.add(td);
		// write it then return it
		FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
		return td;
	}

	public static CodeTimeSummary getCodeTimeSummary() {
		CodeTimeSummary summary = new CodeTimeSummary();

		String day = SoftwareCoUtils.getTodayInStandardFormat();

		List<TimeData> timeDataList = getTimeDataList();

		if (timeDataList != null && timeDataList.size() > 0) {
			for (TimeData timeData : timeDataList) {
				if (timeData.day.equals(day)) {
					summary.activeCodeTimeMinutes += (timeData.session_seconds / 60);
					summary.codeTimeMinutes += (timeData.editor_seconds / 60);
					summary.fileTimeMinutes += (timeData.file_seconds / 60);
				}
			}
		}

		return summary;
	}

	private static void saveTimeDataSummaryToDisk(TimeData timeData) {
		if (timeData == null) {
			return;
		}
		String dir = timeData.project.directory;
		String day = timeData.day;

		// get the existing list
		List<TimeData> timeDataList = getTimeDataList();

        if (timeDataList != null && timeDataList.size() > 0) {
            for (int i = timeDataList.size() - 1; i >= 0; i--) {
                TimeData td = timeDataList.get(i);
                if (td != null && td.day != null && td.project != null &&
                		td.project.directory != null &&
                				td.day.equals(day) && td.project.directory.equals(dir)) {
                    timeDataList.remove(i);
                    break;
                }
            }
        }
        timeDataList.add(timeData);

		// write it all
		FileManager.writeData(getTimeDataSummaryFile(), timeDataList);
	}
}
