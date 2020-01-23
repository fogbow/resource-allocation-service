package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class PublicIpAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "1")
    @Column(name = "allocation_public_ips")
    private int publicIps;

    public PublicIpAllocation() {
    }

    public PublicIpAllocation(int publicIps) {
        this.publicIps = publicIps;
    }

    public int getPublicIps() {
        return publicIps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicIpAllocation that = (PublicIpAllocation) o;
        return publicIps == that.publicIps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicIps);
    }
}
