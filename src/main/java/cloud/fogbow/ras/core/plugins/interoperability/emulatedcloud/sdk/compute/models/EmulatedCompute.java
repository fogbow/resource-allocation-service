package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.models;

import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

import java.util.List;

public class EmulatedCompute extends EmulatedResource {
    private String cloudState;
    private String name;
    private int vCPU;
    private int memory;
    private int disk;
    private List<String> ipAddresses;
    private String imageId;
    private String publicKey;
    private List<UserData> userData;
    private List<NetworkSummary> networks;
    private String cloudName;
    private String provider;

    private EmulatedCompute(String instanceId, String cloudState, String name, int vCPU,
                            int memory, int disk, List<String> ipAddresses, String imageId,
                            String publicKey, List<UserData> userData, List<NetworkSummary> networks) {
        super(instanceId);
        this.cloudState = cloudState;
        this.name = name;
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
        this.imageId = imageId;
        this.publicKey = publicKey;
        this.userData = userData;
        this.networks = networks;
    }

    public String getCloudState() {
        return cloudState;
    }
    
    public void setCloudState(String cloudState) {
        this.cloudState = cloudState;
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

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public String getImageId() {
        return imageId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<UserData> getUserData() {
        return userData;
    }

    public List<NetworkSummary> getNetworks() {
        return networks;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getProvider() {
        return provider;
    }

    public static class Builder {
        private String instanceId;
        private String cloudState;
        private String name;
        private int vCPU;
        private int memory;
        private int disk;
        private List<String> ipAddresses;
        private String imageId;
        private String publicKey;
        private List<UserData> userData;
        private List<NetworkSummary> networks;

        public Builder instanceId(String instanceId){
            this.instanceId = instanceId;
            return this;
        }

        public Builder cloudState(String cloudState){
            this.cloudState = cloudState;
            return this;
        }

        public Builder name(String name){
            this.name = name;
            return this;
        }

        public Builder vCPU(int vCPU){
            this.vCPU = vCPU;
            return this;
        }

        public Builder memory(int memory){
            this.memory = memory;
            return this;
        }

        public Builder disk(int disk){
            this.disk = disk;
            return this;
        }

        public Builder ipAddresses(List<String> ipAddresses){
            this.ipAddresses = ipAddresses;
            return this;
        }

        public Builder imageId(String imageId){
            this.imageId = imageId;
            return this;
        }

        public Builder publicKey(String publicKey){
            this.publicKey = publicKey;
            return this;
        }

        public Builder userData(List<UserData> userData){
            this.userData = userData;
            return this;
        }

        public Builder networks(List<NetworkSummary> networks) {
            this.networks = networks;
            return this;
        }

        public EmulatedCompute build() {
            return new EmulatedCompute(instanceId, cloudState, name, vCPU, memory,
                    disk, ipAddresses, imageId, publicKey, userData, networks);
        }
    }
}
