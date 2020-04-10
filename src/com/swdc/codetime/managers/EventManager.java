package com.swdc.codetime.managers;

import com.swdc.codetime.models.CodeTimeEvent;
import com.swdc.codetime.util.SoftwareCoUtils;

public class EventManager {

    private static String getPluginEventsFile() {
        String file = FileManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\events.json";
        } else {
            file += "/events.json";
        }
        return file;
    }

	public static void createCodeTimeEvent(String type, String name, String description) {
        SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        CodeTimeEvent event = new CodeTimeEvent();
        event.timestamp = timesData.now;
        event.timestamp_local = timesData.local_now;
        event.type = type;
        event.name = name;
        event.description = description;
        FileManager.appendData(getPluginEventsFile(), event);
    }

    public static void sendOfflineEvents() {
        FileManager.sendBatchData(getPluginEventsFile(), "/data/event");
    }
}
