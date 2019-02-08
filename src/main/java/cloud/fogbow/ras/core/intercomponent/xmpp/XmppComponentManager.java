package cloud.fogbow.ras.core.intercomponent.xmpp;

import cloud.fogbow.ras.core.intercomponent.xmpp.handlers.*;
import org.apache.log4j.Logger;
import org.jamppa.component.XMPPComponent;

public class XmppComponentManager extends XMPPComponent {
    private static Logger LOGGER = Logger.getLogger(XmppComponentManager.class);

    public XmppComponentManager(String jid, String password, String xmppServerIp, int xmppServerPort, long timeout) {
        super(jid, password, xmppServerIp, xmppServerPort, timeout);
        // instantiate set handlers here
        addSetHandler(new RemoteCreateOrderRequestHandler());
        addSetHandler(new RemoteDeleteOrderRequestHandler());
        addSetHandler(new RemoteNotifyEventHandler());
        addSetHandler(new RemoteCreateSecurityRuleRequestHandler());
        addSetHandler(new RemoteDeleteSecurityRuleRequestHandler());
        addSetHandler(new RemoteGenericRequestHandler());
        // instantiate get handlers here
        addGetHandler(new RemoteGetAllImagesRequestHandler());
        addGetHandler(new RemoteGetImageRequestHandler());
        addGetHandler(new RemoteGetCloudNamesRequestHandler());
        addGetHandler(new RemoteGetOrderRequestHandler());
        addGetHandler(new RemoteGetUserQuotaRequestHandler());
        addGetHandler(new RemoteGetAllSecurityRuleHandler());
    }
}
