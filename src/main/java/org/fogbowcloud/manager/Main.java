package org.fogbowcloud.manager;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.BehaviorPluginsHolder;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.ProcessorsThreadController;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppComponentManager;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xmpp.component.ComponentException;

@Component
public class Main implements ApplicationRunner {
	
	private final Logger LOGGER = Logger.getLogger(Main.class);
	
    @Override
    public void run(ApplicationArguments args) {
        HomeDir.getInstance().setPath(setHomeDirectory(args));

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

        ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
        RemoteFacade remoteFacade = RemoteFacade.getInstance();
        applicationFacade.setAaController(aaController);
        applicationFacade.setOrderController(orderController);
        remoteFacade.setAaController(aaController);
        remoteFacade.setOrderController(orderController);

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
        
        try {
			xmppComponentManager.connect();
		} catch (ComponentException e) {
			LOGGER.error("Unable to connect to XMPP, check XMPP configuration file");
			return;
		}

        PacketSenderHolder.init(xmppComponentManager);

        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
        cloudConnectorFactory.setLocalMemberId(localMemberId);
        cloudConnectorFactory.setAaController(aaController);
        cloudConnectorFactory.setOrderController(orderController);
        cloudConnectorFactory.setCloudPluginsHolder(cloudPluginsHolder);

        // Setting up order processors and starting threads
        ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(localMemberId);
        processorsThreadController.startManagerThreads();
    }

    private String setHomeDirectory(ApplicationArguments args) {
        if (args.getSourceArgs().length == 0) {
            return DefaultConfigurationConstants.FOGBOW_HOME;
        }
        String homeDir = args.getSourceArgs()[0];
        return (homeDir == null ? DefaultConfigurationConstants.FOGBOW_HOME : homeDir);
    }

}
