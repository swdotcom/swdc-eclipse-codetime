package com.swdc.codetime.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.swdc.codetime.models.CommitChangeStats;
import com.swdc.codetime.models.CommitInfo;
import com.swdc.codetime.models.ResourceInfo;
import com.swdc.codetime.models.TeamMember;

public class GitUtil {

	public static CommitChangeStats accumulateStatChanges(List<String> results, boolean committedChanges) {
		CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

		if (results != null) {
			for (String line : results) {
				line = line.trim();
				if (line.indexOf("insertion") != -1 || line.indexOf("deletion") != -1) {
					String[] parts = line.split(" ");
					// the 1st element is the number of files changed
					int fileCount = Integer.parseInt(parts[0]);
					changeStats.fileCount = fileCount;
					changeStats.commitCount = changeStats.commitCount + 1;
					for (int x = 1; x < parts.length; x++) {
						String part = parts[x];
						if (part.indexOf("insertion") != -1) {
							int insertions = Integer.parseInt(parts[x - 1]);
							changeStats.insertions = changeStats.insertions + insertions;
						} else if (part.indexOf("deletion") != -1) {
							int deletions = Integer.parseInt(parts[x - 1]);
							changeStats.deletions = changeStats.deletions + deletions;
						}
					}
				}
			}
		}

		return changeStats;
	}

	public static CommitChangeStats getChangeStats(List<String> cmdList, String projectDir, boolean committedChanges) {
		CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

		if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
			return changeStats;
		}

		/**
		 * example: -mbp-2:swdc-vscode xavierluiz$ git diff --stat
		 * lib/KpmProviderManager.ts | 22 ++++++++++++++++++++-- 1 file changed, 20
		 * insertions(+), 2 deletions(-)
		 * 
		 * for multiple files it will look like this... 7 files changed, 137
		 * insertions(+), 55 deletions(-)
		 */
		List<String> resultList = SoftwareCoUtils.getCommandResult(cmdList, projectDir);

		if (resultList == null || resultList.size() == 0) {
			// something went wrong, but don't try to parse a null or undefined str
			return changeStats;
		}

		// just look for the line with "insertions" and "deletions"
		changeStats = accumulateStatChanges(resultList, committedChanges);

		return changeStats;
	}

	public static CommitChangeStats getUncommitedChanges(String projectDir) {
		CommitChangeStats changeStats = new CommitChangeStats(false);

		if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
			return changeStats;
		}

		List<String> cmdList = new ArrayList<String>();
		cmdList.add("git");
		cmdList.add("diff");
		cmdList.add("--stat");

		return getChangeStats(cmdList, projectDir, false);
	}
	
	public static String getUsersEmail(String projectDir) {
        String[] emailCmd = { "git", "config", "user.email" };
        String email = SoftwareCoUtils.runCommand(emailCmd, projectDir);
        return email;
    }
	
	public static ResourceInfo getResourceInfo(String projectDir) {
		ResourceInfo resourceInfo = new ResourceInfo();

		// is the project dir avail?
		if (projectDir != null && !projectDir.equals("") && SoftwareCoUtils.isGitProject(projectDir)) {
			try {
				String[] branchCmd = { "git", "symbolic-ref", "--short", "HEAD" };
				String branch = SoftwareCoUtils.runCommand(branchCmd, projectDir);
				if (branch != null) {
					resourceInfo.branch = branch;
				}

				String[] identifierCmd = { "git", "config", "--get", "remote.origin.url" };
				String identifier = SoftwareCoUtils.runCommand(identifierCmd, projectDir);
				if (identifier != null) {
					resourceInfo.identifier = identifier;
				}

				String[] emailCmd = { "git", "config", "user.email" };
				String email = SoftwareCoUtils.runCommand(emailCmd, projectDir);
				if (email != null) {
					resourceInfo.email = email;
				}

				String[] tagCmd = { "git", "describe", "--all" };
				String tag = SoftwareCoUtils.runCommand(tagCmd, projectDir);
				if (tag != null) {
					resourceInfo.tag = tag;
				}
				
				String[] membersCmd = { "git", "log", "--pretty=%an,%ae" };
				String devOutput = SoftwareCoUtils.runCommand(membersCmd, projectDir);

				// String[] devList = devOutput.replace(/\r\n/g, "\r").replace(/\n/g,
				// "\r").split(/\r/);
				String[] devList = devOutput.split("\n");
				List<TeamMember> members = new ArrayList<>();
				Map<String, String> memberMap = new HashMap<>();
				if (devList != null && devList.length > 0) {
					for (String line : devList) {
						String[] parts = line.split(",");
						if (parts != null && parts.length > 1) {
							String name = parts[0].trim();
							String memberEmail = parts[1].trim();
							if (!memberMap.containsKey(email)) {
								memberMap.put(email, memberEmail);
								TeamMember member = new TeamMember();
								member.email = memberEmail;
								member.name = name;
								member.identifier = identifier;
								members.add(member);
							}
						}
					}
				}
				resourceInfo.members.addAll(members);
			} catch (Exception e) {
				//
			}
		}

		return resourceInfo;
	}
	
	public static CommitChangeStats getTodaysCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("today", projectDir, email);
    }

    public static CommitChangeStats getYesterdaysCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("yesterday", projectDir, email);
    }

    public static CommitChangeStats getThisWeeksCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("thisWeek", projectDir, email);
    }

    public static CommitChangeStats getCommitsForRange(String rangeType, String projectDir, String email) {
    	if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return new CommitChangeStats(true);
        }
    	
    	SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
        long startOfRange = 0l;
        if (rangeType == "today") {
            startOfRange = timesData.local_start_day;
        } else if (rangeType == "yesterday") {
            startOfRange = timesData.local_start_yesterday;
        } else if (rangeType == "thisWeek") {
            startOfRange = timesData.local_start_of_week;
        }

        String authorArg = "";
        if (email == null || email.equals("")) {
            ResourceInfo resourceInfo = getResourceInfo(projectDir);
            if (resourceInfo != null && resourceInfo.email != null && !resourceInfo.email.isEmpty()) {
                authorArg = "--author=" + resourceInfo.email;
            }
        } else {
            authorArg = "--author=" + email;
        }

        // set the until to now
        String untilArg = "--until=" + timesData.local_now;

        String[] cmdList = {"git", "log", "--stat", "--pretty=COMMIT:%H,%ct,%cI,%s", "--since=" + startOfRange, untilArg, authorArg};

        return getChangeStats(Arrays.asList(cmdList), projectDir, true);
    }

    public static String getRepoUrlLink(String projectDir) {
    	if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return "";
        }
        String[] cmdList = { "git", "config", "--get", "remote.origin.url" };

        // should only be a result of 1
        List<String> resultList = SoftwareCoUtils.getCommandResult(Arrays.asList(cmdList), projectDir);
        String url = resultList != null && resultList.size() > 0 ? resultList.get(0) : null;
        if (url != null) {
            url = url.substring(0, url.lastIndexOf(".git"));
        }
        return url;
    }

    public static CommitInfo getLastCommitInfo(String projectDir, String email) {
        if (projectDir == null || !SoftwareCoUtils.isGitProject(projectDir)) {
            return null;
        }
        if (email == null) {
            ResourceInfo resourceInfo = getResourceInfo(projectDir);
            email = resourceInfo != null ? resourceInfo.email : null;
        }
        CommitInfo commitInfo = new CommitInfo();

        String authorArg = (email != null) ? "--author=" + email : "";

        String[] cmdList = { "git", "log", "--pretty=%H,%s", authorArg, "--max-count=1" };

        // should only be a result of 1
        List<String> resultList = SoftwareCoUtils.getCommandResult(Arrays.asList(cmdList), projectDir);
        if (resultList != null && resultList.size() > 0) {
            String[] parts = resultList.get(0).split(",");
            if (parts != null && parts.length == 2) {
                commitInfo.setCommitId(parts[0]);
                commitInfo.setComment(parts[1]);
                commitInfo.setEmail(email);
            }
        }

        return commitInfo;
    }
}
