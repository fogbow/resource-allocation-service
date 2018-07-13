package org.fogbowcloud.manager.core.plugins.cloud;

import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

public class JSONMatcher extends ArgumentMatcher<JSONObject>{
	
	private JSONObject jsonObject;
	
	public JSONMatcher(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}
	
	@Override
	public boolean matches(Object argument) {
		return jsonObject.equals(argument.toString());
	}

}
