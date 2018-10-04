package org.fogbowcloud.ras.core.models.images;

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

    public Image() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return this.name;
    }

    public String getStatus() {
        return status;
    }

    public long getSize() {
        return size;
    }

    public long getMinDisk() {
        return minDisk;
    }

    public long getMinRam() {
        return minRam;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (int) (minDisk ^ (minDisk >>> 32));
        result = prime * result + (int) (minRam ^ (minRam >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Image other = (Image) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (minDisk != other.minDisk)
            return false;
        if (minRam != other.minRam)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (size != other.size)
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }
}
