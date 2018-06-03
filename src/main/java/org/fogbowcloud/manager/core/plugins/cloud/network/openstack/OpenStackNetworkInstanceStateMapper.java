package org.fogbowcloud.manager.core.plugins.cloud.network.openstack;

import org.fogbowcloud.manager.core.manager.plugins.cloud.InstanceStateMapper;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;

public class OpenStackNetworkInstanceStateMapper implements InstanceStateMapper {

    public static final String BUILD_STATUS = "BUILD";
    public static final String ACTIVE_STATUS = "READY";
    public static final String DOWN_STATUS = "DOWN";
    public static final String ERROR_STATUS = "ERROR";

    @Override
    public InstanceState getInstanceState(String instanceState) {
        InstanceState state = null;
        if (instanceState.equals(BUILD_STATUS) || instanceState.equals(DOWN_STATUS)) {
            state = InstanceState.INACTIVE;
        } else if (instanceState.equals(ACTIVE_STATUS)) {
            state = InstanceState.READY;
        } else {
            state = InstanceState.FAILED;
        }
        return state;
    }

}
