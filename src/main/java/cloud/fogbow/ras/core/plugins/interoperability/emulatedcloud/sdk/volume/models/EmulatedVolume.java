package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedVolume extends EmulatedResource {
    private final String size;
    private final String name;
    private final String status;

    private EmulatedVolume(String instanceId, String name, String size, String status) {
        super(instanceId);
        this.name = name;
        this.size = size;
        this.status = status;
    }

    public static class Builder {
        private String name;
        private String size;
        private String instanceId;
        private String status;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public EmulatedVolume build() {
            return new EmulatedVolume(instanceId, name, size, status);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getSize() {
        return this.size;
    }

    public String getStatus() {
        return this.status;
    }
}

