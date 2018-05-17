package org.fogbowcloud.manager.core;

import java.util.Properties;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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
        this.properties = this.instantiationInitService.getProperties();

        InstanceProvider localInstanceProvider = new LocalInstanceProvider();;
        InstanceProvider remoteInstanceProvider = new RemoteInstanceProvider();
        ComputePlugin computePlugin = this.instantiationInitService.getComputePlugin();
        IdentityPlugin localIdentityPlugin = this.instantiationInitService .getLocalIdentityPlugin();
        IdentityPlugin federationIdentityPlugin = this.instantiationInitService.getFederationIdentityPlugin();
        AuthorizationPlugin authorizationPlugin = this.instantiationInitService.getAuthorizationPlugin();
        this.managerController = new ManagerController(this.properties, localInstanceProvider, remoteInstanceProvider,
                computePlugin, localIdentityPlugin, federationIdentityPlugin);

        this.authService = new AuthenticationService(federationIdentityPlugin, localIdentityPlugin,
        		authorizationPlugin, this.properties);

        this.facade.setAuthenticationController(this.authService);
        this.facade.setManagerController(this.managerController);
    }
}