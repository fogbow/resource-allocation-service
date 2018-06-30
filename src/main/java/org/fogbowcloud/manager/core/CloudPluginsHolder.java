package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;

public class CloudPluginsHolder {

    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final LocalIdentityPlugin localIdentityPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final ImagePlugin imagePlugin;

    public CloudPluginsHolder(PluginInstantiator instantiationInitService) {

        this.attachmentPlugin = instantiationInitService.getAttachmentPlugin();
        this.computePlugin = instantiationInitService.getComputePlugin();
        this.computeQuotaPlugin = instantiationInitService.getComputeQuotaPlugin();
        this.localIdentityPlugin = instantiationInitService.getLocalIdentityPlugin();
        this.networkPlugin = instantiationInitService.getNetworkPlugin();
        this.volumePlugin = instantiationInitService.getVolumePlugin();
        this.imagePlugin = instantiationInitService.getImagePlugin();
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
