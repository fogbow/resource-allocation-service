package org.fogbowcloud.manager.api.remote.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.CreateRemoteComputeHandler;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.DeleteRemoteComputeHandler;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.GetRemoteComputeHandler;
import org.jamppa.component.XMPPComponent;

public class XmppComponentManager extends XMPPComponent {

    private static Logger LOGGER = Logger.getLogger(XmppComponentManager.class);

    public XmppComponentManager(String jid, String password, String xmppServerIp, int xmppServerPort, long timeout) {
        super(jid, password, xmppServerIp, xmppServerPort, timeout);
        // instantiate all handlers here
        addSetHandler(new CreateRemoteComputeHandler());
        addSetHandler(new DeleteRemoteComputeHandler());
        addGetHandler(new GetRemoteComputeHandler());
    }

}
