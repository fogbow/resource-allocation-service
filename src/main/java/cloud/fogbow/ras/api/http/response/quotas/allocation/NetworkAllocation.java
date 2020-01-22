package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class NetworkAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "1")
    @Column(name = "allocation_networks")
    private int networks;

    public NetworkAllocation(int networks) {
        this.networks = networks;
    }

    public NetworkAllocation() {
    }

    public int getNetworks() {
        return networks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkAllocation that = (NetworkAllocation) o;
        return networks == that.networks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networks);
    }
}
