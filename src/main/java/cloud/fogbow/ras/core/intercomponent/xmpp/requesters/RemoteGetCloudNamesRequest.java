package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetCloudNamesRequest implements RemoteRequest<List<String>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetCloudNamesRequest.class);

    private String provider;
    private SystemUser systemUser;

    public RemoteGetCloudNamesRequest(String provider, SystemUser systemUser) {
        this.provider = provider;
        this.systemUser = systemUser;
    }

    @Override
    public List<String> send() throws Exception {
        IQ iq = RemoteGetCloudNamesRequest.marshal(this.provider, this.systemUser);
        LOGGER.debug(String.format(Messages.Log.SENDING_MSG_S, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        LOGGER.debug(Messages.Log.SUCCESS);
        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_CLOUD_NAMES.toString());

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        return iq;
    }

    private List<String> unmarshalImages(IQ response) throws InternalServerErrorException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String listStr = queryElement.element(IqElement.CLOUD_NAMES_LIST.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.CLOUD_NAMES_LIST_CLASS_NAME.toString()).getText();

        List<String> cloudNamesList;

        try {
            cloudNamesList = (List<String>) new Gson().fromJson(listStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }

        return cloudNamesList;
    }
}
