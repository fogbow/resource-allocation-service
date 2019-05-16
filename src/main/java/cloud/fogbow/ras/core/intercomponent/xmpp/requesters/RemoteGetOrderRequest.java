package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteGetOrderRequest implements RemoteRequest<Instance> {
    private Order order;

    public RemoteGetOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public OrderInstance send() throws Exception {

        IQ iq = marshal(this.order);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvider());
        OrderInstance instance = unmarshalInstance(response);
        return instance;
    }

    public static IQ marshal(Order order) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(order.getProvider());

        //user
        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(order.getSystemUser()));

        //order
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ORDER.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(order.getType().toString());

        return iq;
    }

    private OrderInstance unmarshalInstance(IQ response) throws UnexpectedException {

        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String instanceStr = queryElement.element(IqElement.INSTANCE.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.INSTANCE_CLASS_NAME.toString()).getText();

        OrderInstance instance = null;
        try {
            instance = (OrderInstance) new Gson().fromJson(instanceStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return instance;
    }
}
