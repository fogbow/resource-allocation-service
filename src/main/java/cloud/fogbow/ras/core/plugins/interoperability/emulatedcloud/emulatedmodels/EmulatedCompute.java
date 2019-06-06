package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.ras.core.models.UserData;
import com.google.gson.annotations.SerializedName;
import javafx.util.Pair;

import java.util.List;

public class EmulatedCompute implements JsonSerializable {

    @SerializedName(INSTANCE_ID_KEY_JSON)
    private String instanceId;

    @SerializedName(CLOUD_STATE_KEY_JSON)
    private String cloudState;

    @SerializedName(NAME_KEY_JSON )
    private String name;

    @SerializedName(VCPU_KEY_JSON)
    private int vcpu;

    @SerializedName(MEMORY_KEY_JSON)
    private int memory;

    @SerializedName(DISK_KEY_JSON)
    private int disk;

    @SerializedName(IP_ADDRESSES_KEY_JSON)
    private List<String> ipAddresses;

    @SerializedName(IMAGE_ID_KEY_JSON)
    private String imageId;

    @SerializedName(PUBLIC_KEY_KEY_JSON)
    private String publicKey;

    @SerializedName(USER_DATA_KEY_JSON)
    private List<UserData> userData;

    @SerializedName(NETWORK_KEY_JSON)
    private List<Pair<String, String> > networks;

    @SerializedName(CLOUD_NAME_KEY_JSON)
    private String cloudName;

    @SerializedName(PROVIDER_KEY_JSON)
    private String provider;

    private EmulatedCompute(String instanceId, String cloudState, String name, int vcpu,
                            int memory, int disk, List<String> ipAddresses, String imageId,
                            String publicKey, List<UserData> userData) {
        this.instanceId = instanceId;
        this.cloudState = cloudState;
        this.name = name;
        this.vcpu = vcpu;
        this.memory = memory;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
        this.imageId = imageId;
        this.publicKey = publicKey;
        this.userData = userData;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedCompute fromJson(String jsonContent) {
        return GsonHolder.getInstance().fromJson(jsonContent, EmulatedCompute.class);
    }

    public static class Builder {
        private String instanceId;
        private String cloudState;
        private String name;
        private int vcpu;
        private int memory;
        private int disk;
        private List<String> ipAddresses;
        private String imageId;
        private String publicKey;
        private List<UserData> userData;

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

        public Builder vcpu(int vcpu){
            this.vcpu = vcpu;
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

        public EmulatedCompute build() {
            return new EmulatedCompute(instanceId, cloudState, name, vcpu, memory,
                    disk, ipAddresses, imageId, publicKey, userData);
        }
    }

    public void setNetworks(List<Pair<String, String>> networks) {
        this.networks = networks;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getCloudState() {
        return cloudState;
    }

    public String getName() {
        return name;
    }

    public int getVcpu() {
        return vcpu;
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

    public List<Pair<String, String>> getNetworks() {
        return networks;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
