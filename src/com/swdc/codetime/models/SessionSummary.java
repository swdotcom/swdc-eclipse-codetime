package com.swdc.codetime.models;

public class SessionSummary {
	public long currentDayMinutes = 0L;
	public long currentDayKeystrokes = 0L;
	public long currentDayKpm = 0L;
	public long currentDayLinesAdded = 0L;
	public long currentDayLinesRemoved = 0L;
	public long averageDailyMinutes = 0L;
	public long averageDailyKeystrokes = 0L;
	public long averageDailyKpm = 0L;
	public long averageLinesAdded = 0L;
	public long averageLinesRemoved = 0L;
	public long globalAverageSeconds = 0L;
	public long globalAverageDailyMinutes = 0L;
	public long globalAverageDailyKeystrokes = 0L;
	public long globalAverageLinesAdded = 0L;
	public long globalAverageLinesRemoved = 0L;

	public void clone(SessionSummary in) {
		this.currentDayMinutes = in.currentDayMinutes;
		this.currentDayKeystrokes = in.currentDayKeystrokes;
		this.currentDayKpm = in.currentDayKpm;
		this.currentDayLinesAdded = in.currentDayLinesAdded;
		this.currentDayLinesRemoved = in.currentDayLinesRemoved;
		this.cloneNonCurrentMetrics(in);
	}

	public void cloneNonCurrentMetrics(SessionSummary in) {
		this.averageDailyMinutes = in.averageDailyMinutes;
		this.averageDailyKeystrokes = in.averageDailyKeystrokes;
		this.averageDailyKpm = in.averageDailyKpm;
		this.averageLinesAdded = in.averageLinesAdded;
		this.averageLinesRemoved = in.averageLinesRemoved;
		this.globalAverageSeconds = in.globalAverageSeconds;
		this.globalAverageDailyMinutes = in.globalAverageDailyMinutes;
		this.globalAverageDailyKeystrokes = in.globalAverageDailyKeystrokes;
		this.globalAverageLinesAdded = in.globalAverageLinesAdded;
		this.globalAverageLinesRemoved = in.globalAverageLinesRemoved;
	}
}
