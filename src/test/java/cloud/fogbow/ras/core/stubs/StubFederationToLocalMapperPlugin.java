package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

/**
 * This class is a stub for the FederationToLocalMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationToLocalMapperPlugin implements FederationToLocalMapperPlugin<CloudToken> {

    public StubFederationToLocalMapperPlugin(String conf1) {
    }

    @Override
    public CloudToken map(FederationUser federationUser) {
        return null;
    }
}
