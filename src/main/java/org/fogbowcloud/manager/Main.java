package org.fogbowcloud.manager;

import java.util.Properties;

import org.fogbowcloud.manager.api.remote.xmpp.XmppComponentManager;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.QuotaProviderController;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.behavior.federation.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.local.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.services.AAAController;
import org.fogbowcloud.manager.core.services.InstantiationInitService;
import org.fogbowcloud.manager.core.statisticsprovider.LocalQuotaProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Main implements ApplicationRunner {

    private InstantiationInitService instantiationInitService;

    private Properties properties;

    private ApplicationFacade facade = ApplicationFacade.getInstance();
    private ProcessorController processorController;
    private AAAController aaaController;

    @Override
    public void run(ApplicationArguments args) {
        this.instantiationInitService = new InstantiationInitService();
        this.properties = this.instantiationInitService.getProperties();

        LocalIdentityPlugin localIdentityPlugin = this.instantiationInitService.getLocalIdentityPlugin();
        FederationIdentityPlugin federationIdentityPlugin =
        		this.instantiationInitService.getFederationIdentityPlugin();
        ComputePlugin computePlugin = this.instantiationInitService.getComputePlugin();
        NetworkPlugin networkPlugin = this.instantiationInitService.getNetworkPlugin();
        VolumePlugin volumePlugin = this.instantiationInitService.getVolumePlugin();
        AttachmentPlugin attachmentPlugin = this.instantiationInitService.getAttachmentPlugin();
        AuthorizationPlugin authorizationPlugin = this.instantiationInitService.getAuthorizationPlugin();
        ComputeQuotaPlugin computeQuotaPlugin = this.instantiationInitService.getComputeQuotaPlugin();

        String localMemberId = this.instantiationInitService.getPropertyValue(ConfigurationConstants.XMPP_ID_KEY);        

        this.aaaController = new AAAController(federationIdentityPlugin, localIdentityPlugin,
                authorizationPlugin, this.properties);

        LocalInstanceProvider localInstanceProvider = new LocalInstanceProvider(computePlugin,
                networkPlugin, volumePlugin, attachmentPlugin, this.aaaController);

        // FIXME retrieve from conf file
        String jid = "";
        String password = "";
        String xmppServerIp = "";
        int xmppServerPort = -1;
        long timeout = 5000L;
        XmppComponentManager xmppComponentManager = new XmppComponentManager(jid,
                password, xmppServerIp, xmppServerPort, timeout);
        RemoteInstanceProvider remoteInstanceProvider = new RemoteInstanceProvider(xmppComponentManager);
        
        this.processorController = new ProcessorController(this.properties, localInstanceProvider,
                remoteInstanceProvider, computePlugin, localIdentityPlugin,
                federationIdentityPlugin);

        
		LocalQuotaProvider localQuotaProvider = new LocalQuotaProvider(computeQuotaPlugin, this.aaaController);
        QuotaProviderController quotaProviderController = new QuotaProviderController(localQuotaProvider, localMemberId);
        
        this.facade.setAAAController(this.aaaController);
        this.facade.setOrderController(new OrderController(this.properties, localInstanceProvider,
        		remoteInstanceProvider));
        
        this.facade.setQuotaProviderController(quotaProviderController);
    }

}
