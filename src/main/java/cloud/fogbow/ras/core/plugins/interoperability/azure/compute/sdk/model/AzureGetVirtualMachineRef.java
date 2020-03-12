package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class AzureGetVirtualMachineRef {

    private String id;
    private String cloudState;
    private String name;
    private int vCPU;
    private int memory;
    private int disk;
    private List<String> ipAddresses;
    private Map<String, String> tags;

    public static Builder builder() {
        return new Builder(AzureGetVirtualMachineRef::new);
    }

    public String getId() {
        return this.id;
    }

    private void setId(String id) {
        this.id = id;
    }

    public String getCloudState() {
        return this.cloudState;
    }

    private void setCloudState(String cloudState) {
        this.cloudState = cloudState;
    }

    public String getName() {
        return this.name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public int getvCPU() {
        return vCPU;
    }

    private void setvCPU(int vCPU) {
        this.vCPU = vCPU;
    }

    public int getMemory() {
        return this.memory;
    }

    private void setMemory(int memory) {
        this.memory = memory;
    }

    public int getDisk() {
        return this.disk;
    }

    private void setDisk(int disk) {
        this.disk = disk;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    private void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
    
    public Map<String, String> getTags() {
        return tags;
    }

    private void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureGetVirtualMachineRef that = (AzureGetVirtualMachineRef) o;
        return this.vCPU == that.vCPU &&
                this.memory == that.memory &&
                this.disk == that.disk &&
                Objects.equals(this.id, that.id) &&
                Objects.equals(this.cloudState, that.cloudState) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.ipAddresses, that.ipAddresses) &&
                Objects.equals(this.tags, that.tags);
    }

    public static class Builder extends GenericBuilder<AzureGetVirtualMachineRef> {

        protected Builder(Supplier<AzureGetVirtualMachineRef> instantiator) {
            super(instantiator);
        }

        public Builder id(String id) {
            with(AzureGetVirtualMachineRef::setId, id);
            return this;
        }

        public Builder cloudState(String cloudState) {
            with(AzureGetVirtualMachineRef::setCloudState, cloudState);
            return this;
        }

        public Builder name(String name) {
            with(AzureGetVirtualMachineRef::setName, name);
            return this;
        }

        public Builder vCPU(int vCPU) {
            with(AzureGetVirtualMachineRef::setvCPU, vCPU);
            return this;
        }

        public Builder memory(int memory) {
            with(AzureGetVirtualMachineRef::setMemory, memory);
            return this;
        }

        public Builder disk(int disk) {
            with(AzureGetVirtualMachineRef::setDisk, disk);
            return this;
        }

        public Builder ipAddresses(List<String> ipAddresses) {
            with(AzureGetVirtualMachineRef::setIpAddresses, ipAddresses);
            return this;
        }
        
        public Builder tags(Map<String, String> tags) {
            with(AzureGetVirtualMachineRef::setTags, tags);
            return this;
        }

    }

}
