package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedImage extends EmulatedResource {
    private String name;

    private EmulatedImage(String instanceId, String name) {
        super(instanceId);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static class Builder {
        private String instanceId;
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public EmulatedImage build() {
            return new EmulatedImage(instanceId, name);
        }
    }
}
