package org.fogbowcloud.manager.core;

public class HomeDir {

    public static String getPath() {
        return Thread.currentThread().getContextClassLoader().getResource("").getPath();
    }

}
