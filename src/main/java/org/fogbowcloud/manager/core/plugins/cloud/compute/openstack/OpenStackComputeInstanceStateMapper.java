package org.fogbowcloud.manager.core.plugins.cloud.compute.openstack;

import org.fogbowcloud.manager.core.plugins.cloud.InstanceStateMapper;
import org.fogbowcloud.manager.core.models.instances.InstanceState;

public class OpenStackComputeInstanceStateMapper implements InstanceStateMapper {

    public static final String ACTIVE_STATUS = "active";
    public static final String BUILD_STATUS = "build";

    @Override
    public InstanceState getInstanceState(String instanceState) {
        switch (instanceState.toLowerCase()) {
            case ACTIVE_STATUS:
                return InstanceState.READY;

            case BUILD_STATUS:
                return InstanceState.SPAWNING;

            default:
                return InstanceState.FAILED;
        }
    }
}
