package cloud.fogbow.ras.core.plugins.authorization;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.core.models.RasOperation;

public class DefaultAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) {
        return true;
    }
}
