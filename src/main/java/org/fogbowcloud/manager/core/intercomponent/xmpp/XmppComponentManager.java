package org.fogbowcloud.manager.core.intercomponent.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.intercomponent.xmpp.handlers.*;
import org.jamppa.component.XMPPComponent;

public class XmppComponentManager extends XMPPComponent {

    private static Logger LOGGER = Logger.getLogger(XmppComponentManager.class);

    public XmppComponentManager(String jid, String password, String xmppServerIp, int xmppServerPort, long timeout) {
        super(jid, password, xmppServerIp, xmppServerPort, timeout);
        // instantiate all handlers here
        addSetHandler(new RemoteCreateOrderRequestHandler());
        addSetHandler(new RemoteDeleteOrderRequestHandler());
        addSetHandler(new RemoteGetAllImagesRequestHandler());
        addSetHandler(new RemoteGetImageRequestHandler());
        addGetHandler(new RemoteGetOrderRequestHandler());
        addGetHandler(new RemoteGetUserQuotaRequestHandler());
        addSetHandler(new RemoteNotifyEventHandler());
    }

}
