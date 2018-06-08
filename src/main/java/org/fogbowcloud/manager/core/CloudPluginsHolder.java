package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.image.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;

public class CloudPluginsHolder {

    private AttachmentPlugin attachmentPlugin;
    private ComputePlugin computePlugin;
    private ComputeQuotaPlugin computeQuotaPlugin;
    private LocalIdentityPlugin localIdentityPlugin;
    private NetworkPlugin networkPlugin;
    private VolumePlugin volumePlugin;
    private ImagePlugin imagePlugin;

    public CloudPluginsHolder(PluginInstantiationService instantiationInitService) {

        this.attachmentPlugin = instantiationInitService.getAttachmentPlugin();
        this.computePlugin = instantiationInitService.getComputePlugin();
        this.computeQuotaPlugin = instantiationInitService.getComputeQuotaPlugin();
        this.localIdentityPlugin = instantiationInitService.getLocalIdentityPlugin();
        this.networkPlugin = instantiationInitService.getNetworkPlugin();
        this.volumePlugin = instantiationInitService.getVolumePlugin();
        this.imagePlugin = imagePlugin;
    }

    public AttachmentPlugin getAttachmentPlugin() {
        return this.attachmentPlugin;
    }

    public ComputePlugin getComputePlugin() {
        return this.computePlugin;
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        return this.computeQuotaPlugin;
    }

    public LocalIdentityPlugin getLocalIdentityPlugin() {
        return this.localIdentityPlugin;
    }

    public NetworkPlugin getNetworkPlugin() {
        return this.networkPlugin;
    }

    public VolumePlugin getVolumePlugin() {
        return this.volumePlugin;
    }

    public ImagePlugin getImagePlugin() {
        return this.imagePlugin;
    }
}
