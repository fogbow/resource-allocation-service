package org.fogbowcloud.ras;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.*;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.datastore.orderstorage.RecoveryService;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class Main implements ApplicationRunner {
    private final Logger LOGGER = Logger.getLogger(Main.class);

    @Autowired
    private RecoveryService recoveryService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Getting the name of the local member
            String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

            // Setting up stable storage
            DatabaseManager.getInstance().setRecoveryService(recoveryService);

            // Setting up AAA plugins
            String aaaConfFilePath = HomeDir.getPath() + SystemConstants.AAA_CONF_FILE_NAME;
            AaaPluginsHolder aaaPluginsHolder = new AaaPluginsHolder();
            aaaPluginsHolder.setTokenGeneratorPlugin(AaaPluginInstantiator.getTokenGeneratorPlugin(aaaConfFilePath));
            aaaPluginsHolder.setFederationIdentityPlugin(AaaPluginInstantiator.
                    getFederationIdentityPlugin(aaaConfFilePath));
            aaaPluginsHolder.setAuthenticationPlugin(AaaPluginInstantiator.
                    getAuthenticationPlugin(aaaConfFilePath, localMemberId));
            aaaPluginsHolder.setAuthorizationPlugin(AaaPluginInstantiator.getAuthorizationPlugin(aaaConfFilePath));

            // Setting up controllers, application and remote facades
            AaaController aaaController = new AaaController(aaaPluginsHolder, localMemberId);
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

            // Starting PacketSender
            while (true) {
                try {
                    PacketSenderHolder.init();
                    break;
                } catch (IllegalStateException e1) {
                    LOGGER.error(Messages.Error.NO_PACKET_SENDER);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }

            // Setting up order processors
            ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(localMemberId);

            // Starting threads
            processorsThreadController.startRasThreads();
        } catch (FatalErrorException errorException) {
            LOGGER.fatal(errorException.getMessage(), errorException);
            tryExit();
        }
    }

    private void tryExit() {
        if (!Boolean.parseBoolean(System.getenv("SKIP_TEST_ON_TRAVIS")))
            System.exit(1);
    }
}
