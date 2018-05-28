package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.apache.log4j.Logger;
import org.jamppa.component.XMPPComponent;

public class ManagerXMPPComponent extends XMPPComponent {

    private static Logger LOGGER = Logger.getLogger(ManagerXMPPComponent.class);


    public ManagerXMPPComponent(String jid, String password, String server, int port, long timeout) {
        super(jid, password, server, port, timeout);
        // instantiate all handlers here
    }

}
