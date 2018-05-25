package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;

public class RemoteInstanceProvider implements InstanceProvider {

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
    UnauthorizedException, TokenCreationException, RequestException{
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException {

    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException {
        return null;
    }
}
