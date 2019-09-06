package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel
public class Attachment implements OrderApiParameter {
    @ApiModelProperty(position = 0, required = true, example = ApiDocumentation.Model.VOLUME_ID)
    private String volumeId;
    @ApiModelProperty(position = 1, required = true, example = ApiDocumentation.Model.COMPUTE_ID)
    private String computeId;
    @ApiModelProperty(position = 2, example = ApiDocumentation.Model.DEVICE, notes = ApiDocumentation.Model.DEVICE_NOTE)
    private String device;

    public String getComputeId() {
        return computeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getDevice() {
        return device;
    }

    @Override
    public AttachmentOrder getOrder() {
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        AttachmentOrder order = new AttachmentOrder(computeId, volumeId, device);
        order.setRequester(localProviderId);
        return order;
    }
}
