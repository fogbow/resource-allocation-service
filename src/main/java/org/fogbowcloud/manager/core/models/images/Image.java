package org.fogbowcloud.manager.core.models.images;

public class Image {

    private String id;
    private String name;
    private int size; // in bytes
    private int minDisk; // in GB
    private int minRam; // in MB
    private String status;

    public Image(String id, String name, int size, int minDisk, int minRam, String status) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.minDisk = minDisk;
        this.minRam = minRam;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return this.name;
    }

    public int getSize() {
        return this.size;
    }

    public int getMinDisk() {
        return this.minDisk;
    }

    public int getMinRam() {
        return this.minRam;
    }

    public String getStatus() {
        return status;
    }
}
