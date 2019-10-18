package cloud.fogbow.ras.core;

import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
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

    private InteroperabilityPluginInstantiator interoperabilityPluginInstantiator;

    private static final String TEST_CONF_PATH = "src/test/resources/private/";

    // Note: The tests in this class assume that the used cloud is configured at
    // src/test/resources/private/default so, if you intend to change something
    // something in it's files, it should be match in here. For example, if you
    // change volume_plugin_class key at src/test/resources/private/default/plugins.conf ,
    // It will be necessary set the value of expected_volume_plugin_class_value accordingly.
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(HomeDir.class);
        BDDMockito.given(HomeDir.getPath()).willReturn(TEST_CONF_PATH);
        this.interoperabilityPluginInstantiator = new InteroperabilityPluginInstantiator();
    }

    // test case: Tests if the key member_id in ras.conf has its value as fake-localidentity-member.
    @Test
    public void testSetUpProperties() {
        // set up
        String expected_provider_id_value = "fake-localidentity-provider";

        // verify
        Assert.assertEquals(expected_provider_id_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY));
    }

    // test case: Tests if getFederationToLocalMapperPlugin() returns StubSystemToCloudMapperPlugin
    // as the plugin class name.
    @Test
    public void testCreateLocalUserCredentialsMapperPluginInstance() {

        // set up
        String expected_local_user_credentials_mapper_class_value =
                "cloud.fogbow.ras.core.stubs.StubSystemToCloudMapperPlugin";

        // exercise
        SystemToCloudMapperPlugin plugin = this.interoperabilityPluginInstantiator.getSystemToCloudMapperPlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        AttachmentPlugin plugin = this.interoperabilityPluginInstantiator.getAttachmentPlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        ComputePlugin plugin = this.interoperabilityPluginInstantiator.getComputePlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        ComputeQuotaPlugin plugin = this.interoperabilityPluginInstantiator.getComputeQuotaPlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        NetworkPlugin plugin = this.interoperabilityPluginInstantiator.getNetworkPlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        VolumePlugin plugin = this.interoperabilityPluginInstantiator.getVolumePlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        ImagePlugin plugin = this.interoperabilityPluginInstantiator.getImagePlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        PublicIpPlugin plugin = this.interoperabilityPluginInstantiator.getPublicIpPlugin(TestUtils.DEFAULT_CLOUD_NAME);

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

        SecurityRulePlugin plugin = this.interoperabilityPluginInstantiator.getSecurityRulePlugin(TestUtils.DEFAULT_CLOUD_NAME);

        // verify
        Assert.assertEquals(expected_security_rule_plugin_class_value, plugin.getClass().getName());
    }
}
