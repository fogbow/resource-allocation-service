package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.publicip.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;

import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.publicip.CreatePublicIpRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import org.apache.log4j.Logger;

public class GoogleCloudPublicIpPlugin implements PublicIpPlugin<GoogleCloudUser> {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudPublicIpPlugin.class);

    public GoogleCloudPublicIpPlugin(String confPathFile) {

    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        EtherType etherType = EtherType.IPv4;

        CreatePublicIpRequest request = new CreatePublicIpRequest.Builder()
                .projectId(projectId)
                .etherType(etherType)
                .build();

        String instanceId = doRequestInstance(request, cloudUser);

        return instanceId;
    }

    private String doRequestInstance(CreatePublicIpRequest request, GoogleCloudUser cloudUser) {
        // TODO: Implement doRequestInstance
        return null;
    }

    @Override
    public void deleteInstance(PublicIpOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPublicIpEndpoint(projectId) +
                            GoogleCloudConstants.ENDPOINT_SEPARATOR +
                            instanceId;

        doDeleteInstance(endpoint, cloudUser);
    }

    private void doDeleteInstance(String endpoint, GoogleCloudUser cloudUser) {
        // TODO: Implement doDeleteInstance
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        String endpoint = getPublicIpEndpoint(projectId) +
                            GoogleCloudConstants.ENDPOINT_SEPARATOR +
                            instanceId;

        return doGetInstance(endpoint, cloudUser);
    }

    private PublicIpInstance doGetInstance(String endpoint, GoogleCloudUser cloudUser) {
        // TODO: Implement doGetInstance
        return null;
    }


    private String getPublicIpEndpoint(String projectId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId) +
                GoogleCloudConstants.GLOBAL_IP_ENDPOINT;
    }

    @Override
    public boolean isReady(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.FAILED);
    }
}
