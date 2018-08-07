package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteGetOrderRequest implements RemoteRequest<Instance> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetOrderRequest.class);

    private Order order;

    public RemoteGetOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Instance send() throws Exception {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvidingMember());
        Instance instance = getInstanceFromResponse(response);
        return instance;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.order.getProvidingMember());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(this.order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(this.order.getType().toString());
        
        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.order.getFederationUserAttributes()));
        
        return iq;
    }

    private Instance getInstanceFromResponse(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String instanceStr = queryElement.element(IqElement.INSTANCE.toString()).getText();
        
        String instanceClassName = queryElement.element(IqElement.INSTANCE_CLASS_NAME.toString()).getText();
        
        Instance instance = null;
		try {
			instance = (Instance) new Gson().fromJson(instanceStr, Class.forName(instanceClassName));
		} catch (Exception e) {
			throw new UnexpectedException(e.getMessage());
		}
        
        return instance;
    }
}
