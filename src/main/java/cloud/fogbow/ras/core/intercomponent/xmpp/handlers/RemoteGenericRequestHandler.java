package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGenericRequestHandler extends AbstractQueryHandler {
    private final Logger LOGGER = Logger.getLogger(RemoteGenericRequestHandler.class);

    private static final String REMOTE_GENERIC_REQUEST = RemoteMethod.REMOTE_GENERIC_REQUEST.toString();

    public RemoteGenericRequestHandler() {
        super(REMOTE_GENERIC_REQUEST);
    }

    @Override
    public IQ handle(IQ iq) {
        String cloudName = unmarshalCloudName(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            GenericRequest genericRequest = unmarshalGenericRequest(iq);
            FederationUser federationUser = unmarshalFederationUser(iq);

            GenericRequestResponse genericRequestResponse = RemoteFacade.getInstance().
                    genericRequest(iq.getFrom().toBareJID(), cloudName, genericRequest, federationUser);
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

    private FederationUser unmarshalFederationUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = GsonHolder.getInstance().fromJson(federationUserElement.getText(),
                FederationUser.class);
        return federationUser;
    }

    private GenericRequest unmarshalGenericRequest(IQ iq) throws UnexpectedException {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element genericRequestElement = queryElement.element(IqElement.GENERIC_REQUEST.toString());
        Element genericRequestClassNameElement = queryElement.element(IqElement.GENERIC_REQUEST_CLASS_NAME.toString());

        try {
            return  (GenericRequest) GsonHolder.getInstance().fromJson(genericRequestElement.getText(),
                    Class.forName(genericRequestClassNameElement.getText()));
        } catch (ClassNotFoundException|ClassCastException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    private void updateResponse(IQ response, GenericRequestResponse genericRequestResponse) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GENERIC_REQUEST);
        Element genericRequestElement = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE.toString());
        Element genericRequestElementClassname = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE_CLASS_NAME.toString());

        genericRequestElement.setText(GsonHolder.getInstance().toJson(genericRequestResponse));
        genericRequestElementClassname.setText(genericRequestResponse.getClass().getName());
    }
}
