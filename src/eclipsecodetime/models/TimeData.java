package eclipsecodetime.models;

import eclipsecodetime.util.SoftwareCoProject;

public class TimeData {
	public long timestamp = 0L;
	public long timestamp_local = 0L;
	public long editor_seconds = 0L;
	public long session_seconds = 0L;
	public long file_seconds = 0L;
	public String day = "";
	public SoftwareCoProject project = null;

    public void clone(TimeData td) {
        this.timestamp = td.timestamp;
        this.timestamp_local = td.timestamp_local;
        this.editor_seconds = td.editor_seconds;
        this.session_seconds = td.session_seconds;
        this.file_seconds = td.file_seconds;
        this.day = td.day;
        if (td.project != null) {
        	this.project = new SoftwareCoProject(td.project.name, td.project.directory);
        } else {
        	this.project = new SoftwareCoProject("Unnamed", "Untitled");
        }
    }
}
