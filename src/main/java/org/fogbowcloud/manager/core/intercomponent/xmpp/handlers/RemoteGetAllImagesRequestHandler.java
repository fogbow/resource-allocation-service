package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

import java.util.Map;

public class RemoteGetAllImagesRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequestHandler.class);

    private static final String REMOTE_GET_ALL_IMAGES = RemoteMethod.REMOTE_GET_ALL_IMAGES.toString();

    public RemoteGetAllImagesRequestHandler() {
        super(REMOTE_GET_ALL_IMAGES);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug("Received request for order: " + iq.getID());

        String memberId = unmarshalMemberId(iq);
        FederationUser federationUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Map<String, String> imagesMap = RemoteFacade.getInstance().getAllImages(memberId, federationUser);
            updateResponse(response, imagesMap);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private String unmarshalMemberId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element memberIdElement = queryElement.element(IqElement.MEMBER_ID.toString());
        String memberId = memberIdElement.getText();
        return memberId;
    }

    private FederationUser unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);
        return federationUser;
    }

    private void updateResponse(IQ response, Map<String, String> imagesMap) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_IMAGES);
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(imagesMap.getClass().getName());

        imagesMapElement.setText(new Gson().toJson(imagesMap));
    }

}
