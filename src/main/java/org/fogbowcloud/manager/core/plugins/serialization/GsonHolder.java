package org.fogbowcloud.manager.core.plugins.serialization;

import com.google.gson.Gson;

public class GsonHolder {

    private static Gson gson;

    private GsonHolder() {}

    public static synchronized Gson getInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

}
