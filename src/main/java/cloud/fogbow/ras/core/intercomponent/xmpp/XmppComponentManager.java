package cloud.fogbow.ras.core.intercomponent.xmpp;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.xmpp.handlers.*;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemotePauseOrderRequest;
import org.apache.log4j.Logger;
import org.jamppa.component.XMPPComponent;

public class XmppComponentManager extends XMPPComponent {
    private static Logger LOGGER = Logger.getLogger(XmppComponentManager.class);

    public XmppComponentManager(String jid, String password, String xmppServerIp, int xmppServerPort, long timeout) {
        super(jid, password, xmppServerIp, xmppServerPort, timeout);
        // instantiate set handlers here
        addSetHandler(new RemoteCreateOrderRequestHandler());
        addSetHandler(new RemoteDeleteOrderRequestHandler());
        addSetHandler(new RemotePauseOrderRequestHandler());
        addSetHandler(new RemoteHibernateOrderRequestHandler());
        addSetHandler(new RemoteResumeOrderRequestHandler());
        addSetHandler(new CloseOrderAtRemoteRequesterHandler());
        addSetHandler(new RemoteCreateSecurityRuleRequestHandler());
        addSetHandler(new RemoteDeleteSecurityRuleRequestHandler());
        // instantiate get handlers here
        addGetHandler(new RemoteGetAllImagesRequestHandler());
        addGetHandler(new RemoteGetImageRequestHandler());
        addGetHandler(new RemoteGetCloudNamesRequestHandler());
        addGetHandler(new RemoteGetInstanceRequestHandler());
        addGetHandler(new RemoteGetOrderRequestHandler());
        addGetHandler(new RemoteGetUserQuotaRequestHandler());
        addGetHandler(new RemoteGetAllSecurityRuleHandler());
        addGetHandler(new RemoteTakeSnapshotRequestHandler());
        LOGGER.info(Messages.Log.XMPP_HANDLERS_SET);
    }
}
