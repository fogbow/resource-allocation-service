package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.CloudListController;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;
import java.util.UUID;

@ApiModel
public class Volume implements OrderApiParameter {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.PROVIDER, notes = ApiDocumentation.Model.PROVIDER_NOTE)
    private String provider;
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.CLOUD_NAME, notes = ApiDocumentation.Model.CLOUD_NAME_NOTE)
    private String cloudName;
    @ApiModelProperty(position = 2, example = ApiDocumentation.Model.VOLUME_NAME)
    private String name;
    @ApiModelProperty(position = 3, required = true, example = "1", notes = ApiDocumentation.Model.VOLUME_SIZE_NOTE)
    private int size;
    @ApiModelProperty(position = 4, example = ApiDocumentation.Model.VOLUME_REQUIREMENTS, notes = ApiDocumentation.Model.VOLUME_REQUIREMENTS_NOTE)
    private Map<String, String> requirements;

    @Override
    public VolumeOrder getOrder() {
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        String defaultCloudName = (new CloudListController()).getDefaultCloudName();
        if (this.provider == null) this.provider = localProviderId;
        if (this.cloudName == null) this.cloudName = defaultCloudName;
        if (this.name == null) this.name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + UUID.randomUUID();
        VolumeOrder order = new VolumeOrder(this.provider, this.cloudName, this.name, this.size);
        order.setRequester(localProviderId);
        order.setRequirements(requirements);
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

    public int getSize() {
        return size;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }
}
