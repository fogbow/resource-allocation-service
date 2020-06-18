package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class CloseOrderAtRemoteRequesterHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(CloseOrderAtRemoteRequesterHandler.class);

    private static final String REMOTE_NOTIFY_EVENT = RemoteMethod.REMOTE_NOTIFY_EVENT.toString();

    public CloseOrderAtRemoteRequesterHandler() {
        super(REMOTE_NOTIFY_EVENT);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Log.RECEIVING_REMOTE_REQUEST_S, iq.getID()));
        IQ response = IQ.createResultIQ(iq);

        String orderId = unmarshalOrderId(iq);
        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            RemoteFacade.getInstance().closeOrderAtRemoteRequester(senderId, orderId);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element remoteOrderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        String orderId = remoteOrderIdElement.getText();
        return orderId;
    }
}
