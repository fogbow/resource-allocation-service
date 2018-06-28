package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteGetImageRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequestHandler.class);

    public static final String REMOTE_GET_IMAGE = RemoteMethod.REMOTE_GET_IMAGE.toString();

    public RemoteGetImageRequestHandler() {
        super(REMOTE_GET_IMAGE);
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element imageIdElementRequest = queryElement.element(IqElement.IMAGE_ID.toString());
        String imageId = imageIdElementRequest.getText();

        Element memberIdElement = queryElement.element(IqElement.MEMBER_ID.toString());
        String memberId = memberIdElement.getText();

        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            Image image = RemoteFacade.getInstance().getImage(memberId, imageId, federationUser);

            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_IMAGE);
            Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());

            Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
            imageClassNameElement.setText(image.getClass().getName());

            imageElement.setText(new Gson().toJson(image));
        } catch (UnexpectedException e) {
            // TODO: Switch this error for an appropriate one.
            response.setError(PacketError.Condition.internal_server_error);
        } catch (UnavailableProviderException e) {
            LOGGER.error("Error while creating token", e);
            response.setError(PacketError.Condition.service_unavailable);
        } catch (UnauthorizedRequestException e) {
            LOGGER.error("The user is not authorized to get quota.", e);
            response.setError(PacketError.Condition.forbidden);
        } finally {
            return response;
        }
    }

}