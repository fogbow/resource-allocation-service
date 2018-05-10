package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class Main implements ApplicationRunner {

    @Autowired
    private InstantiationInitService instantiationInitService;

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void run(ApplicationArguments args) throws Exception {

        ManagerController facade = new ManagerController(instantiationInitService.getProperties(), null, null);

        facade.setComputePlugin(instantiationInitService.getComputePlugin());
        facade.setLocalIdentityPlugin(instantiationInitService .getLocalIdentityPlugin());
        facade.setFederationIdentityPlugin(instantiationInitService.getFederationIdentityPlugin());

        String xmppHost = instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_HOST_KEY);
        String xmppJid = instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_ID_KEY);
    }

}