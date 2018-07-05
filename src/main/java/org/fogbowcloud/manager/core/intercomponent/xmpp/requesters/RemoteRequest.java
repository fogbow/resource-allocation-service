package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

public interface RemoteRequest<T> {

    T send() throws Exception;

}
