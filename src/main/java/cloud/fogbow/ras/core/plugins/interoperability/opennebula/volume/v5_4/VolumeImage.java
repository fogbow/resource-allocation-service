package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = IMAGE)
public class VolumeImage extends OpenNebulaMarshaller {
    
	private String name;
    private String imagePersistent;
    private String imageType;
    private String driver;
    private String diskType;
    private String devicePrefix;
    private long size;

    @XmlElement(name = NAME)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = PERSISTENT)
    public String getImagePersistent() {
        return imagePersistent;
    }

    public void setImagePersistent(String imagePersistent) {
        this.imagePersistent = imagePersistent;
    }

    @XmlElement(name = TYPE)
    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    @XmlElement(name = DRIVER)
    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
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
    
    @XmlElement(name = SIZE)
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
    
}
