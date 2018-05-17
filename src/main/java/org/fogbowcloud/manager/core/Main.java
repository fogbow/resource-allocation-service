package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class Main implements ApplicationRunner {

    private InstantiationInitService instantiationInitService;

    private Properties properties;

    private ApplicationController facade = ApplicationController.getInstance();
    private ManagerController managerController;
    private AuthenticationService authService;

    @Override
    public void run(ApplicationArguments args) {

        this.instantiationInitService = new InstantiationInitService();
        this.properties = instantiationInitService.getProperties();

        InstanceProvider localInstanceProvider = new LocalInstanceProvider();;
        InstanceProvider remoteInstanceProvider = new RemoteInstanceProvider();
        ComputePlugin computePlugin = instantiationInitService.getComputePlugin();
        IdentityPlugin localIdentityPlugin = instantiationInitService .getLocalIdentityPlugin();
        IdentityPlugin federationIdentityPlugin = instantiationInitService.getFederationIdentityPlugin();
        this.managerController = new ManagerController(this.properties, localInstanceProvider, remoteInstanceProvider,
                computePlugin, localIdentityPlugin, federationIdentityPlugin);

        this.authService = new AuthenticationService(federationIdentityPlugin);

        this.facade.setAuthenticationController(this.authService);
    }
}