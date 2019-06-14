package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetCloudNamesRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetCloudNamesRequestHandler.class);

    private static final String REMOTE_GET_CLOUD_NAMES = RemoteMethod.REMOTE_GET_CLOUD_NAMES.toString();

    public RemoteGetCloudNamesRequestHandler() {
        super(REMOTE_GET_CLOUD_NAMES);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        SystemUser systemUser = unmarshalFederationUser(iq);
        IQ response = IQ.createResultIQ(iq);

        try {
            List<String> cloudNames = RemoteFacade.getInstance().getCloudNames(iq.getFrom().toBareJID(),
                    systemUser);
            updateResponse(response, cloudNames);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private SystemUser unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        SystemUser systemUser = new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
        return systemUser;
    }

    private void updateResponse(IQ response, List<String> cloudNames) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_CLOUD_NAMES);
        Element cloudNamesListElement = queryEl.addElement(IqElement.CLOUD_NAMES_LIST.toString());

        Element cloudNamesListClassNameElement = queryEl.addElement(IqElement.CLOUD_NAMES_LIST_CLASS_NAME.toString());
        cloudNamesListClassNameElement.setText(cloudNames.getClass().getName());

        cloudNamesListElement.setText(new Gson().toJson(cloudNames));
    }
}
