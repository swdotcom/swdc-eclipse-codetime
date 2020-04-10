/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package eclipsecodetime.util;

import eclipsecodetime.models.ResourceInfo;

public class SoftwareCoProject {

	public String name = "";
	public String directory = "";
	public String identifier = "";
	public ResourceInfo resource = new ResourceInfo();
	
	public SoftwareCoProject(String name, String directory) {
		this.name = name;
		this.directory = directory;
		
		// get the resource information
		this.resource = GitUtil.getResourceInfo(directory);
	}
	
	public SoftwareCoProject clone() {
		SoftwareCoProject p = new SoftwareCoProject(this.name, this.directory);
        p.identifier = p.identifier;
        if (this.resource != null) {
            p.resource = this.resource.clone();
        }
        return p;
    }
	
	public void resetData() {
        this.name = "";
        this.directory = "";
        this.identifier = "";
        resource = new ResourceInfo();
    }

    
	@Override
	public String toString() {
		return "SoftwareCoProject [name=" + name + ", directory=" + directory + "]";
	}
	
}
