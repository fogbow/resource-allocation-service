package org.fogbowcloud.manager;

import org.fogbowcloud.manager.api.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.api.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.api.intercomponent.xmpp.XmppComponentManager;
import org.fogbowcloud.manager.core.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.processors.ClosedProcessor;
import org.fogbowcloud.manager.core.processors.FulfilledProcessor;
import org.fogbowcloud.manager.core.processors.OpenProcessor;
import org.fogbowcloud.manager.core.processors.SpawningProcessor;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.fogbowcloud.manager.utils.SshCommonUserUtil;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;
import org.jamppa.component.PacketSender;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Main implements ApplicationRunner {

    private ApplicationFacade applicationFacade = ApplicationFacade.getInstance();
    private RemoteFacade remoteFacade = RemoteFacade.getInstance();

    @Override
    public void run(ApplicationArguments args) {
        InstantiationInitService instantiationInitService = new InstantiationInitService();

        // Setting up cloud plugins
        CloudPluginsHolder cloudPluginsHolder = new CloudPluginsHolder(instantiationInitService);

        // Setting up behavior plugins
        BehaviorPluginsHolder behaviorPluginsHolder = new BehaviorPluginsHolder(instantiationInitService);

        // Setting up controllers and application facade
        String localMemberId = instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_JID_KEY);

        AaController aaController =
                new AaController(cloudPluginsHolder.getLocalIdentityPlugin(), behaviorPluginsHolder);
        OrderController orderController = new OrderController(localMemberId);
        UserQuotaController userQuotaController = new UserQuotaController();

        this.applicationFacade.setAaController(aaController);
        this.applicationFacade.setOrderController(orderController);
        this.applicationFacade.setUserQuotaController(userQuotaController);
        this.remoteFacade.setAaController(aaController);
        this.remoteFacade.setOrderController(orderController);
        this.remoteFacade.setUserQuotaController(userQuotaController);

        // Setting up cloud connector's factory
        String xmppPassword = instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_PASSWORD_KEY);
        String xmppServerIp = instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_SERVER_IP_KEY);
        int xmppServerPort =
                Integer.parseInt(instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_SERVER_PORT_KEY));
        long xmppTimeout =
                Long.parseLong(instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_TIMEOUT_KEY));

        PacketSenderHolder.init(localMemberId,
                xmppPassword, xmppServerIp, xmppServerPort, xmppTimeout, orderController);

        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
        cloudConnectorFactory.setLocalMemberId(localMemberId);
        cloudConnectorFactory.setAaController(aaController);
        cloudConnectorFactory.setOrderController(orderController);
        cloudConnectorFactory.setCloudPluginsHolder(cloudPluginsHolder);

        // Setting up order processors and starting threads
        String openOrdersProcSleepTimeStr =
                instantiationInitService.getPropertyValue(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY);

        OpenProcessor openProcessor = new OpenProcessor(localMemberId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr =
                instantiationInitService.getPropertyValue(ConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME_KEY);

        TunnelingServiceUtil tunnelingServiceUtil = TunnelingServiceUtil.getInstance();
        SshConnectivityUtil sshConnectivityUtil = SshConnectivityUtil.getInstance();
        tunnelingServiceUtil.setProperties(instantiationInitService.getProperties());
        SshCommonUserUtil.setProperties(instantiationInitService.getProperties());
        SshConnectivityUtil.setProperties(instantiationInitService.getProperties());

        SpawningProcessor spawningProcessor =
                new SpawningProcessor(localMemberId, tunnelingServiceUtil,
                        sshConnectivityUtil, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr =
                instantiationInitService.getPropertyValue(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY);

        FulfilledProcessor fulfilledProcessor =
                new FulfilledProcessor(localMemberId, tunnelingServiceUtil,
                        sshConnectivityUtil, fulfilledOrdersProcSleepTimeStr);

        String closedOrdersProcSleepTimeStr =
                instantiationInitService.getPropertyValue(ConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME_KEY);

        ClosedProcessor closedProcessor = new ClosedProcessor(orderController, closedOrdersProcSleepTimeStr);

        ProcessorsThreadController processorsThreadController = new ProcessorsThreadController(openProcessor,
                spawningProcessor, fulfilledProcessor, closedProcessor);

    }

}
