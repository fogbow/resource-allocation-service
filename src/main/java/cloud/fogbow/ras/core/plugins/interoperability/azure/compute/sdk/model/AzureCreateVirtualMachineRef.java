package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class AzureCreateVirtualMachineRef {

    @GenericBuilder.Required
    private AzureGetImageRef azureGetImageRef;
    @GenericBuilder.Required
    private String networkInterfaceId;
    @GenericBuilder.Required
    private String resourceGroupName;
    @GenericBuilder.Required
    private String resourceName;
    @GenericBuilder.Required
    private String osUserPassword;
    @GenericBuilder.Required
    private String osComputeName;
    @GenericBuilder.Required
    private String osUserName;
    @GenericBuilder.Required
    private String regionName;
    @GenericBuilder.Required
    private String userData;
    @GenericBuilder.Required
    private int diskSize;
    @GenericBuilder.Required
    private String size;
    @GenericBuilder.Required
    private Map tags;

    public static Builder builder() {
        return new Builder(AzureCreateVirtualMachineRef::new);
    }

    public AzureGetImageRef getAzureGetImageRef() {
        return this.azureGetImageRef;
    }

    private void setAzureGetImageRef(AzureGetImageRef azureGetImageRef) {
        this.azureGetImageRef = azureGetImageRef;
    }

    public String getNetworkInterfaceId() {
        return this.networkInterfaceId;
    }

    private void setNetworkInterfaceId(String networkInterfaceId) {
        this.networkInterfaceId = networkInterfaceId;
    }

    public String getResourceGroupName() {
        return this.resourceGroupName;
    }

    private void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    /* 
     * Resource name refers to the resource's identifier in the Azure cloud.
     */
    public String getResourceName() {
        return this.resourceName;
    }

    private void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getOsUserPassword() {
        return this.osUserPassword;
    }

    private void setOsUserPassword(String osUserPassword) {
        this.osUserPassword = osUserPassword;
    }

    public String getOsComputeName() {
        return this.osComputeName;
    }

    private void setOsComputeName(String osComputeName) {
        this.osComputeName = osComputeName;
    }

    public String getOsUserName() {
        return this.osUserName;
    }

    private void setOsUserName(String osUserName) {
        this.osUserName = osUserName;
    }

    public String getRegionName() {
        return this.regionName;
    }

    private void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getUserData() {
        return this.userData;
    }

    private void setUserData(String userData) {
        this.userData = userData;
    }

    public int getDiskSize() {
        return this.diskSize;
    }

    private void setDiskSize(int diskSize) {
        this.diskSize = diskSize;
    }

    public String getSize() {
        return this.size;
    }

    private void setSize(String size) {
        this.size = size;
    }
    
    public Map getTags() {
        return this.tags;
    }

    public void setTags(Map tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureCreateVirtualMachineRef that = (AzureCreateVirtualMachineRef) o;
        return this.diskSize == that.diskSize &&
                Objects.equals(this.azureGetImageRef, that.azureGetImageRef) &&
                Objects.equals(this.networkInterfaceId, that.networkInterfaceId) &&
                Objects.equals(this.resourceGroupName, that.resourceGroupName) &&
                Objects.equals(this.resourceName, that.resourceName) &&
                Objects.equals(this.osUserPassword, that.osUserPassword) &&
                Objects.equals(this.osComputeName, that.osComputeName) &&
                Objects.equals(this.osUserName, that.osUserName) &&
                Objects.equals(this.regionName, that.regionName) &&
                Objects.equals(this.userData, that.userData) &&
                Objects.equals(this.size, that.size) &&
                Objects.equals(this.tags, that.tags);
    }

    public static class Builder extends GenericBuilder<AzureCreateVirtualMachineRef> {

        Builder(Supplier instantiator) {
            super(instantiator);
        }

        public Builder azureGetImageRef(AzureGetImageRef azureGetImageRef) {
            with(AzureCreateVirtualMachineRef::setAzureGetImageRef, azureGetImageRef);
            return this;
        }

        public Builder networkInterfaceId(String networkInterfaceId) {
            with(AzureCreateVirtualMachineRef::setNetworkInterfaceId, networkInterfaceId);
            return this;
        }

        public Builder resourceGroupName(String resourceGroupName) {
            with(AzureCreateVirtualMachineRef::setResourceGroupName, resourceGroupName);
            return this;
        }

        public Builder resourceName(String resourceName) {
            with(AzureCreateVirtualMachineRef::setResourceName, resourceName);
            return this;
        }

        public Builder osUserPassword(String osUserPassword) {
            with(AzureCreateVirtualMachineRef::setOsUserPassword, osUserPassword);
            return this;
        }

        public Builder osComputeName(String osComputeName) {
            with(AzureCreateVirtualMachineRef::setOsComputeName, osComputeName);
            return this;
        }

        public Builder osUserName(String osUserName) {
            with(AzureCreateVirtualMachineRef::setOsUserName, osUserName);
            return this;
        }

        public Builder regionName(String regionName) {
            with(AzureCreateVirtualMachineRef::setRegionName, regionName);
            return this;
        }

        public Builder userData(String userData) {
            with(AzureCreateVirtualMachineRef::setUserData, userData);
            return this;
        }

        public Builder diskSize(int diskSize) {
            with(AzureCreateVirtualMachineRef::setDiskSize, diskSize);
            return this;
        }

        public Builder size(String size) {
            with(AzureCreateVirtualMachineRef::setSize, size);
            return this;
        }
        
        public Builder tags(Map tags) {
            with(AzureCreateVirtualMachineRef::setTags, tags);
            return this;
        }
    }

}
