package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;

public interface RemoteRequest<T> {

    T send() throws RemoteRequestException, OrderManagementException, UnauthorizedException;

}
