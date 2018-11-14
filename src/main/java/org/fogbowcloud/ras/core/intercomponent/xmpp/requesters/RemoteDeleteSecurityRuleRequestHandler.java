package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteDeleteSecurityRuleRequestHandler extends AbstractQueryHandler {

    public RemoteDeleteSecurityRuleRequestHandler() {
        super(RemoteMethod.REMOTE_DELETE_SECURITY_GROUP.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        return null;
    }

}
