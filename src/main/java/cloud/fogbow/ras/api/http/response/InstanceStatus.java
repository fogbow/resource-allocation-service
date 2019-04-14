package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class InstanceStatus {
    private String instanceId;
    private String instanceName;
    private String provider;
    private String cloudName;
    private InstanceState state;

    public InstanceStatus(String instanceId, String provider, String cloudName, InstanceState state) {
        this.instanceId = instanceId;
        this.provider = provider;
        this.cloudName = cloudName;
        this.state = state;
    }

    public InstanceStatus(String instanceId, String instanceName, String provider, String cloudName, InstanceState state) {
        this.instanceId = instanceId;
        this.instanceName = instanceName;
        this.provider = provider;
        this.cloudName = cloudName;
        this.state = state;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getProvider() {
        return provider;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public static InstanceState mapInstanceStateFromOrderState(OrderState orderState) throws UnexpectedException {
        switch(orderState) {
            case OPEN:
            case PENDING:
                return InstanceState.DISPATCHED;
            case SPAWNING:
                return InstanceState.CREATING;
            case FAILED_ON_REQUEST:
                return InstanceState.ERROR;
            case FULFILLED:
                return InstanceState.READY;
            case FAILED_AFTER_SUCCESSFUL_REQUEST:
                return InstanceState.FAILED;
            case UNABLE_TO_CHECK_STATUS:
                return InstanceState.UNKNOWN;
            case CLOSED:
            case DEACTIVATED:
            default:
                throw new UnexpectedException(String.format(Messages.Exception.INVALID_ORDER_STATE_S, orderState));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.instanceId == null) ? 0 : this.instanceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InstanceStatus other = (InstanceStatus) obj;
        if (this.instanceId == null) {
            if (other.getInstanceId() != null) return false;
        } else if (!this.instanceId.equals(other.getInstanceId())) return false;
        return true;
    }
}
