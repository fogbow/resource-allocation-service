package org.fogbowcloud.manager.api.remote.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.RemoteCreateOrderRequestHandler;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.RemoteDeleteOrderRequestHandler;
import org.fogbowcloud.manager.api.remote.xmpp.handlers.RemoteGetOrderRequestHandler;
import org.jamppa.component.XMPPComponent;

public class XmppComponentManager extends XMPPComponent {

    private static Logger LOGGER = Logger.getLogger(XmppComponentManager.class);

    public XmppComponentManager(String jid, String password, String xmppServerIp, int xmppServerPort, long timeout) {
        super(jid, password, xmppServerIp, xmppServerPort, timeout);
        // instantiate all handlers here
        addSetHandler(new RemoteCreateOrderRequestHandler());
        addSetHandler(new RemoteDeleteOrderRequestHandler());
        addGetHandler(new RemoteGetOrderRequestHandler());
    }

}
