package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteCreateOrderRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteCreateOrderRequestHandler.class);

    public RemoteCreateOrderRequestHandler() {
        super(RemoteMethod.REMOTE_CREATE_ORDER.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info("Received request for order: " + iq.getID());
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderJsonStr = orderElement.getText();

        Element orderClassNameElement = queryElement.element(IqElement.ORDER_CLASS_NAME.toString());
        String className = orderClassNameElement.getText();
        
        IQ response = IQ.createResultIQ(iq);
        
        Gson gson = new Gson();
        Order order = null;
		try {
			order = (Order) gson.fromJson(orderJsonStr, Class.forName(className));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			response.setError(PacketError.Condition.bad_request);
			return response;
		}

        try {
            RemoteFacade.getInstance().activateOrder(order);
        } catch (UnauthorizedException e) {
            LOGGER.error("The user is not authorized to create order: " + order.getId(), e);
            response.setError(PacketError.Condition.forbidden);
        } catch (OrderManagementException e) {
            LOGGER.error("The id is duplicated.", e);
            response.setError(PacketError.Condition.bad_request);
        }

        return response;
    }

}
