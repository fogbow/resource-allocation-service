package cloud.fogbow.ras.core;

import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HomeDir.class})
public class PluginInstantiatorTest {

    private InteroperabilityPluginInstantiator interoperabilityPluginInstantiator;

    private static final String TEST_CONF_PATH = "src/test/resources/private/";

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(HomeDir.class);
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        BDDMockito.given(HomeDir.getPath()).willReturn(TEST_CONF_PATH);
        this.interoperabilityPluginInstantiator = new InteroperabilityPluginInstantiator();
    }

    // test case: Tests if the key xmpp_jid in ras.conf has its value as fake-localidentity-member.
    @Test
    public void testSetUpProperties() {
        // set up
        String expected_xmpp_jid_value = "fake-localidentity-provider";

        // verify
        Assert.assertEquals(expected_xmpp_jid_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_JID_KEY));
    }

    // test case: Tests if getFederationToLocalMapperPlugin() returns StubSystemToCloudMapperPlugin
    // as the plugin class name.
    @Test
    public void testCreateLocalUserCredentialsMapperPluginInstance() {
        // set up
        String expected_local_user_credentials_mapper_class_value =
                "cloud.fogbow.ras.core.stubs.StubSystemToCloudMapperPlugin";

        // exercise
        String fakeCloudName = "default";
        SystemToCloudMapperPlugin plugin = this.interoperabilityPluginInstantiator.getSystemToCloudMapperPlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_local_user_credentials_mapper_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getAttachmentPlugin() returns StubAttachmentPlugin as the plugin class name.
    @Test
    public void testCreateAttachmentPlugin() {
        // set up
        String expected_attachment_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubAttachmentPlugin";

        // exercise
        String fakeCloudName = "default";
        AttachmentPlugin plugin = this.interoperabilityPluginInstantiator.getAttachmentPlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_attachment_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getComputePlugin() returns StubComputePlugin as the plugin class name.
    @Test
    public void testCreateComputePlugin() {
        // set up
        String expected_compute_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubComputePlugin";

        // exercise
        String fakeCloudName = "default";
        ComputePlugin plugin = this.interoperabilityPluginInstantiator.getComputePlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_compute_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getComputeQuotaPlugin() returns StubComputeQuotaPlugin as the plugin class name.
    @Test
    public void testCreateComputeQuotaPlugin() {
        // set up
        String expected_compute_quota_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubComputeQuotaPlugin";

        // exercise
        String fakeCloudName = "default";
        ComputeQuotaPlugin plugin = this.interoperabilityPluginInstantiator.getComputeQuotaPlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_compute_quota_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getNetworkPlugin() returns StubNetworkPlugin as the plugin class name.
    @Test
    public void testCreateNetworkPlugin() {
        // set up
        String expected_network_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubNetworkPlugin";

        // exercise
        String fakeCloudName = "default";
        NetworkPlugin plugin = this.interoperabilityPluginInstantiator.getNetworkPlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_network_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getVolumePlugin() returns StubVolumePlugin as the plugin class name.
    @Test
    public void testCreateVolumePlugin() {
        // set up
        String expected_volume_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubVolumePlugin";

        // exercise
        String fakeCloudName = "default";
        VolumePlugin plugin = this.interoperabilityPluginInstantiator.getVolumePlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_volume_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getImagePlugin() returns StubImagePlugin as the plugin class name.
    @Test
    public void testCreateImagePlugin() {
        // set up
        String expected_image_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubImagePlugin";

        // exercise
        String fakeCloudName = "default";
        ImagePlugin plugin = this.interoperabilityPluginInstantiator.getImagePlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_image_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getPublicIpPlugin() returns StubPublicIpPlugin as the plugin class name.
    @Test
    public void testCreatePublicIpPlugin() {
        // set up
        String expected_public_ip_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubPublicIpPlugin";

        // exercise
        String fakeCloudName = "default";
        PublicIpPlugin plugin = this.interoperabilityPluginInstantiator.getPublicIpPlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_public_ip_plugin_class_value, plugin.getClass().getName());
    }

    // test case: Tests if getSecurityRulePlugin() returns StubSecurityRulePlugin as the plugin class name.
    @Test
    public void testCreateSecurityRulePlugin() {
        // set up
        String expected_security_rule_plugin_class_value =
                "cloud.fogbow.ras.core.stubs.StubSecurityRulePlugin";

        // exercise
        String fakeCloudName = "default";
        SecurityRulePlugin plugin = this.interoperabilityPluginInstantiator.getSecurityRulePlugin(fakeCloudName);

        // verify
        Assert.assertEquals(expected_security_rule_plugin_class_value, plugin.getClass().getName());
    }
}
