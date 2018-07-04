package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PluginInstantiator;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PluginInstantiatorTest {

    private PluginInstantiator service;

    private static final String TEST_CONF_PATH = "src/test/resources/private";

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath(TEST_CONF_PATH);
        this.service = PluginInstantiator.getInstance();
    }

    // FIXME: This test works if none class use getInstance from PluginInstantiator before (if you run only this class, works fine).
    // Note that DefaultLaunchCommandGeneratorTest uses PluginInstantiator, preventing to set XMPP_JID_KEY afterwards.
    @Ignore
    @Test
    public void testSetUpProperties() {
        HomeDir.getInstance().setPath(TEST_CONF_PATH);
        this.service = PluginInstantiator.getInstance();

        String expected_xmpp_jid_value = "fake-localidentity-member";

        Assert.assertEquals(
                expected_xmpp_jid_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationConstants.XMPP_JID_KEY)
                );
    }

    @Test
    public void testCreateFederationIdentityPluginInstance() {
        String expected_federation_identity_class_value =
                "org.fogbowcloud.manager.core.plugins.behavior.federationidentity.DefaultFederationIdentityPlugin";

        FederationIdentityPlugin plugin = this.service.getFederationIdentityPlugin();
        Assert.assertEquals(expected_federation_identity_class_value, plugin.getClass().getName());
    }

    @Test
    public void testCreateLocalIdentityPluginInstance() {
        String expected_local_identity_class_value =
                "org.fogbowcloud.manager.core.plugins.cloud.openstack.KeystoneV3IdentityPlugin";

        LocalIdentityPlugin plugin = this.service.getLocalIdentityPlugin();
        Assert.assertEquals(expected_local_identity_class_value, plugin.getClass().getName());
    }

    @Test
    public void testGetComputePlugin() {
        String expected_compute_plugin_class_value =
                "org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackNovaV2ComputePlugin";

        ComputePlugin plugin = this.service.getComputePlugin();
        Assert.assertEquals(expected_compute_plugin_class_value, plugin.getClass().getName());
    }
}
