package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteTakeSnapshotRequestHandler extends AbstractQueryHandler {

    private static final String REMOTE_TAKE_SNAPSHOT = RemoteMethod.REMOTE_TAKE_SNAPSHOT.toString();
    private static final Logger LOGGER = Logger.getLogger(RemoteTakeSnapshotRequestHandler.class);

    public RemoteTakeSnapshotRequestHandler() {
        super(REMOTE_TAKE_SNAPSHOT);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Log.RECEIVING_REMOTE_REQUEST_S, iq.getID()));
        String orderId = unmarshalOrderId(iq);
        String snapshotName = unmarshalSnapshotName(iq);
        SystemUser systemUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            RemoteFacade.getInstance().takeSnapshot(senderId, orderId, snapshotName, systemUser);
        } catch (Throwable e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER.toString());
        String orderId = orderIdElement.getText();
        return orderId;
    }

    private String unmarshalSnapshotName(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element snapshotNameElement = queryElement.element(IqElement.SNAPSHOT_NAME.toString());
        String snapshotName = snapshotNameElement.getText();
        return snapshotName;
    }

    private SystemUser unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        SystemUser systemUser = new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
        return systemUser;
    }
}
