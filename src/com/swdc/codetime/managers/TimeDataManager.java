package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTimeSummary;
import swdc.java.ops.model.Project;
import swdc.java.ops.model.TimeData;

public class TimeDataManager {

	public static void clearTimeDataSummary() {
		FileUtilManager.writeData(FileUtilManager.getTimeDataSummaryFile(), new JsonArray());
	}

	public static void updateEditorSeconds(long editorSeconds) {
		UtilManager.TimesData timesData = UtilManager.getTimesData();
		// get the current active project
		Project project = EclipseProjectUtil.getInstance().getFirstActiveProject();
		if (project != null) {
			TimeData td = getTodayTimeDataSummary(project);

			if (td != null) {
				td.setEditor_seconds(td.getEditor_seconds() + editorSeconds);
				td.setTimestamp_local(timesData.local_now);
				td.setEditor_seconds(Math.max(td.getEditor_seconds(), td.getSession_seconds()));

				saveTimeDataSummaryToDisk(td);
			}
		}
	}

	public static TimeData incrementSessionAndFileSeconds(Project keystrokeProj, long sessionSeconds) {
		TimeData td = getTodayTimeDataSummary(keystrokeProj);
		if (td != null) {
			td.setSession_seconds(td.getSession_seconds() + sessionSeconds);;
			td.setFile_seconds(td.getFile_seconds() + 60);

			td.setEditor_seconds(Math.max(td.getEditor_seconds(), td.getSession_seconds()));
			td.setFile_seconds(Math.min(td.getFile_seconds(), td.getSession_seconds()));

			saveTimeDataSummaryToDisk(td);
		}
		return td;
	}

	public static void updateSessionFromSummaryApi(long currentDayMinutes) {
		UtilManager.TimesData timesData = UtilManager.getTimesData();
		String day = UtilManager.getTodayInStandardFormat();

		CodeTimeSummary ctSummary = getCodeTimeSummary();
		// find out if there's a diff
		long diffActiveCodeMinutesToAdd = ctSummary.activeCodeTimeMinutes < currentDayMinutes
				? currentDayMinutes - ctSummary.activeCodeTimeMinutes
				: 0;

		Project activeProject = EclipseProjectUtil.getInstance().getFirstActiveProject();
		TimeData td = null;
		if (activeProject != null) {
			td = getTodayTimeDataSummary(activeProject);
		} else {
			// find the 1st one
			List<TimeData> timeDataList = getTimeDataList();
			if (timeDataList != null && timeDataList.size() > 0) {
				for (TimeData timeData : timeDataList) {
					if (timeData != null && timeData.getDay() != null && timeData.getDay().equals(day)) {
						// use this one
						td = timeData;
						break;
					}
				}
			}
		}

		if (td == null) {
			Project project = new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
			td = new TimeData();
			td.setDay(day);
			td.setTimestamp(timesData.now);
			td.setTimestamp_local(timesData.local_now);
			td.setProject(project);
		}

		long secondsToAdd = diffActiveCodeMinutesToAdd * 60;
		td.setSession_seconds(td.getSession_seconds() + secondsToAdd);
		td.setEditor_seconds(td.getEditor_seconds() + secondsToAdd);

		// save the info to disk
		// make sure editor seconds isn't less
		saveTimeDataSummaryToDisk(td);
	}

	private static List<TimeData> getTimeDataList() {
		JsonArray jsonArr = FileUtilManager.getFileContentAsJsonArray(FileUtilManager.getTimeDataSummaryFile());
		Type listType = new TypeToken<List<TimeData>>() {
		}.getType();
		List<TimeData> timeDataList = UtilManager.gson.fromJson(jsonArr, listType);
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
	public static TimeData getTodayTimeDataSummary(Project p) {
		if (p == null || p.getDirectory() == null) {
			return null;
		}
		String day = UtilManager.getTodayInStandardFormat();

		List<TimeData> timeDataList = getTimeDataList();

		if (timeDataList != null && timeDataList.size() > 0) {
			for (TimeData timeData : timeDataList) {
				if (timeData != null && timeData.getDay() != null && timeData.getProject() != null &&
						timeData.getProject().getDirectory() != null &&
						timeData.getDay().equals(day) && timeData.getProject().getDirectory().equals(p.getDirectory())) {
					// return it
					return timeData;
				}
			}
		}

		UtilManager.TimesData timesData = UtilManager.getTimesData();

		TimeData td = new TimeData();
		td.setDay(day);
		td.setTimestamp_local(timesData.local_now);
		td.setTimestamp(timesData.now);
		td.setProject(p);

		if (timeDataList == null) {
			timeDataList = new ArrayList<>();
		}

		timeDataList.add(td);
		// write it then return it
		FileUtilManager.writeData(FileUtilManager.getTimeDataSummaryFile(), timeDataList);
		return td;
	}

	public static CodeTimeSummary getCodeTimeSummary() {
		CodeTimeSummary summary = new CodeTimeSummary();

		String day = UtilManager.getTodayInStandardFormat();

		List<TimeData> timeDataList = getTimeDataList();

		if (timeDataList != null && timeDataList.size() > 0) {
			for (TimeData timeData : timeDataList) {
				if (timeData.getDay().equals(day)) {
					summary.activeCodeTimeMinutes += (timeData.getSession_seconds() / 60);
					summary.codeTimeMinutes += (timeData.getEditor_seconds() / 60);
					summary.fileTimeMinutes += (timeData.getFile_seconds() / 60);
				}
			}
		}

		return summary;
	}

	private static void saveTimeDataSummaryToDisk(TimeData timeData) {
		if (timeData == null) {
			return;
		}
		String dir = timeData.getProject().getDirectory();
		String day = timeData.getDay();

		// get the existing list
		List<TimeData> timeDataList = getTimeDataList();

        if (timeDataList != null && timeDataList.size() > 0) {
            for (int i = timeDataList.size() - 1; i >= 0; i--) {
                TimeData td = timeDataList.get(i);
                if (td != null && td.getDay() != null && td.getProject() != null &&
                		td.getProject().getDirectory() != null &&
                				td.getDay().equals(day) && td.getProject().getDirectory().equals(dir)) {
                    timeDataList.remove(i);
                    break;
                }
            }
        }
        timeDataList.add(timeData);

		// write it all
        FileUtilManager.writeData(FileUtilManager.getTimeDataSummaryFile(), timeDataList);
	}
}
