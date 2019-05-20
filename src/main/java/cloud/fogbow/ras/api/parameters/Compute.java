package cloud.fogbow.ras.api.parameters;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApiModel
public class Compute implements OrderApiParameter {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.PROVIDER, notes = ApiDocumentation.Model.PROVIDER_NOTE)
    private String provider;
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.CLOUD_NAME, notes = ApiDocumentation.Model.CLOUD_NAME_NOTE)
    private String cloudName;
    @ApiModelProperty(position = 2, example = ApiDocumentation.Model.COMPUTE_NAME, notes = ApiDocumentation.Model.COMPUTE_NAME_NOTE)
    private String name;
    @ApiModelProperty(position = 3, example = "2", notes = ApiDocumentation.Model.VCPU_NOTE)
    private int vCPU;
    @ApiModelProperty(position = 4, example = "1", notes = ApiDocumentation.Model.MEMORY_NOTE)
    private int memory;
    @ApiModelProperty(position = 5, example = "1", notes = ApiDocumentation.Model.DISK_NOTE)
    private int disk;
    @ApiModelProperty(position = 6, required = true, example = ApiDocumentation.Model.IMAGE_ID, notes = ApiDocumentation.Model.IMAGE_NOTE)
    private String imageId;
    @ApiModelProperty(position = 7, example = ApiDocumentation.Model.SSH_PUBLIC_KEY, notes = ApiDocumentation.Model.SSH_PUBLIC_KEY_NOTE)
    private String publicKey;
    @ApiModelProperty(position = 8, example = ApiDocumentation.Model.USER_DATA, notes = ApiDocumentation.Model.USER_DATA_NOTE)
    private ArrayList<UserData> userData;
    @ApiModelProperty(position = 9, example = ApiDocumentation.Model.NETWORK_IDS, notes = ApiDocumentation.Model.NETWORK_IDS_NOTE)
    private List<String> networkIds;
    @ApiModelProperty(position = 10, example = ApiDocumentation.Model.COMPUTE_REQUIREMENTS, notes = ApiDocumentation.Model.COMPUTE_REQUIREMENTS_NOTE)
    private Map<String, String> requirements;

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getName() {
        return name;
    }

    public int getvCPU() {
        return vCPU;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    public String getImageId() {
        return imageId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<String> getNetworkIds() {
        return networkIds;
    }

    public List<UserData> getUserData() {
        return userData;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public void setUserData(ArrayList<UserData> userData) {
        this.userData = userData;
    }

    @Override
    public ComputeOrder getOrder() {
        ComputeOrder order = new ComputeOrder(provider, cloudName, name, vCPU, memory, disk, imageId, userData,
                publicKey, networkIds);
        order.setRequirements(requirements);
        return order;
    }
}
