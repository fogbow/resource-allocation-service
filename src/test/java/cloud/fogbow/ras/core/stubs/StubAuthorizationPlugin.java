package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;

/**
 * This class is a stub for the AuthorizationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthorizationPlugin implements AuthorizationPlugin {

    public StubAuthorizationPlugin() {
    }

    @Override
    public boolean isAuthorized(SystemUser systemUser, String s, String s1, String s2) throws UnauthorizedRequestException, UnexpectedException {
        return true;
    }

    @Override
    public boolean isAuthorized(SystemUser systemUser, String s, String s1) throws UnauthorizedRequestException, UnexpectedException {
        return true;
    }
}
