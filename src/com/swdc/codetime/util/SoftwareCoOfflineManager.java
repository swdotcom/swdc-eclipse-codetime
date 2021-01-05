package com.swdc.codetime.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

public class SoftwareCoOfflineManager {

	public static final Logger LOG = Logger.getLogger("SoftwareCoOfflineManager");

	private static SoftwareCoOfflineManager instance = null;

	public SessionSummaryData sessionSummaryData = new SessionSummaryData();

	public static SoftwareCoOfflineManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoOfflineManager();
		}
		return instance;
	}

	protected class SessionSummaryData {
		public int currentDayMinutes;
		public int averageDailyMinutes;
		public int averageDailyKeystrokes;
		public int currentDayKeystrokes;
		public int liveshareMinutes;
	}

	public void setSessionSummaryData(int minutes, int keystrokes, int averageDailyMinutes) {
		sessionSummaryData = new SessionSummaryData();
		sessionSummaryData.currentDayKeystrokes = keystrokes;
		sessionSummaryData.currentDayMinutes = minutes;
		sessionSummaryData.averageDailyMinutes = averageDailyMinutes;
		saveSessionSummaryToDisk();
	}

	public void clearSessionSummaryData() {
		sessionSummaryData = new SessionSummaryData();
		saveSessionSummaryToDisk();
	}

	public void setSessionSummaryLiveshareMinutes(int minutes) {
		sessionSummaryData.liveshareMinutes = minutes;
	}

	public void incrementSessionSummaryData(int minutes, int keystrokes) {
		sessionSummaryData.currentDayMinutes += minutes;
		sessionSummaryData.currentDayKeystrokes += keystrokes;
		saveSessionSummaryToDisk();
	}

	public SessionSummaryData getSessionSummaryData() {
		return sessionSummaryData;
	}

	public void saveSessionSummaryToDisk() {
		File f = new File(FileUtilManager.getSessionDataSummaryFile());

		final String summaryDataJson = UtilManager.gson.toJson(sessionSummaryData);

		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
			writer.write(summaryDataJson);
		} catch (IOException ex) {
			// Report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}

	public String getSessionSummaryInfoFileContent() {
		String content = null;

		String sessionSummaryFile = FileUtilManager.getSummaryInfoFile();
		File f = new File(sessionSummaryFile);
		if (f.exists()) {
			try {
				Path p = Paths.get(sessionSummaryFile);
				synchronized (p) {
					byte[] encoded = Files.readAllBytes(p);
					content = new String(encoded, Charset.defaultCharset());
				}
				
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Code Time: Error trying to read and json parse the session file.", e);
			}
		}
		return content;
	}

}
