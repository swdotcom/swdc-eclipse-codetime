/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swdc.codetime.Activator;
import com.swdc.codetime.managers.FileManager;
import com.swdc.codetime.managers.WallClockManager;

public class SoftwareCoUtils {

	private static int DASHBOARD_LABEL_WIDTH = 25;
	private static int DASHBOARD_VALUE_WIDTH = 25;

	public static final Logger LOG = Logger.getLogger("SoftwareCoUtils");
	// set the api endpoint to use
	// "http://localhost:5000", "https://qaapi.software.com",
	// "https://stagingapi.software.com", "https://api.software.com"

	private final static String PROD_API_ENDPOINT = "https://api.software.com";
	// set the launch url to use
	// "http://localhost:3000", "https://qa.software.com",
	// "https://staging.software.com", "https://app.software.com"

	private final static String PROD_URL_ENDPOINT = "https://app.software.com";

	// set the api endpoint to use
	public final static String api_endpoint = PROD_API_ENDPOINT;
	// set the launch url to use
	public final static String launch_url = PROD_URL_ENDPOINT;
	public final static String webui_login_url = PROD_URL_ENDPOINT + "/login";

	public static JsonParser jsonParser = new JsonParser();

	private final static int EOF = -1;

	// sublime = 1, vs code = 2, eclipse = 3, intellij = 4, visual studio = 6, atom
	// = 7
	public final static int pluginId = 3;

	private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
	
	private static long DAYS_IN_SECONDS = 60 * 60 * 24;
	
	public static String lastOpenFile = "";

	public static ExecutorService executorService;
	public static HttpClient pingClient;
	public static HttpClient httpClient;

	private static boolean appAvailable = true;
	private static boolean showStatusText = true;
	private static String lastMsg = "";

	static {
		// initialize the HttpClient
		RequestConfig config = RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000)
				.setSocketTimeout(5000).build();

		pingClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		httpClient = HttpClientBuilder.create().build();

		executorService = Executors.newCachedThreadPool();
	}

	public static boolean isLoggedIn() {
		String name = FileManager.getItem("name");
		return name != null && !name.isEmpty() ? true : false;
	}

	public static class UserStatus {
		public boolean loggedIn;
	}

	public static boolean isAppAvailable() {
		return appAvailable;
	}

	public static void updateServerStatus(boolean isOnlineStatus) {
		appAvailable = isOnlineStatus;
	}

	public static String getVersion() {
		String version = Platform.getBundle(SWCorePlugin.ID).getVersion().toString();
		return version;
	}

	public static String getHostname() {
		List<String> cmd = new ArrayList<String>();
		cmd.add("hostname");
		String hostname = getSingleLineResult(cmd, 1);
		return hostname;
	}

	public static String getUserHomeDir() {
		return System.getProperty("user.home");
	}

	public static String getOs() {
		String osInfo = "";
		try {
			String osName = System.getProperty("os.name");
			String osVersion = System.getProperty("os.version");
			String osArch = System.getProperty("os.arch");

			if (osArch != null) {
				osInfo += osArch;
			}
			if (osInfo.length() > 0) {
				osInfo += "_";
			}
			if (osVersion != null) {
				osInfo += osVersion;
			}
			if (osInfo.length() > 0) {
				osInfo += "_";
			}
			if (osName != null) {
				osInfo += osName;
			}
		} catch (Exception e) {
			//
		}

		return osInfo;
	}

	public static boolean isWindows() {
		return (SWT.getPlatform().startsWith("win"));
	}

	public static boolean isLinux() {
		return (SoftwareCoUtils.isMac() || SoftwareCoUtils.isWindows()) ? false : true;
	}

	public static boolean isMac() {
		return (SWT.getPlatform().equals("carbon") || SWT.getPlatform().equals("cocoa"));
	}

	public static class HttpResponseInfo {
		public boolean isOk;
		public String jsonStr;
		public JsonObject jsonObj;
	}

	/**
	 * Return the http response info data
	 * 
	 * @param response
	 * @return
	 */
	public static HttpResponseInfo getResponseInfo(HttpResponse response) {
		HttpResponseInfo responseInfo = new HttpResponseInfo();
		try {
			// get the entity json string
			// (consume the entity so there's no connection leak causing a connection pool
			// timeout)
			if (response != null) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String jsonStr = getStringRepresentation(response.getEntity());
					if (jsonStr != null) {
						Object jsonEl = null;
						try {
							jsonEl = Activator.jsonParser.parse(jsonStr);
						} catch (Exception e) {
							LOG.warning("Code Time: Error trying to read and parse: " + e.getMessage());
						}
						if (jsonEl != null && jsonEl instanceof JsonElement) {
							JsonObject jsonObj = ((JsonElement) jsonEl).getAsJsonObject();
							responseInfo.jsonObj = jsonObj;
						}
						responseInfo.jsonStr = jsonStr;
					}
					responseInfo.isOk = isOk(response);
				} else if (response.getStatusLine().getStatusCode() < 300) {
					responseInfo.isOk = true;
				} else {
					responseInfo.isOk = false;
				}
			} else {
				responseInfo.isOk = false;
			}
		} catch (Exception e) {
			SWCoreLog.logErrorMessage("Unable to get http response info.");
		}
		return responseInfo;
	}
	
	public static SoftwareCoProject getActiveKeystrokeProject() {
		IProject project = getActiveProject();
		if (project != null) {
			// build the keystroke project
			IPath locationPath = project.getLocation();
			String pathStr = locationPath.toString();
			String name = project.getName();
			return new SoftwareCoProject(name, pathStr);
		}
		return null;
	}

	public static IProject getActiveProject() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null && projects.length > 0) {
			if (lastOpenFile != null && !lastOpenFile.isEmpty()) {
				for (IProject proj : projects) {
					IPath locationPath = proj.getLocation();
					String pathStr = locationPath.toString();
					if (lastOpenFile.indexOf(pathStr) != -1) {
						return proj;
					}
				}
			}
			// not found, just return the 1st proj
			return projects[0];
		}
		return null;
	}

	public static IProject getFileProject(String fileName) {
		if (fileName == null) {
			return null;
		}
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			IPath locationPath = project.getLocation();
			String pathStr = locationPath.toString();
			if (pathStr != null && fileName.indexOf(pathStr) != -1) {
				return project;
			}
		}
		return null;
	}

	private static String getStringRepresentation(HttpEntity res) throws IOException {
		if (res == null) {
			return null;
		}

		ContentType contentType = ContentType.getOrDefault(res);
		String mimeType = contentType.getMimeType();
		boolean isPlainText = (mimeType.indexOf("text/plain") == -1) ? false : true;

		InputStream inputStream = res.getContent();

		// Timing information--- verified that the data is still streaming
		// when we are called (this interval is about 2s for a large response.)
		// So in theory we should be able to do somewhat better by interleaving
		// parsing and reading, but experiments didn't show any improvement.

		StringBuffer sb = new StringBuffer();
		InputStreamReader reader;
		reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));

		BufferedReader br = new BufferedReader(reader);
		boolean done = false;
		while (!done) {
			String aLine = br.readLine();
			if (aLine != null) {
				sb.append(aLine);
				if (isPlainText) {
					sb.append("\n");
				}
			} else {
				done = true;
			}
		}
		br.close();

		return sb.toString();
	}

	private static boolean isOk(HttpResponse response) {
		if (response == null || response.getStatusLine() == null || response.getStatusLine().getStatusCode() != 200) {
			return false;
		}
		return true;
	}

	public static String humanizeLongNumbers(long number) {
		NumberFormat nf = NumberFormat.getInstance();
		return nf.format(number);
	}

	public static String humanizeMinutes(long minutes) {
		String minutesStr = "";
		if (minutes == 60) {
			minutesStr = "1 hr";
		} else if (minutes > 60) {
			float hours = (float) minutes / 60;
			if (hours == 1) {
				minutesStr = "1 hr";
			} else {
				try {
					minutesStr = String.format("%.1f", hours) + " hrs";
				} catch (Exception e) {
					minutesStr = String.valueOf(Math.round(hours)) + " hrs";
				}
			}
		} else if (minutes == 1) {
			minutesStr = "1 min";
		} else {
			minutesStr = minutes + " min";
		}
		return minutesStr;
	}

	public static boolean showingStatusText() {
		return showStatusText;
	}

	public static void submitFeedback() {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
					.openURL(new URL("mailto:cody@software.com"));
		} catch (Exception e) {
			SWCoreLog.logException(e);
		}
	}

	public static void toggleStatusBarText() {
		showStatusText = !showStatusText;
		WallClockManager.getInstance().dispatchStatusViewUpdate();
	}

	public static void setStatusLineMessage(final String statusMsg, final String iconName, final String tooltip) {
		String statusTooltip = tooltip;
		String name = FileManager.getItem("name");

		if (showStatusText) {
			lastMsg = statusMsg;
		}

		if (statusTooltip == null) {
			statusTooltip = "Active code time today. Click to see more from Code Time.";
		}

		if (statusTooltip.lastIndexOf(".") != statusTooltip.length() - 1) {
			statusTooltip += ".";
		}

		if (name != null) {
			statusTooltip += " Logged in as " + name;
		}

		final String finalTooltip = statusTooltip;

		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (!workbench.getDisplay().isDisposed())
			workbench.getDisplay().asyncExec(new Runnable() {
				public void run() {
					String statusTooltip = finalTooltip;
					if (showStatusText) {
						SWCoreStatusBar.get().setText(statusMsg);
						SWCoreStatusBar.get().setIconName(iconName);
					} else {
						statusTooltip = lastMsg + " | " + tooltip;
						SWCoreStatusBar.get().setText("");
						SWCoreStatusBar.get().setIconName("clock.png");
					}

					SWCoreStatusBar.get().setTooltip(statusTooltip);
					SWCoreStatusBar.get().update();
				}
			});
	}

	protected static boolean isSpotifyRunning() {
		String[] args = { "osascript", "-e", "get running of application \"Spotify\"" };
		String result = runCommand(args, null);
		return (result != null) ? Boolean.valueOf(result) : false;
	}

	protected static String spotifyTrackScript = "tell application \"Spotify\"\n"
			+ "set track_artist to artist of current track\n" + "set track_album to album of current track\n"
			+ "set track_name to name of current track\n" + "set track_duration to duration of current track\n"
			+ "set track_id to id of current track\n" + "set track_state to player state\n"
			+ "set json to \"type='spotify';album='\" & track_album & \"';genre='';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='\" & track_state & \"';duration='\" & track_duration & \"'\"\n"
			+ "end tell\n" + "return json\n";

	protected static String getSpotifyTrack() {
		String[] args = { "osascript", "-e", spotifyTrackScript };
		return runCommand(args, null);
	}

	protected static boolean isItunesRunning() {
		// get running of application "iTunes"
		String[] args = { "osascript", "-e", "get running of application \"iTunes\"" };
		String result = runCommand(args, null);
		return (result != null) ? Boolean.valueOf(result) : false;
	}

	protected static String itunesTrackScript = "tell application \"iTunes\"\n"
			+ "set track_artist to artist of current track\n" + "set track_album to album of current track\n"
			+ "set track_name to name of current track\n" + "set track_duration to duration of current track\n"
			+ "set track_id to id of current track\n" + "set track_genre to genre of current track\n"
			+ "set track_state to player state\n"
			+ "set json to \"type='itunes';album='\" & track_album & \"';genre='\" & track_genre & \"';artist='\" & track_artist & \"';id='\" & track_id & \"';name='\" & track_name & \"';state='\" & track_state & \"';duration='\" & track_duration & \"'\"\n"
			+ "end tell\n" + "return json\n";

	protected static String getItunesTrack() {
		String[] args = { "osascript", "-e", itunesTrackScript };
		return runCommand(args, null);
	}

	public static JsonObject getCurrentMusicTrack() {
		// genre:Alternative, artist:AWOLNATION, id:6761, name:Kill Your Heroes,
		// state:playing
		JsonObject jsonObj = new JsonObject();
		if (!isMac()) {
			return jsonObj;
		}
		boolean spotifyRunning = isSpotifyRunning();
		boolean itunesRunning = isItunesRunning();

		String trackInfo = "";
		// Vintage Trouble, My Whole World Stopped Without You,
		// spotify:track:7awBL5Pu8LD6Fl7iTrJotx, My Whole World Stopped Without You,
		// 244080
		if (spotifyRunning) {
			trackInfo = getSpotifyTrack();
		} else if (itunesRunning) {
			trackInfo = getItunesTrack();
		}

		if (trackInfo != null && !trackInfo.equals("")) {
			// trim and replace things
			trackInfo = trackInfo.trim();
			trackInfo = trackInfo.replace("\"", "");
			trackInfo = trackInfo.replace("'", "");
			String[] paramParts = trackInfo.split(";");
			for (String paramPart : paramParts) {
				paramPart = paramPart.trim();
				String[] params = paramPart.split("=");
				if (params != null && params.length == 2) {
					jsonObj.addProperty(params[0], params[1]);
				}
			}

		}
		return jsonObj;
	}

	/**
	 * Execute the args
	 * 
	 * @param args
	 * @return
	 */
	public static String runCommand(String[] args, String dir) {
		// use process builder as it allows to run the command from a specified dir
		ProcessBuilder builder = new ProcessBuilder();

		try {
			builder.command(args);
			if (dir != null) {
				// change to the directory to run the command
				builder.directory(new File(dir));
			}
			Process process = builder.start();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			InputStream is = process.getInputStream();
			copyLarge(is, baos, new byte[4096]);
			return baos.toString().trim();
		} catch (Exception e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public static List<String> getCommandResult(List<String> cmdList, String dir) {
		String[] args = Arrays.copyOf(cmdList.toArray(), cmdList.size(), String[].class);
		List<String> results = new ArrayList<>();
		String result = runCommand(args, dir);
		if (result == null || result.trim().length() == 0) {
			return results;
		}
		String[] contentList = result.split("\n");
		results = Arrays.asList(contentList);
		return results;
	}

	private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
		long count = 0;
		int n;
		while (EOF != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static String generateToken() {
		String uuid = UUID.randomUUID().toString();
		return uuid.replace("-", "");
	}

	public static boolean isDeactivated() {
		String tokenVal = FileManager.getItem("token");
		if (tokenVal == null) {
			return false;
		}

		SoftwareResponse resp = makeApiCall("/users/ping/", HttpGet.METHOD_NAME, null);
		if (!resp.isOk() && resp.isDeactivated()) {
			// update the status bar with Sign Up message
			SoftwareCoUtils.setStatusLineMessage("️Code Time", "paw.png",
					"To see your coding data in Code Time, please reactivate your account.");

		}
		return resp.isDeactivated();
	}

	public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload) {
		return makeApiCall(api, httpMethodName, payload, null);
	}

	public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload,
			String overridingJwt) {

		SoftwareResponse softwareResponse = new SoftwareResponse();
		if (!Activator.SEND_TELEMTRY.get()) {
			softwareResponse.setIsOk(true);
			return softwareResponse;
		}

		SoftwareHttpManager httpTask = null;
		if (api.contains("/ping") || api.contains("/sessions") || api.contains("/dashboard")
				|| api.contains("/users/plugin/accounts")) {
			// if the server is having issues, we'll timeout within 3 seconds for these
			// calls
			httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
		} else {
			if (httpMethodName == HttpPost.METHOD_NAME) {
				// continue, POSTS encapsulated in invoke laters with a timeout of 3 seconds
				httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
			} else {
				if (!appAvailable) {
					// bail out
					softwareResponse.setIsOk(false);
					return softwareResponse;
				}
				httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, httpClient);
			}
		}

		Future<HttpResponse> response = EXECUTOR_SERVICE.submit(httpTask);

		//
		// Handle the Future if it exist
		//
		if (response != null) {
			try {
				HttpResponse httpResponse = response.get();
				if (httpResponse != null) {
					int statusCode = httpResponse.getStatusLine().getStatusCode();
					if (statusCode < 300) {
						softwareResponse.setIsOk(true);
					}
					HttpEntity entity = httpResponse.getEntity();
					ContentType contentType = ContentType.getOrDefault(entity);
					String mimeType = contentType.getMimeType();
					JsonObject jsonObj = null;
					if (entity != null) {
						try {
							String jsonStr = getStringRepresentation(entity);
							softwareResponse.setJsonStr(jsonStr);
							// LOG.log(Level.INFO, "Code Time: API response {0}", jsonStr);
							if (jsonStr != null && mimeType.indexOf("text/plain") == -1) {
								LOG.log(Level.INFO, "Code Time: API response {0}", jsonStr);
								Object jsonEl = null;
								try {
									jsonEl = jsonParser.parse(jsonStr);
								} catch (Exception e) {
									LOG.log(Level.INFO, "Code Time: error {0}", e.getMessage());
								}

								if (jsonEl != null && jsonEl instanceof JsonElement) {
									try {
										JsonElement el = (JsonElement) jsonEl;
										if (el.isJsonPrimitive()) {
											if (statusCode < 300) {
												softwareResponse.setDataMessage(el.getAsString());
											} else {
												softwareResponse.setErrorMessage(el.getAsString());
											}
										} else {
											jsonObj = ((JsonElement) jsonEl).getAsJsonObject();
											softwareResponse.setJsonObj(jsonObj);
										}
									} catch (Exception e) {
										LOG.log(Level.WARNING, "Unable to parse response data: {0}", e.getMessage());
									}
								}
							}
						} catch (IOException e) {
							String errorMessage = "Code Time: Unable to get the response from the http request, error: "
									+ e.getMessage();
							softwareResponse.setErrorMessage(errorMessage);
							LOG.log(Level.WARNING, errorMessage);
						}
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				String errorMessage = "Code Time: Unable to get the response from the http request, error: "
						+ e.getMessage();
				softwareResponse.setErrorMessage(errorMessage);
				LOG.log(Level.WARNING, errorMessage);
			}
		}

		return softwareResponse;
	}

	private static String getSingleLineResult(List<String> cmd, int maxLen) {
		String result = null;
		String[] cmdArgs = Arrays.copyOf(cmd.toArray(), cmd.size(), String[].class);
		String content = SoftwareCoUtils.runCommand(cmdArgs, null);

		// for now just get the 1st one found
		if (content != null) {
			String[] contentList = content.split("\n");
			if (contentList != null && contentList.length > 0) {
				int len = (maxLen != -1) ? Math.min(maxLen, contentList.length) : contentList.length;
				for (int i = 0; i < len; i++) {
					String line = contentList[i];
					if (line != null && line.trim().length() > 0) {
						result = line.trim();
						break;
					}
				}
			}
		}
		return result;
	}

	public static String getOsUsername() {
		String username = System.getProperty("user.name");
		if (username == null || username.trim().equals("")) {
			try {
				List<String> cmd = new ArrayList<String>();
				if (isWindows()) {
					cmd.add("cmd");
					cmd.add("/c");
					cmd.add("whoami");
				} else {
					cmd.add("/bin/sh");
					cmd.add("-c");
					cmd.add("whoami");
				}
				username = getSingleLineResult(cmd, 1);
			} catch (Exception e) {
				//
			}
		}
		return username;
	}

	public static String getAppJwt(boolean serverIsOnline) {
		if (serverIsOnline) {
			long now = Math.round(System.currentTimeMillis() / 1000);
			String api = "/data/apptoken?token=" + now;
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null);
			if (resp.isOk()) {
				JsonObject obj = resp.getJsonObj();
				return obj.get("jwt").getAsString();
			}
		}
		return null;
	}

	private static String regex = "^\\S+@\\S+\\.\\S+$";
	private static Pattern pattern = Pattern.compile(regex);

	private static boolean validateEmail(String email) {
		return pattern.matcher(email).matches();
	}

	private static JsonObject getUser(boolean serverIsOnline) {
		String jwt = FileManager.getItem("jwt");
		if (serverIsOnline) {
			String api = "/users/me";
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
			if (resp.isOk()) {
				// check if we have the data and jwt
				// resp.data.jwt and resp.data.user
				// then update the session.json for the jwt
				JsonObject obj = resp.getJsonObj();
				if (obj != null && obj.has("data")) {
					return obj.get("data").getAsJsonObject();
				}
			}
		}
		return null;
	}

	public static String createAnonymousUser(boolean serverIsOnline) {
		// make sure we've fetched the app jwt
		String appJwt = getAppJwt(serverIsOnline);

		if (serverIsOnline && appJwt != null) {
			String timezone = TimeZone.getDefault().getID();

			JsonObject payload = new JsonObject();
			payload.addProperty("username", getOsUsername());
			payload.addProperty("timezone", timezone);
			payload.addProperty("hostname", getHostname());
			payload.addProperty("creation_annotation", "NO_SESSION_FILE");

			String api = "/data/onboard";
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), appJwt);
			if (resp.isOk()) {
				JsonObject data = resp.getJsonObj();
				// check if we have any data
				if (data != null && data.has("jwt")) {
					String dataJwt = data.get("jwt").getAsString();
					FileManager.setItem("jwt", dataJwt);
					return dataJwt;
				}
			}
		}
		return null;
	}

	private static boolean isLoggedOn(boolean serverIsOnline) {
		String jwt = FileManager.getItem("jwt");
		if (serverIsOnline) {
			JsonObject userObj = getUser(serverIsOnline);
			if (userObj != null && userObj.has("email")) {
				// check if the email is valid
				String email = userObj.get("email").getAsString();
				if (validateEmail(email)) {
					FileManager.setItem("jwt", userObj.get("plugin_jwt").getAsString());
					FileManager.setItem("name", email);
					return true;
				}
			}
			String api = "/users/plugin/state";
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
			if (resp.isOk()) {
				JsonObject data = resp.getJsonObj();
				// check if we have any data
				String state = (data != null && data.has("state")) ? data.get("state").getAsString() : "UNKNOWN";
				if (state.equals("OK")) {
					String dataJwt = data.get("jwt").getAsString();
					FileManager.setItem("jwt", dataJwt);
					String dataEmail = data.get("email").getAsString();
					if (dataEmail != null) {
						FileManager.setItem("name", dataEmail);
					}
					return true;
				} else if (state.equals("NOT_FOUND")) {
					FileManager.setItem("jwt", null);
				}
			}
		}
		FileManager.setItem("name", null);
		return false;
	}

	public static synchronized UserStatus getUserStatus() {

		boolean serverIsOnline = SoftwareCoSessionManager.isServerOnline();

		boolean loggedIn = isLoggedOn(serverIsOnline);

		UserStatus currentUserStatus = new UserStatus();
		currentUserStatus.loggedIn = loggedIn;

		return currentUserStatus;
	}

	public static void sendHeartbeat(String reason) {
		boolean serverIsOnline = SoftwareCoSessionManager.isServerOnline();
		String jwt = FileManager.getItem("jwt");
		if (serverIsOnline && jwt != null) {

			long start = Math.round(System.currentTimeMillis() / 1000);

			JsonObject payload = new JsonObject();
			payload.addProperty("pluginId", pluginId);
			payload.addProperty("os", getOs());
			payload.addProperty("start", start);
			payload.addProperty("version", getVersion());
			payload.addProperty("hostname", getHostname());
			payload.addProperty("trigger_annotation", reason);

			String api = "/data/heartbeat";
			SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), jwt);
			if (!resp.isOk()) {
				LOG.log(Level.WARNING, "Code Time: unable to send heartbeat ping");
			} else {
				LOG.log(Level.INFO, "Code Time: sent heartbeat");
			}
		}
	}

	public static Date atStartOfWeek(long local_now) {
        // find out how many days to go back
        int daysBack = 0;
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            while (dayOfWeek != Calendar.SUNDAY) {
                daysBack++;
                dayOfWeek -= 1;
            }
        } else {
            daysBack = 7;
        }

        long startOfDayInSec = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        long startOfWeekInSec = startOfDayInSec - (DAYS_IN_SECONDS * daysBack);

        return new Date(startOfWeekInSec * 1000);
    }

    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // the timestamps are all in seconds
    public static class TimesData {
        public Integer offset = ZonedDateTime.now().getOffset().getTotalSeconds();
        public long now = System.currentTimeMillis() / 1000;
        public long local_now = now + offset;
        public String timezone = TimeZone.getDefault().getID();
        public long local_start_day = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        public long local_start_yesterday = local_start_day - DAYS_IN_SECONDS;
        public Date local_start_of_week_date = atStartOfWeek(local_now);
        public Date local_start_of_yesterday_date = new Date(local_start_yesterday * 1000);
        public Date local_start_today_date = new Date(local_start_day * 1000);
        public long local_start_of_week = local_start_of_week_date.toInstant().getEpochSecond();
        public long local_end_day = atEndOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        public long utc_end_day = atEndOfDay(new Date(now * 1000)).toInstant().getEpochSecond();

    }

	public static TimesData getTimesData() {
		TimesData timesData = new TimesData();
		return timesData;
	}

	public static String getTodayInStandardFormat() {
		SoftwareCoUtils.TimesData timesData = SoftwareCoUtils.getTimesData();
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
		String day = dateFormat.format(new Date(timesData.now * 1000));
		return day;
	}

	public static String getDashboardRow(String label, String value) {
		String content = getDashboardLabel(label) + " : " + getDashboardValue(value) + "\n";
		return content;
	}

	public static String getSectionHeader(String label) {
		String content = label + "\n";
		// add 3 to account for the " : " between the columns
		int dashLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH + 15;
		for (int i = 0; i < dashLen; i++) {
			content += "-";
		}
		content += "\n";
		return content;
	}

	public static String getDashboardLabel(String label) {
		return getDashboardDataDisplay(DASHBOARD_LABEL_WIDTH, label);
	}

	public static String getDashboardValue(String value) {
		String valueContent = getDashboardDataDisplay(DASHBOARD_VALUE_WIDTH, value);
		String paddedContent = "";
		for (int i = 0; i < 11; i++) {
			paddedContent += " ";
		}
		paddedContent += valueContent;
		return paddedContent;
	}

	public static String getDashboardDataDisplay(int widthLen, String data) {
		int len = widthLen - data.length();
		String content = "";
		for (int i = 0; i < len; i++) {
			content += " ";
		}
		return content + "" + data;
	}

}
