package com.swdc.codetime.webview;

import com.google.gson.JsonObject;

public class WebviewMessage {
	// {"cmd":"registerAccount","payload":{}}
	public String cmd = "";
	public String action = "";
	public long id = 0;
	public JsonObject payload = new JsonObject();
}
