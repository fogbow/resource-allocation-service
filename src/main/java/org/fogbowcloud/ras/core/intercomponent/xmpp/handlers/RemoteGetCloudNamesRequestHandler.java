package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import java.util.List;
import java.util.Map;

public class RemoteGetCloudNamesRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetCloudNamesRequestHandler.class);

    private static final String REMOTE_GET_CLOUD_NAMES = RemoteMethod.REMOTE_GET_CLOUD_NAMES.toString();

    public RemoteGetCloudNamesRequestHandler() {
        super(REMOTE_GET_CLOUD_NAMES);
    }

    @Override
    public IQ handle(IQ iq) {
        FederationUserToken federationUserToken = unmarshalFederationUser(iq);
        IQ response = IQ.createResultIQ(iq);

        try {
            List<String> cloudNames = RemoteFacade.getInstance().getCloudNames(federationUserToken);
            updateResponse(response, cloudNames);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private FederationUserToken unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserTokenElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUserToken federationUserToken = new Gson().fromJson(federationUserTokenElement.getText(),
                FederationUserToken.class);
        return federationUserToken;
    }

    private void updateResponse(IQ response, List<String> cloudNames) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_CLOUD_NAMES);
        Element cloudNamesListElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element cloudNamesListClassNameElement = queryEl.addElement(IqElement.CLOUD_NAMES_LIST_CLASS_NAME.toString());
        cloudNamesListClassNameElement.setText(cloudNames.getClass().getName());

        cloudNamesListElement.setText(new Gson().toJson(cloudNames));
    }
}
