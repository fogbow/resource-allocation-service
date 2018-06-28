package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;

public interface RemoteRequest<T> {

    T send() throws FogbowManagerException;

}
