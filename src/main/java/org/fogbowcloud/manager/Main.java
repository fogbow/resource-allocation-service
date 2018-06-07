package org.fogbowcloud.manager;

import org.fogbowcloud.manager.api.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.api.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.api.intercomponent.xmpp.XmppComponentManager;
import org.fogbowcloud.manager.core.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Main implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {

        PluginInstantiationService instantiationInitService = PluginInstantiationService.getInstance();

        // Setting up cloud plugins
        CloudPluginsHolder cloudPluginsHolder = new CloudPluginsHolder(instantiationInitService);

        // Setting up behavior plugins
        BehaviorPluginsHolder behaviorPluginsHolder = new BehaviorPluginsHolder(instantiationInitService);

        // Setting up controllers, application and remote facades
        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        AaController aaController =
                new AaController(cloudPluginsHolder.getLocalIdentityPlugin(), behaviorPluginsHolder);
        OrderController orderController = new OrderController(localMemberId);
        UserQuotaController userQuotaController = new UserQuotaController();

        ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
        RemoteFacade remoteFacade = RemoteFacade.getInstance();
        applicationFacade.setAaController(aaController);
        applicationFacade.setOrderController(orderController);
        applicationFacade.setUserQuotaController(userQuotaController);
        remoteFacade.setAaController(aaController);
        remoteFacade.setOrderController(orderController);
        remoteFacade.setUserQuotaController(userQuotaController);

        // Setting up xmpp packet sender and cloud connector's factory
        String xmppJid = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY);
        String xmppPassword = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_PASSWORD_KEY);
        String xmppServerIp = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_SERVER_IP_KEY);
        int xmppServerPort =
                Integer.parseInt(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_SERVER_PORT_KEY));
        long xmppTimeout =
                Long.parseLong(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_TIMEOUT_KEY));

        XmppComponentManager xmppComponentManager = new XmppComponentManager(xmppJid, xmppPassword, xmppServerIp,
                xmppServerPort, xmppTimeout);

        PacketSenderHolder.init(xmppComponentManager);

        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
        cloudConnectorFactory.setLocalMemberId(localMemberId);
        cloudConnectorFactory.setAaController(aaController);
        cloudConnectorFactory.setOrderController(orderController);
        cloudConnectorFactory.setCloudPluginsHolder(cloudPluginsHolder);

        // Setting up order processors and starting threads
        ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(localMemberId);

    }

}
