package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteGetOrderRequestHandler extends AbstractQueryHandler {

    public static final String REMOTE_GET_INSTANCE = RemoteMethod.REMOTE_GET_ORDER.toString();

    public RemoteGetOrderRequestHandler() {
        super(REMOTE_GET_INSTANCE);
    }

    @Override
    public IQ handle(IQ iq) {
        String orderId = unmarshalOrderId(iq);
        ResourceType resourceType = unmarshalResourceType(iq);
        FederationUser federationUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Instance instance = RemoteFacade.getInstance().getResourceInstance(orderId,
                    federationUser, resourceType);

            //on success, update response with instance data
            updateResponse(response, instance);
        } catch (Exception e) {
            //on error, update response with exception data
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private void updateResponse(IQ response, Instance instance) {
        Element queryElement =
                response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_INSTANCE);

        Element instanceElement = queryElement.addElement(IqElement.INSTANCE.toString());

        Element instanceClassNameElement =
                queryElement.addElement(IqElement.INSTANCE_CLASS_NAME.toString());

        instanceClassNameElement.setText(instance.getClass().getName());

        instanceElement.setText(new Gson().toJson(instance));
    }

    private FederationUser unmarshalFederationUser(IQ iq) {
        Element federationUserElement =
                iq.getElement().element(IqElement.FEDERATION_USER.toString());

        FederationUser federationUser =
                new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        return federationUser;
    }

    private ResourceType unmarshalResourceType(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());

        ResourceType resourceType =
                new Gson().fromJson(orderTypeElementRequest.getText(), ResourceType.class);

        return resourceType;
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        String orderId = orderIdElement.getText();
        return orderId;
    }
}
