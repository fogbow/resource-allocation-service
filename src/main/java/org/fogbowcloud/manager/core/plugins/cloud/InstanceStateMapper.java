package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;

public interface InstanceStateMapper {
    /**
     * This method will map Openstack instance status to Fogbow instance status.
     *
     * @param instanceState status.
     * @return {@link InstanceState}
     */
    public InstanceState getInstanceState(String instanceState);
}
