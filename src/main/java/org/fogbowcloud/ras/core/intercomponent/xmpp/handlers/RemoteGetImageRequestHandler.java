package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGetImageRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequestHandler.class);

    private static final String REMOTE_GET_IMAGE = RemoteMethod.REMOTE_GET_IMAGE.toString();

    public RemoteGetImageRequestHandler() {
        super(REMOTE_GET_IMAGE);
    }

    @Override
    public IQ handle(IQ iq) {
        String imageId = unmarshalImageId(iq);
        String memberId = unmarshalMemberId(iq);
        FederationUserToken federationUserToken = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Image image = RemoteFacade.getInstance().getImage(iq.getFrom().toBareJID(), memberId, imageId,
                    federationUserToken);
            updateResponse(response, image);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private void updateResponse(IQ response, Image image) {
        Element queryEl = response.getElement()
                .addElement(IqElement.QUERY.toString(), REMOTE_GET_IMAGE);
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());

        Element imageClassNameElement = queryEl
                .addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText(image.getClass().getName());

        imageElement.setText(new Gson().toJson(image));
    }

    private String unmarshalImageId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element imageIdElementRequest = queryElement.element(IqElement.IMAGE_ID.toString());
        return imageIdElementRequest.getText();
    }

    private String unmarshalMemberId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element memberIdElement = queryElement.element(IqElement.MEMBER_ID.toString());
        return memberIdElement.getText();
    }

    private FederationUserToken unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element federationUserTokenElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        return new Gson().fromJson(federationUserTokenElement.getText(), FederationUserToken.class);
    }
}