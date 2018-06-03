package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;

public interface RemoteRequest<T> {

    T send() throws RemoteRequestException, OrderManagementException, UnauthorizedException;

}
