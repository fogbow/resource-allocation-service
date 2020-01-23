package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class VolumeAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "30")
    @Column(name = "allocation_disk")
    private int disk;

    public VolumeAllocation(int disk) {
        this.disk = disk;
    }

    public VolumeAllocation() {
    }

    public int getDisk() {
        return disk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VolumeAllocation that = (VolumeAllocation) o;
        return disk == that.disk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(disk);
    }
}
