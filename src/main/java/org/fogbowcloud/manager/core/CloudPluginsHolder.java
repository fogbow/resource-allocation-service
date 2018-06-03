package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.services.InstantiationInitService;

public class CloudPluginsHolder {

    private AttachmentPlugin attachmentPlugin;
    private ComputePlugin computePlugin;
    private ComputeQuotaPlugin computeQuotaPlugin;
    private LocalIdentityPlugin localIdentityPlugin;
    private NetworkPlugin networkPlugin;
    private VolumePlugin volumePlugin;

    public CloudPluginsHolder(InstantiationInitService instantiationInitService) {

        this.attachmentPlugin = instantiationInitService.getAttachmentPlugin();
        this.computePlugin = instantiationInitService.getComputePlugin();
        this.computeQuotaPlugin = instantiationInitService.getComputeQuotaPlugin();
        this.localIdentityPlugin = instantiationInitService.getLocalIdentityPlugin();
        this.networkPlugin = instantiationInitService.getNetworkPlugin();
        this.volumePlugin = instantiationInitService.getVolumePlugin();
    }

    public AttachmentPlugin getAttachmentPlugin() {
        return attachmentPlugin;
    }

    public ComputePlugin getComputePlugin() {
        return computePlugin;
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        return computeQuotaPlugin;
    }

    public LocalIdentityPlugin getLocalIdentityPlugin() {
        return localIdentityPlugin;
    }

    public NetworkPlugin getNetworkPlugin() {
        return networkPlugin;
    }

    public VolumePlugin getVolumePlugin() {
        return volumePlugin;
    }
}
