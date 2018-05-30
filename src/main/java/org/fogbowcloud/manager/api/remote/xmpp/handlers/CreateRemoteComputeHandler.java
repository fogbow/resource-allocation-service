package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.RemoteFacade;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class CreateRemoteComputeHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(CreateRemoteComputeHandler.class);
    private RemoteFacade remoteFacade;

    public CreateRemoteComputeHandler() {
        super(RemoteMethod.CREATE_REMOTE_ORDER.toString());
        remoteFacade = RemoteFacade.getInstance();
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info("Received request for order: " + iq.getID());
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderStr = orderElement.getText();

        Gson gson = new Gson();
        ComputeOrder order = createOrderFromJson(orderStr, gson);

        IQ response = IQ.createResultIQ(iq);

        try {
            remoteFacade.createCompute(order);
        } catch (UnauthorizedException e) {
            LOGGER.error("The user is not authorized to create order: " + order.getId());
            response.setError(PacketError.Condition.forbidden);
        } catch (OrderManagementException e) {
            LOGGER.error("The id is duplicated.");
            response.setError(PacketError.Condition.bad_request);
        }

        response.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.CREATE_REMOTE_ORDER.toString());

        return response;
    }

    private ComputeOrder createOrderFromJson(String orderStr, Gson gson) {
        ComputeOrder order = gson.fromJson(orderStr, ComputeOrder.class);
        ComputeOrder newOrder = ComputeOrder.from(order);
        return newOrder;
    }
}
