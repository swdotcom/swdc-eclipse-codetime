package eclipsecodetime.models;

import java.util.TimeZone;

import eclipsecodetime.util.SoftwareCoUtils;

public class CodeTimeEvent {
	public String type = "";
	public String name = "";
	public long timestamp = 0L;
	public long timestamp_local = 0L;
	public String description = "";
	public int pluginId = SoftwareCoUtils.pluginId;
	public String os = SoftwareCoUtils.getOs();
	public String version = SoftwareCoUtils.getVersion();
	public String hostname = SoftwareCoUtils.getHostname();
	public String timezone = TimeZone.getDefault().getID();
}
