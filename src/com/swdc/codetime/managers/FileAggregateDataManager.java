package com.swdc.codetime.managers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.models.FileChangeInfo;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

public class FileAggregateDataManager {


    public static void clearFileChangeInfoSummaryData() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }

    public static Map<String, FileChangeInfo>  getFileChangeInfo() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getFileChangeSummaryFile());
        if (jsonObj != null) {
            Type type = new TypeToken<Map<String, FileChangeInfo>>() {}.getType();
			fileInfoMap = UtilManager.gson.fromJson(jsonObj, type);
        } else {
            // create it
        	FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
        }
        return fileInfoMap;
    }

    public static void updateFileChangeInfo(Map<String, FileChangeInfo> fileInfoMap) {
    	FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }
}