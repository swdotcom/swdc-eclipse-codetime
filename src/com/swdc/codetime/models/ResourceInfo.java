package com.swdc.codetime.models;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ResourceInfo {

	public String identifier = "";
	public String branch = "";
	public String tag = "";
	public String email = "";
	public List<TeamMember> members = new ArrayList<TeamMember>();
	
	public ResourceInfo clone() {
        ResourceInfo info = new ResourceInfo();
        info.identifier = this.identifier;
        info.branch = this.branch;
        info.tag = this.tag;
        info.email = this.email;
        info.members = this.members;
        return info;
    }
	
	public JsonObject getJsonObject() {
		JsonObject jsonObj = new JsonObject();
		jsonObj.add("members", this.getJsonMembers());
		jsonObj.addProperty("identifier", this.identifier);
		jsonObj.addProperty("tag", this.tag);
		jsonObj.addProperty("branch", this.branch);
		return jsonObj;
	}
	
	public JsonArray getJsonMembers() {
        JsonArray jsonMembers = new JsonArray();
        for (TeamMember member : members) {
            JsonObject json = new JsonObject();
            json.addProperty("email", member.email);
            json.addProperty("name", member.name);
            jsonMembers.add(json);
        }
        return jsonMembers;
    }
}
