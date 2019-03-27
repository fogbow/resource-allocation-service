package cloud.fogbow.ras;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.plugins.authorization.AuthorizationPluginInstantiator;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.datastore.services.AuditableOrderStateChangeService;
import cloud.fogbow.ras.core.datastore.services.AuditableRequestService;
import cloud.fogbow.ras.core.datastore.services.RecoveryService;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.apache.log4j.Logger;
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

    @Autowired
    private AuditableRequestService auditableRequestService;

    @Autowired
    private AuditableOrderStateChangeService auditableOrderStateChangeService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Getting the name of the local member
            String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);

            // Setting up stable storage
            DatabaseManager.getInstance().setRecoveryService(recoveryService);
            DatabaseManager.getInstance().setAuditableOrderStateChangeService(auditableOrderStateChangeService);
            DatabaseManager.getInstance().setAuditableRequestService(auditableRequestService);

            // Setting up asymmetric cryptography
            String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
            String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
            ServiceAsymmetricKeysHolder.getInstance().setPublicKeyFilePath(publicKeyFilePath);
            ServiceAsymmetricKeysHolder.getInstance().setPrivateKeyFilePath(privateKeyFilePath);

            // Setting up controllers and application facade
            String className = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
            AuthorizationPlugin authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin(className);
            AuthorizationController authorizationController =  new AuthorizationController(authorizationPlugin);
            OrderController orderController = new OrderController();
            SecurityRuleController securityRuleController = new SecurityRuleController();
            CloudListController cloudListController = new CloudListController();
            ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
            RemoteFacade remoteFacade = RemoteFacade.getInstance();
            applicationFacade.setAuthorizationController(authorizationController);

            applicationFacade.setOrderController(orderController);
            applicationFacade.setCloudListController(cloudListController);
            applicationFacade.setSecurityRuleController(securityRuleController);
            remoteFacade.setSecurityRuleController(securityRuleController);
            remoteFacade.setAuthorizationController(authorizationController);
            remoteFacade.setOrderController(orderController);
            remoteFacade.setCloudListController(cloudListController);

            // Setting up order processors
            ProcessorsThreadController processorsThreadController = new
                    ProcessorsThreadController(localMemberId, orderController);

            // Starting threads
            processorsThreadController.startRasThreads();

            // Starting PacketSender
            while (true) {
                try {
                    PacketSenderHolder.init();
                    break;
                } catch (IllegalStateException e1) {
                    LOGGER.error(Messages.Error.NO_PACKET_SENDER, e1);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }
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
