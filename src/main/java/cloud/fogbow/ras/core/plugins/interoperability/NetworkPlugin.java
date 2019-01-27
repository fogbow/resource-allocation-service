package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;

public interface NetworkPlugin {

    public static final String SECURITY_GROUP_PREFIX = "ras-sg-pn-";

    public String requestInstance(NetworkOrder networkOrder, CloudToken localUserAttributes) throws FogbowException;

    public NetworkInstance getInstance(String networkInstanceId, CloudToken localUserAttributes) throws FogbowException;

    public void deleteInstance(String networkInstanceId, CloudToken localUserAttributes) throws FogbowException;
}
