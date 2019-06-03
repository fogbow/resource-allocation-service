package cloud.fogbow.ras.api.http.response;

import io.swagger.annotations.ApiModelProperty;

public class OrderInstance extends Instance {
    @ApiModelProperty(position = 3, example = "READY")
    private InstanceState state;
    @ApiModelProperty(position = 4, example = "active")
    private String cloudState;
    @ApiModelProperty(position = 5)
    private boolean isReady;
    @ApiModelProperty(position = 6)
    private boolean hasFailed;

    public OrderInstance(String id) {
        super(id);
        this.isReady = false;
        this.hasFailed = false;
    }

    public OrderInstance(String id, String cloudState) {
        this(id);
        this.cloudState = cloudState;
    }

    public InstanceState getState() {
        return this.state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public String getCloudState() {
        return cloudState;
    }

    public void setCloudState(String cloudState) {
        cloudState = cloudState;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady() {
        isReady = true;
        hasFailed = false;
    }

    public boolean hasFailed() {
        return hasFailed;
    }

    public void setHasFailed() {
        this.hasFailed = true;
        this.isReady = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OrderInstance other = (OrderInstance) obj;
        if (getId() == null) {
            if (other.getId() != null) return false;
        } else if (!getId().equals(other.getId())) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "id='" + getId() + '\'' +
                ", state=" + state +
                ", provider='" + getProvider() + '\'' +
                ", cloudName='" + getCloudName() + '\'' +
                '}';
    }
}
