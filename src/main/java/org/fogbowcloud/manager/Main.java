package org.fogbowcloud.manager;

import java.util.Properties;

import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.services.AuthenticationController;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Main implements ApplicationRunner {

    private InstantiationInitService instantiationInitService;

    private Properties properties;

    private ApplicationFacade facade = ApplicationFacade.getInstance();
    private ManagerController managerController;
    private AuthenticationController authService;

    @Override
    public void run(ApplicationArguments args) {

        this.instantiationInitService = new InstantiationInitService();
        this.properties = this.instantiationInitService.getProperties();

        ComputePlugin computePlugin = this.instantiationInitService.getComputePlugin();
        IdentityPlugin localIdentityPlugin = this.instantiationInitService .getLocalIdentityPlugin();
        IdentityPlugin federationIdentityPlugin = this.instantiationInitService.getFederationIdentityPlugin();
        AuthorizationPlugin authorizationPlugin = this.instantiationInitService.getAuthorizationPlugin();
        
        InstanceProvider localInstanceProvider = new LocalInstanceProvider(computePlugin);
        InstanceProvider remoteInstanceProvider = new RemoteInstanceProvider();

        this.managerController = new ManagerController(this.properties, localInstanceProvider, remoteInstanceProvider,
                computePlugin, localIdentityPlugin, federationIdentityPlugin);

        this.authService = new AuthenticationController(federationIdentityPlugin, localIdentityPlugin,
        		authorizationPlugin, this.properties);

        this.facade.setAuthenticationController(this.authService);
    }
}