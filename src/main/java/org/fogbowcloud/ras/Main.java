package org.fogbowcloud.ras;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.*;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.datastore.orderstorage.RecoveryService;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppComponentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xmpp.component.ComponentException;

@Component
public class Main implements ApplicationRunner {
    private final Logger LOGGER = Logger.getLogger(Main.class);

    @Autowired
    private RecoveryService recoveryService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Setting up stable storage
            DatabaseManager.getInstance().setRecoveryService(recoveryService);

            // Setting up plugins
            PluginInstantiator instantiationInitService = PluginInstantiator.getInstance();
            InteroperabilityPluginsHolder interoperabilityPluginsHolder = new InteroperabilityPluginsHolder(instantiationInitService);
            AaaPluginsHolder aaaPluginsHolder = new AaaPluginsHolder(instantiationInitService);

            // Setting up controllers, application and remote facades
            String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
            AaaController aaaController = new AaaController(aaaPluginsHolder);
            OrderController orderController = new OrderController();
            ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
            RemoteFacade remoteFacade = RemoteFacade.getInstance();
            applicationFacade.setAaaController(aaaController);
            applicationFacade.setOrderController(orderController);
            remoteFacade.setAaaController(aaaController);
            remoteFacade.setOrderController(orderController);

            // Setting up xmpp packet sender and cloud connector's factory
            String xmppJid = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY);
            String xmppPassword = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_PASSWORD_KEY);
            String xmppServerIp = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_SERVER_IP_KEY);
            int xmppServerPort = Integer.parseInt(PropertiesHolder.getInstance().
                    getProperty(ConfigurationConstants.XMPP_C2S_PORT_KEY));
            long xmppTimeout =
                    Long.parseLong(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_TIMEOUT_KEY,
                            DefaultConfigurationConstants.XMPP_TIMEOUT));
            XmppComponentManager xmppComponentManager = new XmppComponentManager(xmppJid, xmppPassword, xmppServerIp,
                    xmppServerPort, xmppTimeout);
            xmppComponentManager.connect();
            PacketSenderHolder.init(xmppComponentManager);
            CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
            cloudConnectorFactory.setLocalMemberId(localMemberId);
            cloudConnectorFactory.setMapperPlugin(aaaPluginsHolder.getFederationToLocalMapperPlugin());
            cloudConnectorFactory.setInteroperabilityPluginsHolder(interoperabilityPluginsHolder);

            // Setting up order processors
            ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(localMemberId);

            // Starting threads
            processorsThreadController.startRasThreads();
        } catch (FatalErrorException errorException) {
            LOGGER.fatal(errorException.getMessage(), errorException);
            tryExit();
        } catch (ComponentException componentException) {
            LOGGER.fatal("Unable to connect to XMPP, check XMPP configuration file.", componentException);
            tryExit();
        }
    }

    private void tryExit(){
        if(!Boolean.parseBoolean(System.getenv("SKIP_TEST_ON_TRAVIS")))
            System.exit(1);
    }
}
