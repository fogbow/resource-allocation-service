package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class CreateRemoteOrderHandler extends AbstractQueryHandler {

    public CreateRemoteOrderHandler() {
        super(RemoteMethod.CREATE_REMOTE_ORDER.name());
    }

    @Override
    public IQ handle(IQ iq) {
        return null;
    }
}
