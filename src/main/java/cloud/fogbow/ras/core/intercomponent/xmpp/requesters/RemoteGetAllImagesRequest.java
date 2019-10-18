package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageSummary;
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

public class RemoteGetAllImagesRequest implements RemoteRequest<List<ImageSummary>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequest.class);

    private String provider;
    private String cloudName;
    private SystemUser systemUser;

    public RemoteGetAllImagesRequest(String provider, String cloudName, SystemUser systemUser) {
        this.provider = provider;
        this.cloudName = cloudName;
        this.systemUser = systemUser;
    }

    @Override
    public List<ImageSummary> send() throws Exception {
        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.cloudName, this.systemUser);
        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        LOGGER.debug(Messages.Info.SUCCESS);
        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, String cloudName, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element cloudNameElement = queryElement.addElement(IqElement.CLOUD_NAME.toString());
        cloudNameElement.setText(cloudName);

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        return iq;
    }

    private List<ImageSummary> unmarshalImages(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String hashMapStr = queryElement.element(IqElement.IMAGE_SUMMARY_LIST.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGE_SUMMARY_LIST_CLASS_NAME.toString()).getText();

        List<ImageSummary> imageSummaryList;

        try {
            imageSummaryList = (List<ImageSummary>) new Gson().fromJson(hashMapStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return imageSummaryList;
    }
}
