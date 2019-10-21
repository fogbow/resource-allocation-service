package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
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
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        String imageId = unmarshalImageId(iq);
        String cloudName = unmarshalCloudName(iq);
        SystemUser systemUser = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            ImageInstance imageInstance = RemoteFacade.getInstance().getImage(senderId, cloudName, imageId, systemUser);
            updateResponse(response, imageInstance);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private void updateResponse(IQ response, ImageInstance imageInstance) {
        Element queryEl = response.getElement()
                .addElement(IqElement.QUERY.toString(), REMOTE_GET_IMAGE);
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());

        Element imageClassNameElement = queryEl
                .addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText(imageInstance.getClass().getName());

        imageElement.setText(new Gson().toJson(imageInstance));
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

        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        return new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
    }
}