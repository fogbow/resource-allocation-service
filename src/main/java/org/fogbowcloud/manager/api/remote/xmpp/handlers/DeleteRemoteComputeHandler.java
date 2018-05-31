package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.RemoteFacade;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class DeleteRemoteComputeHandler extends AbstractQueryHandler {


    private RemoteFacade remoteFacade;

    public DeleteRemoteComputeHandler() {
        super(RemoteMethod.DELETE_REMOTE_ORDER.toString());
        remoteFacade = RemoteFacade.getInstance();
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element remoteOrderIdElement = queryElement.element(IqElement.REMOTE_ORDER_ID.toString());
        String orderId = remoteOrderIdElement.getText();

        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            remoteFacade.deleteCompute(orderId, federationUser);
        } catch (OrderManagementException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (UnauthorizedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (UnauthenticatedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        }

        response.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.DELETE_REMOTE_ORDER.toString());

        return response;
    }
}
