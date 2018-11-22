package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IMAGE")
public class VolumeImageRequestTemplate extends OpenNebulaMarshallerTemplate {
    private String name;
    private String persistent;
    private String type;
    private String fstype;
    private String diskType;
    private String devPrefix;
    private int size;

    public VolumeImageRequestTemplate(String name, String persistent, String type, String fstype, String diskType, String devPrefix, int size) {
        this.name = name;
        this.persistent = persistent;
        this.type = type;
        this.fstype = fstype;
        this.diskType = diskType;
        this.devPrefix = devPrefix;
        this.size = size;
    }

    @XmlElement(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "PERSISTENT")
    public String getPersistent() {
        return persistent;
    }

    public void setPersistent(String persistent) {
        this.persistent = persistent;
    }

    @XmlElement(name = "SIZE")
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @XmlElement(name = "TYPE")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "FSTYPE")
    public String getFstype() {
        return fstype;
    }

    public void setFstype(String fstype) {
        this.fstype = fstype;
    }

    @XmlElement(name = "DISK_TYPE")
    public String getDiskType() {
        return diskType;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }

    @XmlElement(name = "DEV_PREFIX")
    public String getDevPrefix() {
        return devPrefix;
    }

    public void setDevPrefix(String devPrefix) {
        this.devPrefix = devPrefix;
    }
}