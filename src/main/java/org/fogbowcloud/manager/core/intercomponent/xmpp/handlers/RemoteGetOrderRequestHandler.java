package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteGetOrderRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetOrderRequestHandler.class);

    public static final String REMOTE_GET_INSTANCE = RemoteMethod.REMOTE_GET_ORDER.toString();

    public RemoteGetOrderRequestHandler() {
        super(REMOTE_GET_INSTANCE);
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        String orderId = orderIdElement.getText();

        Element orderTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());
        InstanceType instanceType = new Gson().fromJson(orderTypeElementRequest.getText(), InstanceType.class);
        
        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            Instance instance = RemoteFacade.getInstance().getResourceInstance(orderId, federationUser, instanceType);
            
            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_INSTANCE);
            Element instanceElement = queryEl.addElement(IqElement.INSTANCE.toString());
            
            Element instanceClassNameElement = queryEl.addElement(IqElement.INSTANCE_CLASS_NAME.toString());
            instanceClassNameElement.setText(instance.getClass().getName());
            
            instanceElement.setText(new Gson().toJson(instance));
        } catch (PropertyNotSpecifiedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (RequestException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (InstanceNotFoundException e) {
            LOGGER.error("The instance does not exist.", e);
            response.setError(PacketError.Condition.item_not_found);
        } catch (TokenCreationException e) {
            LOGGER.error("Error while creating token", e);
            response.setError(PacketError.Condition.service_unavailable);
        } catch (UnauthorizedException e) {
            LOGGER.error("The user is not authorized to get the instance.", e);
            response.setError(PacketError.Condition.forbidden);
        } finally {
            return response;
        }
    }

}
