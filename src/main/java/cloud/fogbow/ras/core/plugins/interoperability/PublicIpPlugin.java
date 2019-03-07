package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;

public interface PublicIpPlugin<T extends CloudUser> {

    public static final String SECURITY_GROUP_PREFIX = "ras-sg-pip-";

    String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, T cloudUser) throws FogbowException;

    void deleteInstance(String publicIpInstanceId, String computeInstanceId, T cloudUser) throws FogbowException;

    PublicIpInstance getInstance(String publicIpInstanceId, T cloudUser) throws FogbowException;

}
