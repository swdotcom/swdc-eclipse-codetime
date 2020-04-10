/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.time.ZonedDateTime;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.FileManager;

public class SoftwareCoMusicManager {
	
	private static SoftwareCoMusicManager instance = null;
	
	private JsonObject currentTrack = new JsonObject();
	
	private MusicProcessor musicProcessor = null;
	
	public static SoftwareCoMusicManager getInstance() {
		if (instance == null) {
			instance = new SoftwareCoMusicManager();
		}
		return instance;
	}
	
	protected class MusicProcessor implements Runnable {

		@Override
		public void run() {

			//
			// get the music track json string
			//
			JsonObject trackInfo = SoftwareCoUtils.getCurrentMusicTrack();
			
			if (trackInfo == null || !trackInfo.has("id") || !trackInfo.has("name")) {
				SWCoreLog.logInfoMessage("No track info");
				return;
			}
			
			String existingTrackId = (currentTrack.has("id")) ? currentTrack.get("id").getAsString() : null;
			String trackId = (trackInfo != null && trackInfo.has("id")) ? trackInfo.get("id").getAsString() : null;
			
			if (trackId != null && trackId.indexOf("spotify") == -1 && trackId.indexOf("itunes") == -1) {
				// update it to itunes since spotify uses that in the id
				trackId = "itunes:track:" + trackId;
				trackInfo.addProperty("id", trackId);
			}
			
			boolean isSpotify = (trackId != null && trackId.indexOf("spotify") != -1) ? true : false;
            if (isSpotify) {
                // convert the duration from milliseconds to seconds
                String durationStr = trackInfo.get("duration").getAsString();
                long duration = Long.parseLong(durationStr);
                int durationInSec = Math.round(duration / 1000);
                trackInfo.addProperty("duration", durationInSec);
            }
			
			String trackState = (trackInfo.get("state").getAsString());
            boolean isPaused = (trackState.toLowerCase().equals("playing")) ? false : true;
			
			Integer offset  = ZonedDateTime.now().getOffset().getTotalSeconds();
        	long now = Math.round(System.currentTimeMillis() / 1000);
        	long local_start = now + offset;
			
			if (trackId != null) {
	        	
				if (existingTrackId != null && (!existingTrackId.equals(trackId) || isPaused)) {
	        		// update the end time on the previous track and send it as well
	        		currentTrack.addProperty("end", now);
	        		// send the post to end the previous track
	        		postTrackInfo(CodeTimeActivator.gson.toJson(currentTrack));
	        	}
	        	
	        	
	        	// if the current track doesn't have an "id" then a song has started
				if (!isPaused && (existingTrackId == null  || !existingTrackId.equals(trackId))) {
	        		
	        		// send the post to send the new track info
	        		trackInfo.addProperty("start", now);
	        		trackInfo.addProperty("local_start", local_start);
	        		
	        		postTrackInfo(CodeTimeActivator.gson.toJson(trackInfo));
	        		
	        		// update the current track
		        	cloneTrackInfoToCurrent(trackInfo);
				}

	        } else {
	        	if (existingTrackId != null) {
	        		// update the end time on the previous track and send it as well
	        		currentTrack.addProperty("end", now);
	        		// send the post to end the previous track
	        		postTrackInfo(CodeTimeActivator.gson.toJson(currentTrack));
	        	}
	        	
	        	// song has ended, clear out the current track
	        	currentTrack = new JsonObject();
	        }
			
		}
		
		private void cloneTrackInfoToCurrent(JsonObject trackInfo) {
			currentTrack = new JsonObject();
			currentTrack.addProperty("start", trackInfo.get("start").getAsLong());
			long end = (trackInfo.has("end")) ? trackInfo.get("end").getAsLong() : 0;
			currentTrack.addProperty("end", end);
			currentTrack.addProperty("local_start", trackInfo.get("local_start").getAsLong());
			JsonElement durationElement = (trackInfo.has("duration")) ? trackInfo.get("duration") : null;
			double duration = 0;
			if (durationElement != null) {
				String durationStr = durationElement.getAsString();
				duration = Double.parseDouble(durationStr);
				if (duration > 1000) {
					duration /= 1000;
				}
			}
			currentTrack.addProperty("duration", duration);
			String genre = (trackInfo.has("genre")) ? trackInfo.get("genre").getAsString() : "";
			currentTrack.addProperty("genre", genre);
			String artist = (trackInfo.has("artist")) ? trackInfo.get("artist").getAsString() : "";
			currentTrack.addProperty("artist", artist);
			currentTrack.addProperty("name", trackInfo.get("name").getAsString());
			String state = (trackInfo.has("state")) ? trackInfo.get("state").getAsString() : "";
			currentTrack.addProperty("state", state);
			currentTrack.addProperty("id", trackInfo.get("id").getAsString());
		}
		
		private HttpResponse postTrackInfo(String trackInfo) {
			HttpPost request = null;
			try {

				//
				// Add the json body to the outgoing post request
				//
				request = new HttpPost(SoftwareCoUtils.api_endpoint + "/data/music");
				String jwtToken = FileManager.getItem("jwt");
                // we need the header, but check if it's null anyway
                if (jwtToken != null) {
                    request.addHeader("Authorization", jwtToken);
                }
				StringEntity params = new StringEntity(trackInfo);
				request.addHeader("Content-type", "application/json");
				request.setEntity(params);

				//
				// Send the POST request
				//
				HttpResponse response = SoftwareCoUtils.httpClient.execute(request);
				int responseStatus = response.getStatusLine().getStatusCode();
				
				SWCoreLog.logInfoMessage("Completed sending music info, status result: " + responseStatus);
				
				//
				// Return the response
				//
				return response;
			} catch (Exception e) {
				SWCoreLog.error("Code Time: Unable to send the keystroke payload request.", e);
			} finally {
				if (request != null) {
					request.releaseConnection();
				}
			}
			return null;
		}
	}

	
	public void processMusicTrackInfo() {
		if (musicProcessor == null) {
			musicProcessor = new MusicProcessor();
		}
		musicProcessor.run();
	}
}
