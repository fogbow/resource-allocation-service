package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.interoperability.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HomeDir.class})
public class PluginInstantiatorTest {

    private PluginInstantiator pluginInstantiator;

    private static final String TEST_CONF_PATH = "src/test/resources/private/";

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(HomeDir.class);
        BDDMockito.given(HomeDir.getPath()).willReturn(TEST_CONF_PATH);
        this.pluginInstantiator = new PluginInstantiator();
    }

    // test case: Tests if the key xmpp_jid in ras.conf has its value as fake-localidentity-member.
    @Test
    public void testSetUpProperties() {
        // set up
        String expected_xmpp_jid_value = "fake-localidentity-member";

        // verify
        Assert.assertEquals(expected_xmpp_jid_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY));
    }

    // test case: Tests if getTokenGeneratorPlugin() returns StubTokenGeneratorPlugin
    // as the plugin class name.
    @Test
    public void testCreateTokenGeneratorPluginInstance() {
        // set up
        String expected_tokengenerator_wrapper_class_value =
                "org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper";
        String expected_tokengenerator_class_value =
                "org.fogbowcloud.ras.core.stubs.StubTokenGeneratorPlugin";

        // exercise
        TokenGeneratorPluginProtectionWrapper plugin = (TokenGeneratorPluginProtectionWrapper)
                this.pluginInstantiator.getTokenGeneratorPlugin();

        // verify
        Assert.assertEquals(expected_tokengenerator_wrapper_class_value, plugin.getClass().getName());
        Assert.assertEquals(expected_tokengenerator_class_value, plugin.getEmbeddedPlugin().getClass().getName());
    }


    // test case: Tests if getFederationIdentityPlugin() returns StubFederationIdentityPlugin
    // as the plugin class name.
    @Test
    public void testCreateFederationIdentityPluginInstance() {
        // set up
        String expected_federation_identity_wrapper_class_value =
                "org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPluginProtectionWrapper";
        String expected_federation_identity_class_value =
                "org.fogbowcloud.ras.core.stubs.StubFederationIdentityPlugin";

        // exercise
        FederationIdentityPluginProtectionWrapper plugin = (FederationIdentityPluginProtectionWrapper)
                this.pluginInstantiator.getFederationIdentityPlugin();

        // verify
        Assert.assertEquals(expected_federation_identity_wrapper_class_value, plugin.getClass().getName());
        Assert.assertEquals(expected_federation_identity_class_value, plugin.getEmbeddedPlugin().getClass().getName());
    }

    // test case: Tests if getAuthenticationPlugin() returns StubAuthenticationPlugin
    // as the plugin class name.
    @Test
    public void testCreateAuthenticationPluginInstance() {
        // set up
        String expected_authentication_class_value =
                "org.fogbowcloud.ras.core.stubs.StubAuthenticationPlugin";

        // exercise
        AuthenticationPlugin plugin = this.pluginInstantiator.getAuthenticationPlugin();

        // verify
        Assert.assertEquals(expected_authentication_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getAuthorizationPlugin() returns StubAuthorizationPlugin as the plugin class name.
    @Test
    public void testCreateAuthorizationPluginInstance() {
        // set up
        String expected_authorization_class_value =
                "org.fogbowcloud.ras.core.stubs.StubAuthorizationPlugin";

        // exercise
        AuthorizationPlugin plugin = this.pluginInstantiator.getAuthorizationPlugin();

        // verify
        Assert.assertEquals(expected_authorization_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getFederationToLocalMapperPlugin() returns StubFederationToLocalMapperPlugin
    // as the plugin class name.
    @Test
    public void testCreateLocalUserCredentialsMapperPluginInstance() {
        // set up
        String expected_local_user_credentials_mapper_class_value =
                "org.fogbowcloud.ras.core.stubs.StubFederationToLocalMapperPlugin";

        // exercise
        FederationToLocalMapperPlugin plugin = this.pluginInstantiator.getLocalUserCredentialsMapperPlugin();

        // verify
        Assert.assertEquals(expected_local_user_credentials_mapper_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getAttachmentPlugin() returns StubAttachmentPlugin as the plugin class name.
    @Test
    public void testCreateAttachmentPlugin() {
        // set up
        String expected_attachment_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubAttachmentPlugin";

        // exercise
        AttachmentPlugin plugin = this.pluginInstantiator.getAttachmentPlugin();

        // verify
        Assert.assertEquals(expected_attachment_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getComputePlugin() returns StubComputePlugin as the plugin class name.
    @Test
    public void testCreateComputePlugin() {
        // set up
        String expected_compute_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubComputePlugin";

        // exercise
        ComputePlugin plugin = this.pluginInstantiator.getComputePlugin();

        // verify
        Assert.assertEquals(expected_compute_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getComputeQuotaPlugin() returns StubComputeQuotaPlugin as the plugin class name.
    @Test
    public void testCreateComputeQuotaPlugin() {
        // set up
        String expected_compute_quota_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubComputeQuotaPlugin";

        // exercise
        ComputeQuotaPlugin plugin = this.pluginInstantiator.getComputeQuotaPlugin();

        // verify
        Assert.assertEquals(expected_compute_quota_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getNetworkPlugin() returns StubNetworkPlugin as the plugin class name.
    @Test
    public void testCreateNetworkPlugin() {
        // set up
        String expected_network_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubNetworkPlugin";

        // exercise
        NetworkPlugin plugin = this.pluginInstantiator.getNetworkPlugin();

        // verify
        Assert.assertEquals(expected_network_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getVolumePlugin() returns StubVolumePlugin as the plugin class name.
    @Test
    public void testCreateVolumePlugin() {
        // set up
        String expected_volume_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubVolumePlugin";

        // exercise
        VolumePlugin plugin = this.pluginInstantiator.getVolumePlugin();

        // verify
        Assert.assertEquals(expected_volume_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getImagePlugin() returns StubImagePlugin as the plugin class name.
    @Test
    public void testCreateImagePlugin() {
        // set up
        String expected_image_plugin_class_value =
                "org.fogbowcloud.ras.core.stubs.StubImagePlugin";

        // exercise
        ImagePlugin plugin = this.pluginInstantiator.getImagePlugin();

        // verify
        Assert.assertEquals(expected_image_plugin_class_value, plugin.getClass().getName());
    }
}
