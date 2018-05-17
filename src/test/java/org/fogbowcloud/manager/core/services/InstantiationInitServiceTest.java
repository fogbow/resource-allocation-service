package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.FogbowManagerApplication;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.util.Properties;

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

    @Test
    public void testSetUpProperties() {
        String expected_xmpp_jid_value = "my-manager.internal.mydomain";

        Assert.assertEquals(
                this.service.getProperties().getProperty(ConfigurationConstants.XMPP_ID_KEY),
                expected_xmpp_jid_value);
    }

    @Test
    public void testCreateFederationIdentityPluginInstance() {
        String expected_federation_identity_class_value = "org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin";

        IdentityPlugin plugin = this.service.getFederationIdentityPlugin();
        Assert.assertEquals(plugin.getClass().getName(), expected_federation_identity_class_value);
    }

    @Test
    public void testCreateLocalIdentityPluginInstance() {
        String expected_local_identity_class_value = "org.fogbowcloud.manager.core.plugins.identity.openstack.KeystoneV3IdentityPlugin";

        IdentityPlugin plugin = this.service.getLocalIdentityPlugin();
        Assert.assertEquals(plugin.getClass().getName(), expected_local_identity_class_value);
    }

    @Test
    public void testGetComputePlugin() {
        String expected_compute_plugin_class_value = "org.fogbowcloud.manager.core.plugins.compute.openstack.OpenStackNovaV2ComputePlugin";

        ComputePlugin plugin = this.service.getComputePlugin();
        Assert.assertEquals(plugin.getClass().getName(), expected_compute_plugin_class_value);
    }

}