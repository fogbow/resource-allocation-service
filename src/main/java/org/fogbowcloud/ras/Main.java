package org.fogbowcloud.ras;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.*;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
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
            AaaPluginInstantiator aaaPluginInstantiator = AaaPluginInstantiator.getInstance();
            AaaPluginsHolder aaaPluginsHolder = new AaaPluginsHolder();
            aaaPluginsHolder.setTokenGeneratorPlugin(aaaPluginInstantiator.getTokenGeneratorPlugin());
            aaaPluginsHolder.setFederationIdentityPlugin(aaaPluginInstantiator.getFederationIdentityPlugin());
            aaaPluginsHolder.setAuthenticationPlugin(aaaPluginInstantiator.getAuthenticationPlugin());
            aaaPluginsHolder.setAuthorizationPlugin(aaaPluginInstantiator.getAuthorizationPlugin());

            // Setting up controllers, application and remote facades
            String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
            AaaController aaaController = new AaaController(aaaPluginsHolder);
            OrderController orderController = new OrderController();
            SecurityRuleController securityRuleController = new SecurityRuleController();
            CloudListController cloudListController = new CloudListController();
            ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
            RemoteFacade remoteFacade = RemoteFacade.getInstance();
            applicationFacade.setAaaController(aaaController);
            applicationFacade.setOrderController(orderController);
            applicationFacade.setCloudListController(cloudListController);
            applicationFacade.setSecurityRuleController(securityRuleController);
            remoteFacade.setSecurityRuleController(securityRuleController);
            remoteFacade.setAaaController(aaaController);
            remoteFacade.setOrderController(orderController);
            remoteFacade.setCloudListController(cloudListController);

            // Setting up xmpp packet sender
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
            xmppComponentManager.connect();
            PacketSenderHolder.init(xmppComponentManager);

            // Setting up order processors
            ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(localMemberId);

            // Starting threads
            processorsThreadController.startRasThreads();
        } catch (FatalErrorException errorException) {
            LOGGER.fatal(errorException.getMessage(), errorException);
            tryExit();
        } catch (ComponentException componentException) {
            LOGGER.fatal(Messages.Fatal.UNABLE_TO_CONNECT_TO_XMPP_SERVER, componentException);
            tryExit();
        }
    }

    private void tryExit() {
        if (!Boolean.parseBoolean(System.getenv("SKIP_TEST_ON_TRAVIS")))
            System.exit(1);
    }
}
