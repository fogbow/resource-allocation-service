package org.fogbowcloud.manager.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {

    public static Map<String, String> toMap(String jsonStr) {
        Map<String, String> newMap = new HashMap<String, String>();
        jsonStr = jsonStr.replace("{", "").replace("}", "");
        String[] blocks = jsonStr.split(",");
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i];
            int indexOfCarac = block.indexOf("=");
            if (indexOfCarac < 0) {
                continue;
            }
            String key = block.substring(0, indexOfCarac).trim();
            String value = block.substring(indexOfCarac + 1, block.length()).trim();
            newMap.put(key, value);
        }
        return newMap;
    }
    
	public static Object getValue(String JsonStr, String... nestedAttributes) {
		JSONObject jsonObject = new JSONObject(JsonStr);
		return getValue(jsonObject, nestedAttributes);
	}
	
	public static Object getValue(JSONObject jsonObject, String... nestedAttributes) {
		int attributesLenght = nestedAttributes.length;
		if (attributesLenght == 0) {
			return null;
		} else {
			for (int i = 0; i < attributesLenght - 1; i++) {
				String key = nestedAttributes[i];
				if (jsonObject.has(key)) {
					jsonObject = jsonObject.getJSONObject(key);
				} else {
					throw new JSONException("Could not find " + key + " in" + jsonObject);
				}
			}
			String lastKey = nestedAttributes[attributesLenght - 1];
			if (jsonObject.has(lastKey)) {
				return jsonObject.get(lastKey);
			} else {
				throw new JSONException("Could not find " + lastKey + " in" + jsonObject);
			}
		}
	}
}
