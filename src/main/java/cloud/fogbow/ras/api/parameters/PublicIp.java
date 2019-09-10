package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PublicIp implements OrderApiParameter{
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.COMPUTE_ID)
    private String computeId;

    @Override
    public PublicIpOrder getOrder() {
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        PublicIpOrder order = new PublicIpOrder(computeId);
        order.setRequester(localProviderId);
        return order;
    }

    public String getComputeId() {
        return computeId;
    }
}
