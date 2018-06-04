package org.fogbowcloud.manager.core.services;

import java.io.FileInputStream;
import java.util.Properties;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
//import org.fogbowcloud.manager.core.manager.plugins.behavior.federationidentity.FederationIdentityPlugin;
//import org.fogbowcloud.manager.core.manager.plugins.cloud.compute.ComputePlugin;
//import org.fogbowcloud.manager.core.manager.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class InstantiationInitServiceTest {

    private InstantiationInitService service;

    private static final String TEST_CONF_FILE_FULL_PATH = "src/main/resources/test.properties";

    @Before
    public void setUp() throws Exception {
        this.service = new InstantiationInitService();
        Properties mProperties = new Properties();
        FileInputStream mInput = new FileInputStream(TEST_CONF_FILE_FULL_PATH);
        mProperties.load(mInput);
        this.service.getProperties().putAll(mProperties);
    }

    // TODO implement
    @Ignore
    @Test
    public void testSetUpProperties() {
        String expected_xmpp_jid_value = "my-manager.internal.mydomain";

        Assert.assertEquals(
                this.service.getProperties().getProperty(ConfigurationConstants.XMPP_JID_KEY),
                expected_xmpp_jid_value);
    }

    // TODO implement
    @Ignore
    @Test
    public void testCreateFederationIdentityPluginInstance() {
        String expected_federation_identity_class_value =
                "org.fogbowcloud.manager.core.manager.plugins.identity.ldap.LdapIdentityPlugin";

        //FederationIdentityPlugin plugin = this.service.getFederationIdentityPlugin();
        //Assert.assertEquals(plugin.getClass().getName(), expected_federation_identity_class_value);
    }

    // TODO implement
    @Ignore
    @Test
    public void testCreateLocalIdentityPluginInstance() {
        String expected_local_identity_class_value =
                "org.fogbowcloud.manager.core.manager.plugins.identity.openstack.KeystoneV3IdentityPlugin";

        //LocalIdentityPlugin plugin = this.service.getLocalIdentityPlugin();
        //Assert.assertEquals(plugin.getClass().getName(), expected_local_identity_class_value);
    }

    // TODO implement
    @Ignore
    @Test
    public void testGetComputePlugin() {
        String expected_compute_plugin_class_value =
                "org.fogbowcloud.manager.core.manager.plugins.compute.openstack.OpenStackNovaV2ComputePlugin";

        //ComputePlugin plugin = this.service.getComputePlugin();
        //Assert.assertEquals(plugin.getClass().getName(), expected_compute_plugin_class_value);
    }
}
