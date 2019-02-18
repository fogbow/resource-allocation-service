package cloud.fogbow.ras.core;

import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
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
        this.interoperabilityPluginInstantiator = new InteroperabilityPluginInstantiator("default");
    }

    // test case: Tests if the key xmpp_jid in ras.conf has its value as fake-localidentity-member.
    @Test
    public void testSetUpProperties() {
        // set up
        String expected_xmpp_jid_value = "fake-localidentity-member";

        // verify
        Assert.assertEquals(expected_xmpp_jid_value,
                PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_JID_KEY));
    }

    // test case: Tests if getFederationToLocalMapperPlugin() returns StubFederationToLocalMapperPlugin
    // as the plugin class name.
    @Test
    public void testCreateLocalUserCredentialsMapperPluginInstance() {
        // set up
        String expected_local_user_credentials_mapper_class_value =
                "cloud.fogbow.ras.core.stubs.StubFederationToLocalMapperPlugin";

        // exercise
        FederationToLocalMapperPlugin plugin = this.interoperabilityPluginInstantiator.getLocalUserCredentialsMapperPlugin();

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
        AttachmentPlugin plugin = this.interoperabilityPluginInstantiator.getAttachmentPlugin();

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
        ComputePlugin plugin = this.interoperabilityPluginInstantiator.getComputePlugin();

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
        ComputeQuotaPlugin plugin = this.interoperabilityPluginInstantiator.getComputeQuotaPlugin();

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
        NetworkPlugin plugin = this.interoperabilityPluginInstantiator.getNetworkPlugin();

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
        VolumePlugin plugin = this.interoperabilityPluginInstantiator.getVolumePlugin();

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
        ImagePlugin plugin = this.interoperabilityPluginInstantiator.getImagePlugin();

        // verify
        Assert.assertEquals(expected_image_plugin_class_value, plugin.getClass().getName());
    }
}
