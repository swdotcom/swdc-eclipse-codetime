package com.swdc.codetime.managers;

import com.swdc.codetime.models.FileDetails;
import com.swdc.codetime.models.ResourceInfo;
import com.swdc.codetime.util.GitUtil;
import com.swdc.codetime.util.KeystrokePayload;
import com.swdc.codetime.util.KeystrokePayload.FileInfo;
import com.swdc.codetime.util.SoftwareCoProject;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.snowplow.tracker.entities.*;
import com.swdc.snowplow.tracker.events.CodetimeEvent;
import com.swdc.snowplow.tracker.events.EditorActionEvent;
import com.swdc.snowplow.tracker.events.UIInteractionEvent;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import com.swdc.snowplow.tracker.manager.TrackerManager;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

public class EventTrackerManager {
	public static final Logger log = Logger.getLogger("EventTrackerManager");

	private static EventTrackerManager instance = null;

	private TrackerManager trackerMgr;
	private boolean ready = false;

	public static EventTrackerManager getInstance() {
		if (instance == null) {
			synchronized(EventTrackerManager.class) {
				if (instance == null) {
					instance = new EventTrackerManager();
				}
			}
		}
		return instance;
	}

	private EventTrackerManager() {
		this.init();
	}

	private void init() {
		trackerMgr = new TrackerManager(SoftwareCoUtils.api_endpoint, "CodeTime", SoftwareCoUtils.pluginName);
		ready = true;
	}

	public void trackCodeTimeEvent(KeystrokePayload payload) {
		if (!this.ready) {
			return;
		}
		ResourceInfo resourceInfo = GitUtil.getResourceInfo(payload.getProject().directory, false);

		Map<String, FileInfo> fileInfoDataSet = payload.getFileInfos();
		for (FileInfo fileInfoData : fileInfoDataSet.values()) {
			CodetimeEvent event = new CodetimeEvent();

			event.keystrokes = fileInfoData.keystrokes;
			event.chars_added = fileInfoData.add;
			event.chars_deleted = fileInfoData.delete;
			event.pastes = fileInfoData.paste;
			event.chars_pasted = fileInfoData.charsPasted;
			event.lines_added = fileInfoData.linesAdded;
			event.lines_deleted = fileInfoData.linesRemoved;

			Date startDate = new Date(fileInfoData.start * 1000);
			event.start_time = DateTimeFormatter.ISO_INSTANT.format(startDate.toInstant());
			Date endDate = new Date(fileInfoData.end * 1000);
			event.end_time = DateTimeFormatter.ISO_INSTANT.format(endDate.toInstant());

			// set the entities
			event.fileEntity = this.getFileEntity(fileInfoData);
			event.projectEntity = this.getProjectEntity();
			event.authEntity = this.getAuthEntity();
			event.pluginEntity = this.getPluginEntity();
			event.repoEntity = this.getRepoEntity(resourceInfo);

			trackerMgr.trackCodeTimeEvent(event);
		}
	}

	public void trackUIInteraction(UIInteractionType interaction_type, UIElementEntity elementEntity) {
		if (!this.ready) {
			return;
		}

		UIInteractionEvent event = new UIInteractionEvent();
		event.interaction_type = interaction_type;

		// set the entities
		event.uiElementEntity = elementEntity;
		event.authEntity = this.getAuthEntity();
		event.pluginEntity = this.getPluginEntity();

		trackerMgr.trackUIInteraction(event);
	}

	public void trackEditorAction(String entity, String type) {
		trackEditorAction(entity, type, null);
	}

	public void trackEditorAction(String entity, String type, String full_file_name) {
		if (!this.ready) {
			return;
		}

		EditorActionEvent event = new EditorActionEvent();
		event.entity = entity;
		event.type = type;

		// set the entities
		event.authEntity = this.getAuthEntity();
		event.pluginEntity = this.getPluginEntity();
		event.projectEntity = this.getProjectEntity();
		event.fileEntity = this.getFileEntityFromFileName(full_file_name);
		ResourceInfo resourceInfo = GitUtil.getResourceInfo(event.project_directory, false);
		event.repoEntity = this.getRepoEntity(resourceInfo);

		trackerMgr.trackEditorAction(event);
	}

	private AuthEntity getAuthEntity() {
		AuthEntity authEntity = new AuthEntity();
		String jwt = FileManager.getItem("jwt");
		authEntity.setJwt(jwt != null ? jwt.split("JWT ")[1].trim() : "");
		return authEntity;
	}

	private FileEntity getFileEntityFromFileName(String fullFileName) {
		FileDetails fileDetails = SoftwareCoUtils.getFileDetails(fullFileName);
		FileEntity fileEntity = new FileEntity();
		fileEntity.character_count = fileDetails.character_count;
		fileEntity.file_name = fileDetails.file_name;
		fileEntity.file_path = fileDetails.project_file_name;
		fileEntity.line_count = fileDetails.line_count;
		fileEntity.syntax = fileDetails.syntax;
		return fileEntity;
	}

	private FileEntity getFileEntity(FileInfo fileInfo) {
		FileEntity fileEntity = new FileEntity();
		fileEntity.character_count = fileInfo.length;
		fileEntity.file_name = fileInfo.name;
		fileEntity.file_path = fileInfo.fsPath;
		fileEntity.line_count = fileInfo.lines;
		fileEntity.syntax = fileInfo.syntax;
		return fileEntity;
	}

	private ProjectEntity getProjectEntity() {
		ProjectEntity projectEntity = new ProjectEntity();
		SoftwareCoProject activeProject = SoftwareCoUtils.getActiveKeystrokeProject();
		if (activeProject != null) {
			projectEntity.project_directory = activeProject.directory;
			projectEntity.project_name = activeProject.name;
		}
		return projectEntity;
	}

	private RepoEntity getRepoEntity(ResourceInfo resourceInfo) {
		RepoEntity repoEntity = new RepoEntity();
		if (resourceInfo != null) {
			repoEntity.git_branch = resourceInfo.branch;
			repoEntity.git_tag = resourceInfo.tag;
			repoEntity.repo_identifier = resourceInfo.identifier;
			repoEntity.owner_id = resourceInfo.ownerId;
			repoEntity.repo_name = resourceInfo.repoName;
		}
		return repoEntity;
	}

	private PluginEntity getPluginEntity() {
		PluginEntity pluginEntity = new PluginEntity();
		pluginEntity.plugin_name = SoftwareCoUtils.pluginName;
		pluginEntity.plugin_version = SoftwareCoUtils.getVersion();
		pluginEntity.plugin_id = SoftwareCoUtils.pluginId;
		return pluginEntity;
	}
}
