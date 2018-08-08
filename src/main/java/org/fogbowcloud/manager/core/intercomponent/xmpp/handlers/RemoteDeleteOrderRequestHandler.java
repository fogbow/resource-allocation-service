package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteDeleteOrderRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteDeleteOrderRequestHandler.class);

    public static final String REMOTE_DELETE_ORDER = RemoteMethod.REMOTE_DELETE_ORDER.toString();

    public RemoteDeleteOrderRequestHandler() {
        super(REMOTE_DELETE_ORDER);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug("Received request for order: " + iq.getID());

        String orderId = unmarshalOrderId(iq);
        ResourceType resourceType = unmarshalInstanceType(iq);
        FederationUser federationUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            RemoteFacade.getInstance().deleteOrder(orderId, federationUser, resourceType);
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

    private ResourceType unmarshalInstanceType(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());
        ResourceType resourceType = new Gson().fromJson(orderTypeElementRequest.getText(), ResourceType.class);
        return resourceType;
    }

    private FederationUser unmarshalFederationUser(IQ iq) {
        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);
        return federationUser;
    }
}
