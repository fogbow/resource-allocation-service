package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.jamppa.component.PacketSender;
import org.xmpp.component.ComponentException;

public class PacketSenderHolder {

    private static PacketSender packetSender = null;

    public static void init() {
        if (packetSender == null) {
            String xmppJid = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY);
            String xmppPassword = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_PASSWORD_KEY);
            String xmppServerIp = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_SERVER_IP_KEY);
            int xmppServerPort = Integer.parseInt(PropertiesHolder.getInstance().
                    getProperty(ConfigurationConstants.XMPP_C2C_PORT_KEY, DefaultConfigurationConstants.XMPP_CSC_PORT));
            long xmppTimeout =
                    Long.parseLong(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_TIMEOUT_KEY,
                            DefaultConfigurationConstants.XMPP_TIMEOUT));
            XmppComponentManager xmppComponentManager = new XmppComponentManager(xmppJid, xmppPassword, xmppServerIp,
                    xmppServerPort, xmppTimeout);
            try {
                xmppComponentManager.connect();
            } catch (ComponentException e) {
                throw new IllegalStateException(Messages.Exception.NO_PACKET_SENDER);
            }
            PacketSenderHolder.packetSender = xmppComponentManager;
        }
    }

    public static synchronized PacketSender getPacketSender() {
        init();
        return packetSender;
    }

    // Used in tests only
    public static void setPacketSender(PacketSender thePacketSender) {
        packetSender = thePacketSender;
    }
}

