package com.swdc.codetime.managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.client.methods.HttpPost;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.util.SWCoreLog;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.codetime.util.SoftwareResponse;

public class FileManager {

	public static final Logger LOG = Logger.getLogger("FileManager");

	private static JsonParser parser = new JsonParser();
	private static final Gson gson = new Gson();

	public static String getSessionSummaryFile() {
		String file = getSoftwareDir(true);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\sessionSummary.json";
		} else {
			file += "/sessionSummary.json";
		}
		return file;
	}

	public static String getSoftwareDir(boolean autoCreate) {
		String softwareDataDir = SoftwareCoUtils.getUserHomeDir();
		if (SoftwareCoUtils.isWindows()) {
			softwareDataDir += "\\.software";
		} else {
			softwareDataDir += "/.software";
		}

		File f = new File(softwareDataDir);
		if (autoCreate && !f.exists()) {
			// make the directory
			f.mkdirs();
		}

		return softwareDataDir;
	}

	public static String getCodeTimeDashboardFile() {
		String dashboardFile = getSoftwareDir(true);
		if (SoftwareCoUtils.isWindows()) {
			dashboardFile += "\\CodeTime.txt";
		} else {
			dashboardFile += "/CodeTime.txt";
		}
		return dashboardFile;
	}

	public static String getSummaryInfoFile(boolean autoCreate) {
		String file = getSoftwareDir(autoCreate);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\SummaryInfo.txt";
		} else {
			file += "/SummaryInfo.txt";
		}
		return file;
	};

	public static String getSoftwareSessionFile(boolean autoCreate) {
		String file = getSoftwareDir(autoCreate);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\session.json";
		} else {
			file += "/session.json";
		}
		return file;
	}

	public static String getSoftwareDataStoreFile() {
		String file = getSoftwareDir(true);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\data.json";
		} else {
			file += "/data.json";
		}
		return file;
	}

	private static String getDeviceFile() {
		String file = getSoftwareDir(true);
		if (SoftwareCoUtils.isWindows()) {
			file += "\\device.json";
		} else {
			file += "/device.json";
		}
		return file;
	}

	public synchronized static void writeData(String file, Object o) {
		if (o == null) {
			return;
		}
		File f = new File(file);
		final String content = CodeTimeActivator.gson.toJson(o);

		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8")));
			writer.write(content);
		} catch (IOException e) {
			LOG.warning("Code Time: Error writing content: " + e.getMessage());
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */
			}
		}
	}

	public static void appendData(String file, Object o) {
		if (o == null) {
			return;
		}
		File f = new File(file);
		String content = CodeTimeActivator.gson.toJson(o);
		if (SoftwareCoUtils.isWindows()) {
			content += "\r\n";
		} else {
			content += "\n";
		}

		try {
			Writer output;
			output = new BufferedWriter(new FileWriter(f, true)); // clears file every time
			output.append(content);
			output.close();
		} catch (Exception e) {
			LOG.warning("Code Time: Error appending content: " + e.getMessage());
		}
	}

	public static JsonObject getSessionSummaryFileAsJson() {
		return getFileContentAsJson(getSessionSummaryFile());
	}

	public static JsonArray getFileContentAsJsonArray(String file) {
		try {
			Object obj = parser.parse(new FileReader(file));
			if (obj != null) {
				JsonArray jsonArray = (JsonArray) obj;
				return jsonArray;
			} else {
				LOG.warning("Code Time: Null data for file: " + file);
			}

		} catch (Exception e) {
			LOG.warning("Code Time: Error trying to read and parse " + file + ": " + e.getMessage());
		}
		return new JsonArray();
	}

	public static JsonObject getFileContentAsJson(String file) {
		try {
			Object obj = parser.parse(new FileReader(file));
			if (obj != null) {
				JsonObject jsonObj = (JsonObject) obj;
				return jsonObj;
			}
		} catch (Exception e) {
			LOG.warning("Code Time: Error trying to read and parse " + file + ": " + e.getMessage());
		}
		return new JsonObject();
	}

	public static void deleteFile(String file) {
		File f = new File(file);
		// if the file exists, delete it
		if (f.exists()) {
			f.delete();
		}
	}

	public static void sendJsonArrayData(String file, String api) {
		File f = new File(file);
		if (f.exists()) {
			try {
				JsonArray jsonArr = FileManager.getFileContentAsJsonArray(file);
				String payloadData = CodeTimeActivator.gson.toJson(jsonArr);
				SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payloadData);
				if (!resp.isOk()) {
					// add these back to the offline file
					LOG.info("Code Time: Unable to send array data: " + resp.getErrorMessage());
				}
			} catch (Exception e) {
				LOG.info("Code Time: Unable to send array data: " + e.getMessage());
			}
		}
	}

	public static String getFileContent(String file) {
		String content = null;

		File f = new File(file);
		if (f.exists()) {
			try {
				Path p = Paths.get(file);
				byte[] encoded = Files.readAllBytes(p);
				content = new String(encoded, Charset.forName("UTF-8"));
			} catch (Exception e) {
				LOG.warning("Code Time: getFileContent - Error trying to read and parse: " + e.getMessage());
			}
		}
		return content;
	}

	public static void saveFileContent(String file, String content) {
		File f = new File(file);

		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8")));
			writer.write(content);
		} catch (IOException ex) {
			// Report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}

	public static String getItem(String key) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
			return sessionJson.get(key).getAsString();
		}
		return null;
	}

	public static boolean getBooleanItem(String key) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
			return sessionJson.get(key).getAsBoolean();
		}
		return false;
	}

	public static void setBooleanItem(String key, boolean val) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		sessionJson.addProperty(key, val);

		String content = sessionJson.toString();
		String sessionFile = getSoftwareSessionFile(true);

		saveFileContent(sessionFile, content);
	}

	public static void setNumericItem(String key, long val) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		sessionJson.addProperty(key, val);

		String content = sessionJson.toString();

		String sessionFile = getSoftwareSessionFile(true);
		saveFileContent(sessionFile, content);
	}

	public static void setItem(String key, String val) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		sessionJson.addProperty(key, val);

		String content = sessionJson.toString();
		String sessionFile = getSoftwareSessionFile(true);

		saveFileContent(sessionFile, content);

	}

	public static long getNumericItem(String key, Long defaultVal) {
		JsonObject sessionJson = getSoftwareSessionAsJson();
		if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
			return sessionJson.get(key).getAsLong();
		}
		return defaultVal.longValue();
	}

	public static synchronized JsonObject getSoftwareSessionAsJson() {

		JsonObject sessionJson = new JsonObject();
		String sessionFile = getSoftwareSessionFile(true);
		File f = new File(sessionFile);
		if (f.exists()) {
			try {
				Path p = Paths.get(sessionFile);

				byte[] encoded = Files.readAllBytes(p);
				String content = new String(encoded, Charset.defaultCharset());
				if (content != null) {
					// json parse it
					sessionJson = CodeTimeActivator.jsonParser.parse(content).getAsJsonObject();
				}

			} catch (Exception e) {
				SWCoreLog.logErrorMessage("Code Time: Error trying to read and json parse the session file.");
				SWCoreLog.logException(e);

			}
		}
		if (sessionJson == null) {
			sessionJson = new JsonObject();
		}
		return sessionJson;
	}

	public static JsonObject getJsonObjectFromFile(String fileName) {
		JsonObject jsonObject = new JsonObject();
		String content = getFileContent(fileName);

		if (content != null) {
			// json parse it
			jsonObject = readAsJsonObject(content);
		}

		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}
		return jsonObject;
	}

	public static JsonArray readAsJsonArray(String data) {
		try {
			JsonArray jsonArray = gson.fromJson(buildJsonReader(data), JsonArray.class);
			return jsonArray;
		} catch (Exception e) {
			return null;
		}
	}

	public static JsonObject readAsJsonObject(String data) {
		try {
			JsonObject jsonObject = gson.fromJson(buildJsonReader(data), JsonObject.class);
			return jsonObject;
		} catch (Exception e) {
			return null;
		}
	}

	public static JsonElement readAsJsonElement(String data) {
		try {
			JsonElement jsonElement = gson.fromJson(buildJsonReader(data), JsonElement.class);
			return jsonElement;
		} catch (Exception e) {
			return null;
		}
	}

	public static JsonReader buildJsonReader(String data) {
		// Clean the data
		data = SoftwareCoUtils.cleanJsonString(data);
		JsonReader reader = new JsonReader(new StringReader(data));
		reader.setLenient(true);
		return reader;
	}
	
	public static String getPluginUuid() {
        String plugin_uuid = null;
        JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
        if (deviceJson.has("plugin_uuid") && !deviceJson.get("plugin_uuid").isJsonNull()) {
            plugin_uuid = deviceJson.get("plugin_uuid").getAsString();
        } else {
            // set it for the 1st and only time
            plugin_uuid = UUID.randomUUID().toString();
            deviceJson.addProperty("plugin_uuid", plugin_uuid);
            String content = deviceJson.toString();
            saveFileContent(getDeviceFile(), content);
        }
        return plugin_uuid;
    }

	public static String getAuthCallbackState() {
		JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
		if (deviceJson != null && deviceJson.has("auth_callback_state")
				&& !deviceJson.get("auth_callback_state").isJsonNull()) {
			return deviceJson.get("auth_callback_state").getAsString();
		}
		return null;
	}

	public static void setAuthCallbackState(String value) {
		String deviceFile = getDeviceFile();
		JsonObject deviceJson = getJsonObjectFromFile(deviceFile);
		deviceJson.addProperty("auth_callback_state", value);

		String content = deviceJson.toString();

		saveFileContent(deviceFile, content);
	}

}
