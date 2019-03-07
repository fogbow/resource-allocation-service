package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.api.http.response.Image;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
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
        String cloudName = unmarshalCloudName(iq);
        SystemUser systemUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Image image = RemoteFacade.getInstance().getImage(iq.getFrom().toBareJID(), cloudName, imageId,
                    systemUser);
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

    private String unmarshalCloudName(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element cloudNameElement = queryElement.element(IqElement.CLOUD_NAME.toString());
        String cloudName = new Gson().fromJson(cloudNameElement.getText(), String.class);
        return cloudName;
    }

    private SystemUser unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        return new Gson().fromJson(federationUserElement.getText(), SystemUser.class);
    }
}