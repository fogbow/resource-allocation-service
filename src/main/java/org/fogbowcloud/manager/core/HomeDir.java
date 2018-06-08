package org.fogbowcloud.manager.core;

public class HomeDir {
    private static HomeDir instance;

    private String path;

    public static HomeDir getInstance() {
        synchronized (HomeDir.class) {
            if (instance == null) {
                instance = new HomeDir();
            }
            return instance;
        }
    }

    private HomeDir() {
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
