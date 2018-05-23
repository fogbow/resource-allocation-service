package org.fogbowcloud.manager.core.manager.plugins.compute.openstack;

import org.fogbowcloud.manager.core.manager.plugins.InstanceStateMapper;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;

public class OpenStackComputeInstanceStateMapper implements InstanceStateMapper {

    public static final String ACTIVE_STATUS = "active";
    public static final String BUILD_STATUS = "build";

    @Override
    public InstanceState getInstanceState(String instanceState) {
        switch (instanceState.toLowerCase()) {
            case ACTIVE_STATUS:
                return InstanceState.ACTIVE;

            case BUILD_STATUS:
                return InstanceState.INACTIVE;

            default:
                return InstanceState.FAILED;
        }
    }
}
