package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.DEVICE_PREFIX;
import static cloud.fogbow.common.constants.OpenNebulaConstants.DISK_TYPE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.FILE_SYSTEM_TYPE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.IMAGE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.PERSISTENT;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SIZE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = IMAGE)
public class VolumeImage extends OpenNebulaMarshallerTemplate {
    
	private String name;
    private String persistent;
    private String type;
    private String fileSystemType;
    private String diskType;
    private String devicePrefix;
    private int size;

    @XmlElement(name = NAME)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = PERSISTENT)
    public String getPersistent() {
        return persistent;
    }

    public void setPersistent(String persistent) {
        this.persistent = persistent;
    }

    @XmlElement(name = SIZE)
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @XmlElement(name = TYPE)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = FILE_SYSTEM_TYPE)
    public String getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(String fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    @XmlElement(name = DISK_TYPE)
    public String getDiskType() {
        return diskType;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }

    @XmlElement(name = DEVICE_PREFIX)
    public String getDevicePrefix() {
        return devicePrefix;
    }

    public void setDevicePrefix(String devicePrefix) {
        this.devicePrefix = devicePrefix;
    }
}
