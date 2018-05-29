package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;

public interface RemoteRequest {

    void send() throws RemoteRequestException;

}
