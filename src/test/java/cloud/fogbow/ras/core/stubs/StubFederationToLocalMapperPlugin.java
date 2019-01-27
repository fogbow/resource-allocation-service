package cloud.fogbow.ras.core.stubs;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

/**
 * This class is a stub for the FederationToLocalMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationToLocalMapperPlugin implements FederationToLocalMapperPlugin {

    public StubFederationToLocalMapperPlugin(String conf1, String conf2) {
    }

    @Override
    public Token map(FederationUserToken user) {
        return null;
    }
}
