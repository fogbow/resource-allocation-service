package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import java.util.Objects;

@Embeddable
public class VolumeAllocation extends Allocation {
    
    @ApiModelProperty(position = 0, example = "5")
    @Transient
    private int instances;
    
    @ApiModelProperty(position = 1, example = "30")
    @Column(name = "allocation_disk")
    private int storage;

    public VolumeAllocation(int instances, int storage) {
        this.instances = instances;
        this.storage = storage;
    }
    
    public VolumeAllocation(int storage) {
        this.storage = storage;
    }
    
    public VolumeAllocation() {
    }
    
    public int getInstances() {
        return instances;
    }

    public int getStorage() {
        return storage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VolumeAllocation that = (VolumeAllocation) o;
        return instances == that.instances 
                && storage == that.storage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage);
    }
}
