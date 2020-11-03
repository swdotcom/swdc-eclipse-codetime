/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.models.ResourceInfo;

public class SoftwareCoRepoManager {

	private static SoftwareCoRepoManager instance = null;

	public static SoftwareCoRepoManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoRepoManager();
		}
		return instance;
	}

	public JsonObject getLatestCommit(String projectDir) {
		ResourceInfo resource = GitUtil.getResourceInfo(projectDir);
		if (resource.identifier != null && !resource.identifier.equals("")) {

			try {
				String encodedIdentifier = URLEncoder.encode(resource.identifier, "UTF-8");
				String encodedTag = URLEncoder.encode(resource.tag, "UTF-8");
				String encodedBranch = URLEncoder.encode(resource.branch, "UTF-8");

				String qryString = "identifier=" + encodedIdentifier;
				qryString += "&tag=" + encodedTag;
				qryString += "&branch=" + encodedBranch;

				SoftwareResponse responseData = SoftwareCoUtils.makeApiCall("/commits/latest?" + qryString,
						HttpGet.METHOD_NAME, null);
				if (responseData != null && responseData.isOk()) {
					JsonObject payload = responseData.getJsonObj();
					// will get a single commit object back with the following attributes
					// commitId, message, changes, timestamp
					JsonObject latestCommit = payload.get("commit").getAsJsonObject();
					return latestCommit;
				} else {
					SWCoreLog.logInfoMessage("Code Time: Unable to fetch latest commit info");
				}
			} catch (Exception e) {
				//
			}

		}

		return null;
	}

	public void getHistoricalCommits(String projectDir) {
		ResourceInfo resource = GitUtil.getResourceInfo(projectDir);
		if (resource.identifier != null && !resource.identifier.equals("")) {

			JsonObject latestCommit = getLatestCommit(projectDir);

			String sinceOption = null;
			if (latestCommit != null && latestCommit.has("timestamp")) {
				long unixTs = latestCommit.get("timestamp").getAsLong();
				sinceOption = "--since=" + unixTs;
			} else {
				sinceOption = "--max-count=100";
			}

			String authorOption = "--author=" + resource.email;
			List<String> cmdList = new ArrayList<String>();
			cmdList.add("git");
			cmdList.add("log");
			cmdList.add("--stat");
			cmdList.add("--pretty=COMMIT:%H,%ct,%cI,%s");
			cmdList.add(authorOption);
			if (sinceOption != null) {
				cmdList.add(sinceOption);
			}

			// String[] commitHistoryCmd = {"git", "log", "--stat",
			// "--pretty=COMMIT:%H,%ct,%cI,%s", authorOption};
			String[] commitHistoryCmd = Arrays.copyOf(cmdList.toArray(), cmdList.size(), String[].class);
			String historyContent = SoftwareCoUtils.runCommand(commitHistoryCmd, projectDir);

			if (historyContent == null || historyContent.isEmpty()) {
				return;
			}

			String latestCommitId = (latestCommit != null && latestCommit.has("commitId"))
					? latestCommit.get("commitId").getAsString()
					: null;

			// split the content
			JsonArray commits = new JsonArray();
			JsonObject commit = null;
			String[] historyContentList = historyContent.split("\n");
			if (historyContentList != null && historyContentList.length > 0) {
				for (String line : historyContentList) {
					line = line.trim();
					if (line.indexOf("COMMIT:") == 0) {
						line = line.substring("COMMIT:".length());
						if (commit != null) {
							commits.add(commit);
						}
						// split by comma
						String[] commitInfos = line.split(",");
						if (commitInfos != null && commitInfos.length > 3) {
							String commitId = commitInfos[0].trim();
							if (latestCommitId != null && commitId.equals(latestCommitId)) {
								commit = null;
								// go to the next one
								continue;
							}
							long timestamp = Long.valueOf(commitInfos[1].trim());
							String date = commitInfos[2].trim();
							String message = commitInfos[3].trim();
							commit = new JsonObject();
							commit.addProperty("commitId", commitId);
							commit.addProperty("timestamp", timestamp);
							commit.addProperty("date", date);
							commit.addProperty("message", message);
							JsonObject sftwTotalsObj = new JsonObject();
							sftwTotalsObj.addProperty("insertions", 0);
							sftwTotalsObj.addProperty("deletions", 0);
							JsonObject changesObj = new JsonObject();
							changesObj.add("__sftwTotal__", sftwTotalsObj);
							commit.add("changes", changesObj);
						}
					} else if (commit != null && line.indexOf("|") != -1) {
						line = line.replaceAll("\\s+", " ");
						String[] lineInfos = line.split("|");

						if (lineInfos != null && lineInfos.length > 1) {
							String file = lineInfos[0].trim();
							String metricsLine = lineInfos[1].trim();
							String[] metricInfos = metricsLine.split(" ");
							if (metricInfos != null && metricInfos.length > 1) {
								String addAndDeletes = metricInfos[1].trim();
								// count the number of plus signs and negative signs to find
								// out how many additions and deletions per file
								int len = addAndDeletes.length();
								int lastPlusIdx = addAndDeletes.lastIndexOf("+");
								int insertions = 0;
								int deletions = 0;
								if (lastPlusIdx != -1) {
									insertions = lastPlusIdx + 1;
									deletions = len - insertions;
								} else if (len > 0) {
									// all deletions
									deletions = len;
								}
								JsonObject fileChanges = new JsonObject();
								fileChanges.addProperty("insertions", insertions);
								fileChanges.addProperty("deletions", deletions);
								JsonObject changesObj = commit.get("changes").getAsJsonObject();
								changesObj.add(file, fileChanges);

								JsonObject swftTotalsObj = changesObj.get("__sftwTotal__").getAsJsonObject();
								int insertionTotal = swftTotalsObj.get("insertions").getAsInt() + insertions;
								int deletionsTotal = swftTotalsObj.get("deletions").getAsInt() + deletions;
								swftTotalsObj.addProperty("insertions", insertionTotal);
								swftTotalsObj.addProperty("deletions", deletionsTotal);
							}
						}
					}
				}

				if (commit != null) {
					commits.add(commit);
				}

				if (commits != null && commits.size() > 0) {
					int batch_size = 10;
					JsonArray batch = new JsonArray();
					for (int i = 0; i < commits.size(); i++) {
						batch.add(commits.get(i));
						if (i > 0 && i % batch_size == 0) {
							this.processCommits(batch, resource.identifier, resource.tag, resource.branch);
							batch = new JsonArray();
						}
					}
					if (batch.size() > 0) {
						this.processCommits(batch, resource.identifier, resource.tag, resource.branch);
					}
				}
			}
		}
	}

	private void processCommits(JsonArray commits, String identifier, String tag, String branch) {
		try {

			// send the commits
			JsonObject commitData = new JsonObject();
			commitData.add("commits", commits);
			commitData.addProperty("identifier", identifier);
			commitData.addProperty("tag", tag);
			commitData.addProperty("branch", branch);
			String commitDataStr = commitData.toString();

			SoftwareResponse responseData = SoftwareCoUtils.makeApiCall("/commits", HttpPost.METHOD_NAME,
					commitDataStr);

			if (responseData != null && responseData.isOk()) {

				// {"status":"success","message":"Updated commits"}
				// {"status":"failed","data":"Unable to process commits data"}
				JsonObject responseObj = responseData.getJsonObj();
				String message = "";
				if (responseObj.has("data")) {
					JsonObject data = responseObj.get("data").getAsJsonObject();
					message = data.get("message").getAsString();
				} else if (responseObj.has("message")) {
					message = responseObj.get("message").getAsString();
				}

				SWCoreLog.logInfoMessage("Code Time: completed commits update - " + message);
			} else {
				SWCoreLog.logInfoMessage("Code Time: Unable to process repo commits");
			}
		} catch (Exception e) {
			SWCoreLog.logInfoMessage("Code Time: Unable to process repo commits, error: " + e.getMessage());
		}
	}
}
