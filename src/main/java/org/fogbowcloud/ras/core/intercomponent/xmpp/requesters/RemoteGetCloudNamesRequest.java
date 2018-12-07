package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetCloudNamesRequest implements RemoteRequest<List<String>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetCloudNamesRequest.class);

    private String provider;
    private FederationUserToken federationUserToken;

    public RemoteGetCloudNamesRequest(String provider, FederationUserToken federationUserToken) {
        this.provider = provider;
        this.federationUserToken = federationUserToken;
    }

    @Override
    public List<String> send() throws Exception {
        IQ iq = RemoteGetCloudNamesRequest.marshal(this.provider, this.federationUserToken);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, FederationUserToken federationUserToken) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_CLOUD_NAMES.toString());

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

        return iq;
    }

    private List<String> unmarshalImages(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String listStr = queryElement.element(IqElement.CLOUD_NAMES_LIST.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.CLOUD_NAMES_LIST_CLASS_NAME.toString()).getText();

        List<String> cloudNamesList;

        try {
            cloudNamesList = (List<String>) new Gson().fromJson(listStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return cloudNamesList;
    }
}
