package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.FileChangeInfo;
import com.swdc.codetime.util.SoftwareCoUtils;

public class FileAggregateDataManager {

    public static String getFileChangeSummaryFile() {
        String file = FileManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\fileChangeSummary.json";
        } else {
            file += "/fileChangeSummary.json";
        }
        return file;
    }

    public static void clearFileChangeInfoSummaryData() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
    }

    public static Map<String, FileChangeInfo>  getFileChangeInfo() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        JsonObject jsonObj = FileManager.getFileContentAsJson(getFileChangeSummaryFile());
        if (jsonObj != null) {
            Type type = new TypeToken<Map<String, FileChangeInfo>>() {}.getType();
			fileInfoMap = CodeTimeActivator.gson.fromJson(jsonObj, type);
        } else {
            // create it
            FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
        }
        return fileInfoMap;
    }

    public static void updateFileChangeInfo(Map<String, FileChangeInfo> fileInfoMap) {
        FileManager.writeData(getFileChangeSummaryFile(), fileInfoMap);
    }
}