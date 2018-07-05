package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PluginInstantiator;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PluginInstantiatorTest {

    private PluginInstantiator pluginInstantiator;

    private static final String TEST_CONF_PATH = "src/test/resources/plugins_instatiator";

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath(TEST_CONF_PATH);
        this.pluginInstantiator = new PluginInstantiator();
    }

    @Test
    public void testSetUpProperties() {
        String expected_xmpp_jid_value = "fake-localidentity-member";
        Assert.assertEquals(expected_xmpp_jid_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY));
    }

    @Test
    public void testCreateAuthorizationPluginInstance() {
        String expected_authorization_class_value =
                "org.fogbowcloud.manager.core.stubs.StubAuthorizationPlugin";

        AuthorizationPlugin plugin = this.pluginInstantiator.getAuthorizationPlugin();
        Assert.assertEquals(expected_authorization_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateFederationIdentityPluginInstance() {
        String expected_federation_identity_class_value =
                "org.fogbowcloud.manager.core.stubs.StubFederationIdentityPlugin";

        FederationIdentityPlugin plugin = this.pluginInstantiator.getFederationIdentityPlugin();
        Assert.assertEquals(expected_federation_identity_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateLocalUserCredentialsMapperPluginInstance() {
        String expected_local_user_credentials_mapper_class_value =
                "org.fogbowcloud.manager.core.stubs.StubLocalUserCredentialsMapperPlugin";

        LocalUserCredentialsMapperPlugin plugin = this.pluginInstantiator.getLocalUserCredentialsMapperPlugin();
        Assert.assertEquals(expected_local_user_credentials_mapper_class_value, plugin.getClass().getName());
    }

    @Test
    public void testCreateAttachmentPlugin() {
        String expected_attachment_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubAttachmentPlugin";

        AttachmentPlugin plugin = this.pluginInstantiator.getAttachmentPlugin();
        Assert.assertEquals(expected_attachment_plugin_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateComputePlugin() {
        String expected_compute_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubComputePlugin";

        ComputePlugin plugin = this.pluginInstantiator.getComputePlugin();
        Assert.assertEquals(expected_compute_plugin_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateComputeQuotaPlugin() {
        String expected_compute_quota_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubComputeQuotaPlugin";

        ComputeQuotaPlugin plugin = this.pluginInstantiator.getComputeQuotaPlugin();
        Assert.assertEquals(expected_compute_quota_plugin_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateLocalIdentityPluginInstance() {
        String expected_local_identity_class_value =
                "org.fogbowcloud.manager.core.stubs.StubLocalIdentityPlugin";

        LocalIdentityPlugin plugin = this.pluginInstantiator.getLocalIdentityPlugin();
        Assert.assertEquals(expected_local_identity_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateNetworkPlugin() {
        String expected_network_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubNetworkPlugin";

        NetworkPlugin plugin = this.pluginInstantiator.getNetworkPlugin();
        Assert.assertEquals(expected_network_plugin_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateVolumePlugin() {
        String expected_volume_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubVolumePlugin";

        VolumePlugin plugin = this.pluginInstantiator.getVolumePlugin();
        Assert.assertEquals(expected_volume_plugin_class_value, plugin.getClass().getName());
    }
    
    @Test
    public void testCreateImagePlugin() {
        String expected_image_plugin_class_value =
                "org.fogbowcloud.manager.core.stubs.StubImagePlugin";

        ImagePlugin plugin = this.pluginInstantiator.getImagePlugin();
        Assert.assertEquals(expected_image_plugin_class_value, plugin.getClass().getName());
    }
}
