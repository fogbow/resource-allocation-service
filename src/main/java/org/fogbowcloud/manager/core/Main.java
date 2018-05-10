package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.logging.Logger;

@Component
public class Main implements ApplicationRunner {

    @Autowired
    private InstantiationInitService instantiationInitService;

    private Properties properties;

    private ApplicationController facade;
    private ManagerController managerController;
    private InstanceProvider localInstanceProvider;
    private InstanceProvider removeInstanceProvider;
    private AuthenticationService authService;
    private IdentityPlugin federationIdentityPlugin;

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void run(ApplicationArguments args) throws Exception {

        properties = instantiationInitService.getProperties();

        localInstanceProvider = new LocalInstanceProvider();
        removeInstanceProvider = new RemoteInstanceProvider();
        managerController = new ManagerController(properties, localInstanceProvider, removeInstanceProvider);
        managerController.setComputePlugin(instantiationInitService.getComputePlugin());
        managerController.setLocalIdentityPlugin(instantiationInitService .getLocalIdentityPlugin());
        managerController.setFederationIdentityPlugin(instantiationInitService.getFederationIdentityPlugin());

        federationIdentityPlugin = new LdapIdentityPlugin(properties);
        authService = new AuthenticationService(federationIdentityPlugin);

        // TODO: change to use getInstance method since AppCtrl will be Singleton
        facade = new ApplicationController();
    }

}