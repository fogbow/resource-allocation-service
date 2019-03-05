package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import java.util.Map;

public class RemoteGetAllImagesRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequestHandler.class);

    private static final String REMOTE_GET_ALL_IMAGES = RemoteMethod.REMOTE_GET_ALL_IMAGES.toString();

    public RemoteGetAllImagesRequestHandler() {
        super(REMOTE_GET_ALL_IMAGES);
    }

    @Override
    public IQ handle(IQ iq) {
        String cloudName = unmarshalCloudName(iq);
        SystemUser systemUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Map<String, String> imagesMap = RemoteFacade.getInstance().getAllImages(iq.getFrom().toBareJID(),
                    cloudName, systemUser);
            updateResponse(response, imagesMap);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
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
        SystemUser systemUser = new Gson().fromJson(federationUserElement.getText(), SystemUser.class);
        return systemUser;
    }

    private void updateResponse(IQ response, Map<String, String> imagesMap) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_IMAGES);
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(imagesMap.getClass().getName());

        imagesMapElement.setText(new Gson().toJson(imagesMap));
    }
}
