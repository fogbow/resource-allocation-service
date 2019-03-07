package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;

/**
 * This class is a stub for the SystemToCloudMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubSystemToCloudMapperPlugin implements SystemToCloudMapperPlugin<CloudUser> {

    public StubSystemToCloudMapperPlugin(String conf1) {
    }

    @Override
    public CloudUser map(SystemUser systemUser) {
        return null;
    }
}
