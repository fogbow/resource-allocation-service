package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk;

import java.util.Objects;

public abstract class EmulatedResource {
    private String instanceId;

    public EmulatedResource(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmulatedResource that = (EmulatedResource) o;
        return Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId);
    }
}
