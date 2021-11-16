package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.OrderState;
import io.swagger.annotations.ApiModelProperty;

public class InstanceStatus {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.INSTANCE_ID)
    private String instanceId;
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.INSTANCE_NAME)
    private String instanceName;
    @ApiModelProperty(position = 2, example = ApiDocumentation.Model.PROVIDER, notes = ApiDocumentation.Model.PROVIDER_NOTE)
    private String provider;
    @ApiModelProperty(position = 3, example = ApiDocumentation.Model.CLOUD_NAME, notes = ApiDocumentation.Model.CLOUD_NAME_NOTE)
    private String cloudName;
    @ApiModelProperty(position = 4, example = "READY")
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

    public static InstanceState mapInstanceStateFromOrderState(OrderState orderState) throws InternalServerErrorException {
        return mapInstanceStateFromOrderState(orderState, false, false, false);
    }

    public static InstanceState mapInstanceStateFromOrderState(OrderState orderState,
          Boolean knowWhetherInstanceIsReadyOrHasFailed, Boolean isReady, Boolean hasFailed) throws InternalServerErrorException {
        switch(orderState) {
            case OPEN:
            case SELECTED:
            case PENDING:
                return InstanceState.DISPATCHED;
            case FAILED_ON_REQUEST:
                return InstanceState.ERROR;
            case SPAWNING:
                if (knowWhetherInstanceIsReadyOrHasFailed) {
                    if (isReady) {
                        return InstanceState.READY;
                    } else if (hasFailed) {
                        return InstanceState.FAILED;
                    } else {
                        return InstanceState.CREATING;
                    }
                } else {
                    return InstanceState.CREATING;
                }
            case FULFILLED:
                if (knowWhetherInstanceIsReadyOrHasFailed) {
                    if (isReady) {
                        return InstanceState.READY;
                    } else if (hasFailed) {
                        return InstanceState.FAILED;
                    } else {
                        return InstanceState.UNKNOWN;
                    }
                } else {
                    return InstanceState.READY;
                }
            case FAILED_AFTER_SUCCESSFUL_REQUEST:
                return InstanceState.FAILED;
            case UNABLE_TO_CHECK_STATUS:
                if (knowWhetherInstanceIsReadyOrHasFailed) {
                    if (isReady) {
                        return InstanceState.READY;
                    } else if (hasFailed) {
                        return InstanceState.FAILED;
                    } else {
                        return InstanceState.UNKNOWN;
                    }
                } else {
                    return InstanceState.UNKNOWN;
                }
            case ASSIGNED_FOR_DELETION:
            case CHECKING_DELETION:
                return InstanceState.DELETING;
            case CLOSED:
                return InstanceState.DELETED;
            case PAUSING:
                return InstanceState.PAUSING;
            case PAUSED:
                return InstanceState.PAUSED;
            case HIBERNATING:
                return InstanceState.HIBERNATING;
            case HIBERNATED:
                return InstanceState.HIBERNATED;
            case RESUMING:
                return InstanceState.RESUMING;
            case STOPPING:
                return InstanceState.STOPPING;
            case STOPPED:
                return InstanceState.STOPPED;
            default:
                throw new InternalServerErrorException(Messages.Exception.UNEXPECTED_ERROR);
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
