package cloud.fogbow.ras.core.plugins.authorization;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.core.models.RasOperation;

public class RasAuthPlugin implements AuthorizationPlugin<RasOperation> {

    @Override
    public boolean isAuthorized(SystemUser systemUserToken, String cloudName, String operation, String type)
            throws UnauthorizedRequestException, UnexpectedException {
        return true;
    }

    @Override
    public boolean isAuthorized(SystemUser systemUserToken, String operation, String type)
            throws UnauthorizedRequestException, UnexpectedException {
        return true;
    }

    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) {
        return true;
    }
}
