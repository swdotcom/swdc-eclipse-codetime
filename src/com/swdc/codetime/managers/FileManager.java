package com.swdc.codetime.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.apache.http.client.methods.HttpPost;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.models.SessionSummary;
import com.swdc.codetime.util.SWCoreLog;
import com.swdc.codetime.util.SoftwareCoUtils;
import com.swdc.codetime.util.SoftwareResponse;

public class FileManager {

	public static final Logger LOG = Logger.getLogger("FileManager");

	private static JsonObject sessionJson = null;

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

	public static void writeData(String file, Object o) {
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
			LOG.info("Code Time: Storing content: " + content);
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
		JsonParser parser = new JsonParser();
		try {
			Object obj = parser.parse(new FileReader(file));
			JsonArray jsonArray = parser.parse(obj.toString()).getAsJsonArray();
			return jsonArray;
		} catch (Exception e) {
			LOG.warning("Code Time: Error trying to read and parse " + file + ": " + e.getMessage());
		}
		return new JsonArray();
	}

	public static JsonObject getFileContentAsJson(String file) {
		JsonParser parser = new JsonParser();
		try {
			Object obj = parser.parse(new FileReader(file));
			JsonObject jsonArray = parser.parse(obj.toString()).getAsJsonObject();
			return jsonArray;
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

	public static void sendBatchData(String file, String api) {
		File f = new File(file);
		if (f.exists()) {
			// found a data file, check if there's content
			StringBuffer sb = new StringBuffer();
			try {
				FileInputStream fis = new FileInputStream(f);

				// Construct BufferedReader from InputStreamReader
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));

				String line = null;
				// add commas to the end of each line
				while ((line = br.readLine()) != null) {
					if (line.length() > 0) {
						sb.append(line).append(",");
					}
				}

				br.close();

				if (sb.length() > 0) {
					// check to see if it's already an array
					String payloads = sb.toString();
					payloads = payloads.substring(0, payloads.lastIndexOf(","));
					payloads = "[" + payloads + "]";

					JsonArray jsonArray = (JsonArray) CodeTimeActivator.jsonParser.parse(payloads);

					// delete the file
					deleteFile(file);

					JsonArray batch = new JsonArray();
					// go through the array about 50 at a time
					for (int i = 0; i < jsonArray.size(); i++) {
						batch.add(jsonArray.get(i));
						if (i > 0 && i % 50 == 0) {
							String payloadData = CodeTimeActivator.gson.toJson(batch);
							SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpPost.METHOD_NAME, payloadData);
							if (!resp.isOk()) {
								// add these back to the offline file
								LOG.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
							}
							batch = new JsonArray();
						}
					}
					if (batch.size() > 0) {
						String payloadData = CodeTimeActivator.gson.toJson(batch);
						SoftwareResponse resp = SoftwareCoUtils.makeApiCall("/data/batch", HttpPost.METHOD_NAME,
								payloadData);
						if (!resp.isOk()) {
							// add these back to the offline file
							LOG.info("Code Time: Unable to send batch data: " + resp.getErrorMessage());
						}
					}

				} else {
					LOG.info("Code Time: No offline data to send");
				}
			} catch (Exception e) {
				LOG.warning("Code Time: Error trying to read and send offline data: " + e.getMessage());
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
		sessionJson = getSoftwareSessionAsJson();
		if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
			return sessionJson.get(key).getAsString();
		}
		return null;
	}

	public static void setNumericItem(String key, long val) {
		sessionJson = getSoftwareSessionAsJson();
		sessionJson.addProperty(key, val);

		String content = sessionJson.toString();

		String sessionFile = getSoftwareSessionFile(true);
		saveFileContent(sessionFile, content);
	}

	public static void setItem(String key, String val) {
		sessionJson = getSoftwareSessionAsJson();
		sessionJson.addProperty(key, val);

		String content = sessionJson.toString();
		String sessionFile = getSoftwareSessionFile(true);

		saveFileContent(sessionFile, content);

	}

	public static long getNumericItem(String key, Long defaultVal) {
		sessionJson = getSoftwareSessionAsJson();
		if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
			return sessionJson.get(key).getAsLong();
		}
		return defaultVal.longValue();
	}

	public static synchronized JsonObject getSoftwareSessionAsJson() {
		if (sessionJson == null) {

			String sessionFile = getSoftwareSessionFile(true);
			File f = new File(sessionFile);
			if (f.exists()) {
				try {
					Path p = Paths.get(sessionFile);

					byte[] encoded = Files.readAllBytes(p);
					String content = new String(encoded, Charset.defaultCharset());
					LOG.info("getSoftwareSessionAsJson: " + content);
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
		}
		return sessionJson;
	}
}
