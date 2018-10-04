package org.fogbowcloud.ras.util;

import com.google.gson.Gson;

public class GsonHolder {
    private static Gson gson;

    private GsonHolder() {
    }

    public static synchronized Gson getInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
}
