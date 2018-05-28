package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class CreateRemoteOrderHandler extends AbstractQueryHandler {

    public CreateRemoteOrderHandler() {
        super(RemoteMethod.CREATE_REMOTE_ORDER.name());
    }

    @Override
    public IQ handle(IQ iq) {

        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String newElement = orderElement.element("newElement").getText();

        IQ response = IQ.createResultIQ(iq);

        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.CREATE_REMOTE_ORDER.toString());

        Element responseElement = queryEl.addElement("response");
        if(newElement.equals("test")){
            responseElement.setText("Okay.");
        } else {
            responseElement.setText("Wrong msg.");
        }

        return response;
    }
}
