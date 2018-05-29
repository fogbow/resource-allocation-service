package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.RemoteFacade;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class GetRemoteComputeHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(GetRemoteComputeHandler.class);

    public static final String GET_REMOTE_INSTANCE = RemoteMethod.GET_REMOTE_INSTANCE.toString();

    public GetRemoteComputeHandler() {
        super(GET_REMOTE_INSTANCE);
    }

    @Override
    public IQ handle(IQ iq) {
        IQ response = IQ.createResultIQ(iq);

        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element remoteOrderIdElement = queryElement.element(IqElement.REMOTE_ORDER_ID.toString());
        String orderId = remoteOrderIdElement.getText();

        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        ComputeInstance compute = null;
        try {
            compute = RemoteFacade.getInstance().getCompute(orderId, federationUser);

            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), GET_REMOTE_INSTANCE);
            Element instanceElement = queryEl.addElement(IqElement.INSTANCE.toString());
            instanceElement.setText(new Gson().toJson(compute));
        } catch (UnauthenticatedException e) {
            LOGGER.error("", e);
        } catch (PropertyNotSpecifiedException e) {
            LOGGER.error("", e);
        } catch (RequestException e) {
            LOGGER.error("", e);
        } catch (InstanceNotFoundException e) {
            LOGGER.error("", e);
        } catch (TokenCreationException e) {
            LOGGER.error("", e);
        } catch (UnauthorizedException e) {
            LOGGER.error("", e);
        } finally {
            return response;
        }
    }

}
