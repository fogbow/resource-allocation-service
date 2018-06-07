package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.apache.log4j.Logger;

public class UserQuotaController {

    private static final Logger LOGGER = Logger.getLogger(UserQuotaController.class);

    public UserQuotaController() { }

    public Quota getUserQuota(String memberId, FederationUser federationUser, InstanceType instanceType) throws
            RemoteRequestException, PropertyNotSpecifiedException, TokenCreationException, QuotaException,
            UnauthorizedException {

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(federationUser, instanceType);
    }
}
