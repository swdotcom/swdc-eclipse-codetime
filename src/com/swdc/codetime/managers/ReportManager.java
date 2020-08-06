package com.swdc.codetime.managers;


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.swdc.codetime.models.CommitChangeStats;
import com.swdc.codetime.util.GitUtil;
import com.swdc.codetime.util.SoftwareCoProject;
import com.swdc.codetime.util.SoftwareCoSessionManager;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;

public class ReportManager {

    private static int DASHBOARD_COL_WIDTH = 21;
    private static int DASHBOARD_LRG_COL_WIDTH = 38;
    private static int TABLE_WIDTH = 80;

    private static SimpleDateFormat formatDayTime = new SimpleDateFormat("EEE, MMM d h:mma");
    private static SimpleDateFormat formatDayYear = new SimpleDateFormat("MMM d, YYYY");

    public static String getProjectContributorSummaryFile() {
        String file = FileManager.getSoftwareDir(true);
        if (SoftwareCoUtils.isWindows()) {
            file += "\\ProjectContributorCodeSummary.txt";
        } else {
            file += "/ProjectContributorCodeSummary.txt";
        }
        return file;
    }

    public static void displayProjectContributorSummaryDashboard(String identifier) {
        StringBuffer sb = new StringBuffer();
        String file = getProjectContributorSummaryFile();

        SoftwareCoProject p = SoftwareCoUtils.getActiveKeystrokeProject();
        if (p != null) {
            SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
            String email = GitUtil.getUsersEmail(p.directory);
            CommitChangeStats usersTodaysCommits = GitUtil.getTodaysCommits(p.directory, email);
            CommitChangeStats contribTodaysCommits = GitUtil.getTodaysCommits(p.directory, null);

            CommitChangeStats usersYesterdaysCommits = GitUtil.getYesterdaysCommits(p.directory, email);
            CommitChangeStats contribYesterdaysCommits = GitUtil.getYesterdaysCommits(p.directory, null);

            CommitChangeStats usersThisWeeksCommits = GitUtil.getThisWeeksCommits(p.directory, email);
            CommitChangeStats contribThisWeeksCommits = GitUtil.getThisWeeksCommits(p.directory, null);

            String lastUpdatedStr = formatDayTime.format(new Date());
            sb.append(getTableHeader("PROJECT SUMMARY", " (Last updated on " + lastUpdatedStr + ")", true));
            sb.append("\n\n Project: ").append(identifier).append("\n\n");

            // TODAY
            String projectDate = formatDayYear.format(timesData.local_start_today_date);
            sb.append(getRightAlignedTableHeader("Today (" + projectDate + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersTodaysCommits.commitCount, contribTodaysCommits.commitCount));
            sb.append(getRowNumberData("Files changed", usersTodaysCommits.fileCount, contribTodaysCommits.fileCount));
            sb.append(getRowNumberData("Insertions", usersTodaysCommits.insertions, contribTodaysCommits.insertions));
            sb.append(getRowNumberData("Deletions", usersTodaysCommits.deletions, contribTodaysCommits.deletions));
            sb.append("\n");

            // YESTERDAY
            String yesterday = formatDayYear.format(timesData.local_start_of_yesterday_date);
            sb.append(getRightAlignedTableHeader("Yesterday (" + yesterday + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersYesterdaysCommits.commitCount, contribYesterdaysCommits.commitCount));
            sb.append(getRowNumberData("Files changed", usersYesterdaysCommits.fileCount, contribYesterdaysCommits.fileCount));
            sb.append(getRowNumberData("Insertions", usersYesterdaysCommits.insertions, contribYesterdaysCommits.insertions));
            sb.append(getRowNumberData("Deletions", usersYesterdaysCommits.deletions, contribYesterdaysCommits.deletions));
            sb.append("\n");

            // THIS WEEK
            String startOfWeek = formatDayYear.format(timesData.local_start_of_week_date);
            sb.append(getRightAlignedTableHeader("This week (" + startOfWeek + " to " + projectDate + ")"));
            sb.append(getColumnHeaders(Arrays.asList("Metric", "You", "All Contributors")));
            sb.append(getRowNumberData("Commits", usersThisWeeksCommits.commitCount, contribThisWeeksCommits.commitCount));
            sb.append(getRowNumberData("Files changed", usersThisWeeksCommits.fileCount, contribThisWeeksCommits.fileCount));
            sb.append(getRowNumberData("Insertions", usersThisWeeksCommits.insertions, contribThisWeeksCommits.insertions));
            sb.append(getRowNumberData("Deletions", usersThisWeeksCommits.deletions, contribThisWeeksCommits.deletions));
            sb.append("\n");

        } else {
            sb.append("Project information not found");
        }

        // write the summary content
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(sb.toString());
        } catch (IOException ex) {
            // Report
        } finally {
            try {writer.close();} catch (Exception ex) {/*ignore*/}
        }

        SoftwareCoSessionManager.launchFile(file);
        
        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = "ct_contributor_repo_identifier_btn";
        elementEntity.element_location = "ct_contributors_tree";
        elementEntity.color = null;
        elementEntity.cta_text = "View your commit report";
        elementEntity.icon_name = "repo";
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
    }

    private static String getRowNumberData(String title, long userStat, long contribStat) {
        String userStatStr = SoftwareCoUtils.humanizeLongNumbers(userStat);
        String contribStatStr = SoftwareCoUtils.humanizeLongNumbers(contribStat);
        List<String> labels = Arrays.asList(title, userStatStr, contribStatStr);
        return getRowLabels(labels);
    }

    private static String getSpaces(int spacesRequired) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < spacesRequired; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String getBorder(int borderLen) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < borderLen; i++) {
            sb.append("-");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getRightAlignedTableLabel(String label, int colWidth) {
        int spacesRequired = colWidth - label.length();
        String spaces = getSpaces(spacesRequired);
        return spaces + "" + label;
    }

    private static String getTableHeader(String leftLabel, String rightLabel, boolean isFullTable) {
        int fullLen = !isFullTable ? TABLE_WIDTH - DASHBOARD_COL_WIDTH : TABLE_WIDTH;
        int spacesRequired = fullLen - leftLabel.length() - rightLabel.length();
        String spaces = getSpaces(spacesRequired);
        return leftLabel + "" + spaces + "" + rightLabel;
    }

    private static String getRightAlignedTableHeader(String label) {
        StringBuffer sb = new StringBuffer();
        String alignedHeader = getRightAlignedTableLabel(label, TABLE_WIDTH);
        sb.append(alignedHeader).append("\n");
        sb.append(getBorder(TABLE_WIDTH));
        return sb.toString();
    }

    private static String getRowLabels(List<String> labels) {
        StringBuffer sb = new StringBuffer();
        int spacesRequired = 0;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (i == 0) {
                sb.append(label);
                spacesRequired = DASHBOARD_COL_WIDTH - sb.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(":");
            } else if (i == 1) {
                spacesRequired = DASHBOARD_LRG_COL_WIDTH + DASHBOARD_COL_WIDTH - sb.length() - label.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(label).append(" ");
            } else {
                spacesRequired = DASHBOARD_COL_WIDTH - label.length() - 2;
                sb.append("| ").append(getSpaces(spacesRequired)).append(label);
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getColumnHeaders(List<String> labels) {
        StringBuffer sb = new StringBuffer();
        int spacesRequired = 0;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (i == 0) {
                sb.append(label);
            } else if (i == 1) {
                spacesRequired = DASHBOARD_LRG_COL_WIDTH + DASHBOARD_COL_WIDTH - sb.length() - label.length() - 1;
                sb.append(getSpaces(spacesRequired)).append(label).append(" ");
            } else {
                spacesRequired = DASHBOARD_COL_WIDTH - label.length() - 2;
                sb.append("| ").append(getSpaces(spacesRequired)).append(label);
            }
        }
        sb.append("\n");
        sb.append(getBorder(TABLE_WIDTH));
        return sb.toString();
    }

}

