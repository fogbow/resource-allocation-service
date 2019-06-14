package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

public class EmulatedComputeQuota implements JsonSerializable {

    @SerializedName(TOTAL_QUOTA_KEY_JSON)
    private Quota totalQuota;

    @SerializedName(USED_QUOTA_KEY_JSON)
    private Quota usedQuota;

    public EmulatedComputeQuota() {
        this.totalQuota = generateNewQuotaObject();
        this.usedQuota = generateNewQuotaObject();
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Quota {
        @SerializedName(INSTANCES_KEY_JSON)
        private int intances;

        @SerializedName(VCPU_KEY_JSON)
        private int vcpu;

        @SerializedName(DISK_KEY_JSON)
        private int disk;

        @SerializedName(MEMORY_KEY_JSON)
        private int memory;

        private Quota(int intances, int vcpu, int disk, int memory) {
            this.intances = intances;
            this.vcpu = vcpu;
            this.disk = disk;
            this.memory = memory;
        }

        public static class Builder {
            private int instances;
            private int vcpu;
            private int disk;
            private int memory;

            public Builder instances(int instances) {
                this.instances = instances;
                return this;
            }

            public Builder vcpu(int vcpu) {
                this.vcpu = vcpu;
                return this;
            }

            public Builder memory(int memory) {
                this.memory = memory;
                return this;
            }

            public Builder disk(int disk) {
                this.disk = disk;
                return this;
            }

            public Quota build(){
                return new Quota(instances, vcpu, disk, memory);
            }
        }

        public int getIntances() {
            return intances;
        }

        public int getVcpu() {
            return vcpu;
        }

        public int getDisk() {
            return disk;
        }

        public int getMemory() {
            return memory;
        }
    }

    private Quota generateNewQuotaObject(){
        return new Quota.Builder()
                .instances(EmulatedCloudConstants.Plugins.Quota.DEFAULT_INSTANCES)
                .disk(EmulatedCloudConstants.Plugins.Quota.DEFAULT_DISK)
                .memory(EmulatedCloudConstants.Plugins.Quota.DEFAULT_MEMORY)
                .vcpu(EmulatedCloudConstants.Plugins.Quota.DEFAULT_VCPU)
                .build();
    }

    public Quota getTotalQuota() {
        return totalQuota;
    }

    public Quota getUsedQuota() {
        return usedQuota;
    }
}
