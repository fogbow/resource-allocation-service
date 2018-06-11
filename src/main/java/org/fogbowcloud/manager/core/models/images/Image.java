package org.fogbowcloud.manager.core.models.images;

public class Image {

    private String id;
    private String name;
    private long size; // in bytes
    private long minDisk; // in GB
    private long minRam; // in MB
    private String status;

    public Image(String id, String name, long size, long minDisk, long minRam, String status) {
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

    public long getSize() {
        return this.size;
    }

    public long getMinDisk() {
        return this.minDisk;
    }

    public long getMinRam() {
        return this.minRam;
    }

    public String getStatus() {
        return status;
    }
}
