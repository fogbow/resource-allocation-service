package org.fogbowcloud.manager.core.manager.plugins.compute;

import java.util.List;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.VolumeLink;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ComputePlugin {

    /**
     * This method requests the virtual machine creation on a provider.
     *
     * @param computeOrder {@link Order} for creating a virtual machine.
     * @param localToken
     * @param imageId Instance image ID. @return Instance ID.
     * @throws RequestException {@link RequestException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, Token localToken, String imageId)
            throws RequestException;

    public ComputeInstance getInstance(Token localToken, String instanceId)
            throws RequestException;

    public List<ComputeInstance> getInstances(Token localToken);

    public void deleteInstance(Token localToken, String instanceId) throws RequestException;

    public void deleteInstances(Token localToken);

    public String attachVolume(Token localToken, VolumeLink volumeLink);

    public String detachVolume(Token localToken, VolumeLink volumeLink);

    public String getImageId(Token localToken, String imageName);
}
