package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

import java.util.Map;

public class RemoteGetAllImagesRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequestHandler.class);

    public static final String REMOTE_GET_ALL_IMAGES = RemoteMethod.REMOTE_GET_ALL_IMAGES.toString();

    public RemoteGetAllImagesRequestHandler() {
        super(REMOTE_GET_ALL_IMAGES);
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element memberIdElement = queryElement.element(IqElement.MEMBER_ID.toString());
        String memberId = memberIdElement.getText();

        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUserToken federationUserToken = new Gson().fromJson(federationUserElement.getText(), FederationUserToken.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            Map<String, String> imagesMap = RemoteFacade.getInstance().getAllImages(memberId, federationUserToken);

            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_IMAGES);
            Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

            Element imagesMapClassNameElement = queryEl.addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
            imagesMapClassNameElement.setText(imagesMap.getClass().getName());

            imagesMapElement.setText(new Gson().toJson(imagesMap));
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }
}