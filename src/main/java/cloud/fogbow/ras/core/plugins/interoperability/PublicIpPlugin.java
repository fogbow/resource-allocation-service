package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;

public interface PublicIpPlugin {

    public static final String SECURITY_GROUP_PREFIX = "ras-sg-pip-";

    String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, CloudToken token) throws FogbowException;

    void deleteInstance(String publicIpInstanceId, String computeInstanceId, CloudToken token) throws FogbowException;

    PublicIpInstance getInstance(String publicIpInstanceId, CloudToken token) throws FogbowException;

}
