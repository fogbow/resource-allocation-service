package org.fogbowcloud.manager.api.remote.xmpp;

import org.apache.log4j.Logger;
import org.jamppa.component.XMPPComponent;

public class ManagerXmppComponent extends XMPPComponent {

    private static Logger LOGGER = Logger.getLogger(ManagerXmppComponent.class);

    public ManagerXmppComponent(String jid, String password, String server, int port, long timeout) {
        super(jid, password, server, port, timeout);
        // instantiate all handlers here
    }

}
