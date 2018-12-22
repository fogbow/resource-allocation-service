package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.fogbowcloud.ras.util.GsonHolder;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGenericRequestHandler extends AbstractQueryHandler {

    private static final String REMOTE_GENERIC_REQUEST = RemoteMethod.REMOTE_GENERIC_REQUEST.toString();

    public RemoteGenericRequestHandler() {
        super(REMOTE_GENERIC_REQUEST);
    }

    @Override
    public IQ handle(IQ iq) {
        String cloudName = unmarshalCloudName(iq);
        GenericRequest genericRequest = unmarshalGenericRequest(iq);
        FederationUserToken federationUserToken = unmarshalFederationUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            GenericRequestResponse genericRequestResponse = RemoteFacade.getInstance().
                    genericRequest(iq.getFrom().toBareJID(), cloudName, genericRequest, federationUserToken);
            updateResponse(response, genericRequestResponse);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private String unmarshalCloudName(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element cloudNameElement = queryElement.element(IqElement.CLOUD_NAME.toString());
        String cloudName = GsonHolder.getInstance().fromJson(cloudNameElement.getText(), String.class);
        return cloudName;
    }

    private FederationUserToken unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserTokenElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUserToken federationUserToken = GsonHolder.getInstance().fromJson(federationUserTokenElement.getText(),
                FederationUserToken.class);
        return federationUserToken;
    }

    private GenericRequest unmarshalGenericRequest(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserTokenElement = queryElement.element(IqElement.GENERIC_REQUEST.toString());
        GenericRequest genericRequest = GsonHolder.getInstance().fromJson(federationUserTokenElement.getText(),
                GenericRequest.class);
        return genericRequest;
    }

    private void updateResponse(IQ response, GenericRequestResponse genericRequestResponse) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GENERIC_REQUEST);
        Element genericRequestElement = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE.toString());
        Element genericRequestElementClassname = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE_CLASS_NAME.toString());

        genericRequestElement.setText(GsonHolder.getInstance().toJson(genericRequestResponse));
        genericRequestElementClassname.setText(genericRequestResponse.getClass().getName());
    }
}
