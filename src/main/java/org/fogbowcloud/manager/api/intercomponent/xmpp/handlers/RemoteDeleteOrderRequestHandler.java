package org.fogbowcloud.manager.api.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteDeleteOrderRequestHandler extends AbstractQueryHandler {

    private RemoteFacade remoteFacade;

    public RemoteDeleteOrderRequestHandler() {
        super(RemoteMethod.REMOTE_DELETE_ORDER.toString());
        remoteFacade = RemoteFacade.getInstance();
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element remoteOrderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        String orderId = remoteOrderIdElement.getText();

        Element orderTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());
        InstanceType instanceType = new Gson().fromJson(orderTypeElementRequest.getText(), InstanceType.class);
        
        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            this.remoteFacade.deleteOrder(orderId, federationUser, instanceType);
        } catch (OrderManagementException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (UnauthorizedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        }

        return response;
    }
}
