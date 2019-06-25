package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PublicIp extends OrderApiParameter<PublicIpOrder> {
    @ApiModelProperty(required = true, example = ApiDocumentation.Model.COMPUTE_ID)
    private String computeId;

    @Override
    public PublicIpOrder createOrder() {
        PublicIpOrder order = new PublicIpOrder(computeId);
        return order;
    }

    @Override
    public void checkConsistency() {

    }

    public String getComputeId() {
        return computeId;
    }
}
