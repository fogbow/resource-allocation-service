package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteTakeSnapshotRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(RemoteTakeSnapshotRequest.class);

    private ComputeOrder computeOrder;
    private String snapshotName;
    private SystemUser systemUser;

    public RemoteTakeSnapshotRequest(ComputeOrder computeOrder, String name, SystemUser systemUser) {
        this.computeOrder = computeOrder;
        this.snapshotName = name;
        this.systemUser = systemUser;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = marshal(this.computeOrder, this.snapshotName, this.systemUser);
        LOGGER.debug(String.format(Messages.Log.SENDING_MSG_S, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.computeOrder.getProvider());
        LOGGER.debug(Messages.Log.SUCCESS);
        return null;
    }

    public static IQ marshal(ComputeOrder computeOrder, String snapshotName, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR
                + SystemConstants.XMPP_SERVER_NAME_PREFIX + computeOrder.getProvider());
        iq.setID(computeOrder.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(computeOrder.getId());

        Element snapshotNameElement = queryElement.addElement(IqElement.SNAPSHOT_NAME.toString());
        snapshotNameElement.setText(snapshotName);

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        return iq;
    }
}
