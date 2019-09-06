package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.CloudListController;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel
public class Network implements OrderApiParameter {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.PROVIDER, notes = ApiDocumentation.Model.PROVIDER_NOTE)
    private String provider;
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.CLOUD_NAME, notes = ApiDocumentation.Model.CLOUD_NAME_NOTE)
    private String cloudName;
    @ApiModelProperty(position = 2, example = ApiDocumentation.Model.NETWORK_NAME)
    private String name;
    @ApiModelProperty(position = 3, required = true, example = "10.0.0.0/8")
    private String cidr;
    @ApiModelProperty(position = 4, required = true, example = "10.10.0.1")
    private String gateway;
    @ApiModelProperty(position = 5, required = true, example = "dynamic")
    private NetworkAllocationMode allocationMode;

    @Override
    public NetworkOrder getOrder() {
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        String defaultCloudName = (new CloudListController()).getDefaultCloudName();
        if (this.provider == null) this.provider = localProviderId;
        if (this.cloudName == null) this.cloudName = defaultCloudName;
        if (this.name == null) this.name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + UUID.randomUUID();
        NetworkOrder order = new NetworkOrder(provider, cloudName, name, gateway, cidr, allocationMode);
        order.setRequester(localProviderId);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getName() {
        return name;
    }

    public String getGateway() {
        return gateway;
    }

    public String getCidr() {
        return cidr;
    }

    public NetworkAllocationMode getAllocationMode() {
        return allocationMode;
    }
}
