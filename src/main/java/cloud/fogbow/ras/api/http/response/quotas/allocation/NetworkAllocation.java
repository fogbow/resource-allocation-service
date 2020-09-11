package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class NetworkAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "1")
    private int instances;

    public NetworkAllocation(int instances) {
        this.instances = instances;
    }

    public NetworkAllocation() {
    }

    public int getInstances() {
        return instances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkAllocation that = (NetworkAllocation) o;
        return instances == that.instances;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instances);
    }
}
