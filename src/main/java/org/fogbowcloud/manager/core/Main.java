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

import java.util.Properties;

public class Main implements ApplicationRunner {

    private InstantiationInitService instantiationInitService;

    private Properties properties;

    private ApplicationController facade;
    private ManagerController managerController;
    private InstanceProvider localInstanceProvider;
    private InstanceProvider removeInstanceProvider;
    private AuthenticationService authService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.instantiationInitService = new InstantiationInitService();
        this.properties = instantiationInitService.getProperties();

        this.localInstanceProvider = new LocalInstanceProvider();
        this.removeInstanceProvider = new RemoteInstanceProvider();

        this.managerController = new ManagerController(this.properties, this.localInstanceProvider, this.removeInstanceProvider);

        ComputePlugin computePlugin = instantiationInitService.getComputePlugin();
        this.managerController.setComputePlugin(computePlugin);

        IdentityPlugin localIdentityPlugin = instantiationInitService .getLocalIdentityPlugin();
        this.managerController.setLocalIdentityPlugin(localIdentityPlugin);

        IdentityPlugin federationIdentityPlugin = instantiationInitService.getFederationIdentityPlugin();
        this.managerController.setFederationIdentityPlugin(federationIdentityPlugin);

        this.authService = new AuthenticationService(federationIdentityPlugin);

        // TODO: change to use getInstance method since AppCtrl will be Singleton
        facade = new ApplicationController(authService, managerController);
    }
}