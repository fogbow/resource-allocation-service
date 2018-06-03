package org.fogbowcloud.manager.api.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteGetUserAllocationRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserAllocationRequestHandler.class);

    public static final String REMOTE_GET_USER_ALLOCATION = RemoteMethod.REMOTE_GET_USER_ALLOCATION.toString();

    public RemoteGetUserAllocationRequestHandler() {
        super(REMOTE_GET_USER_ALLOCATION);
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            Allocation userAllocation = RemoteFacade.getInstance().getUserAllocation(federationUser);

            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_USER_ALLOCATION);
            Element instanceElement = queryEl.addElement(IqElement.USER_ALLOCATION.toString());

            Element instanceClassNameElement = queryElement.addElement(IqElement.USER_ALLOCATION_CLASS_NAME.toString());
            instanceClassNameElement.setText(userAllocation.getClass().getName());

            instanceElement.setText(new Gson().toJson(userAllocation));
        } catch (PropertyNotSpecifiedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (TokenCreationException e) {
            LOGGER.error("Error while creating token", e);
            response.setError(PacketError.Condition.service_unavailable);
        } catch (UnauthorizedException e) {
            LOGGER.error("The user is not authorized to get quota.", e);
            response.setError(PacketError.Condition.forbidden);
        } finally {
            return response;
        }
    }

}
