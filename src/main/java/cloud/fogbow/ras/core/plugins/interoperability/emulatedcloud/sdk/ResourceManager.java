package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk;

import java.util.List;
import java.util.Optional;

public interface ResourceManager<T extends EmulatedResource> {
    Optional<T> find(String instanceId);
    List<T> list();
    String create(T resource);
    void delete(String instanceId);
}
