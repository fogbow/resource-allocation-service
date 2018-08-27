package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGenerator;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.CloudStackComputePlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.util.Properties;

public class CloudStackComputePluginTest {

    private String fakeZoneId;

    private CloudStackComputePlugin plugin;
    private HttpRequestClientUtil client;

    private void initializeProperties() {
        String cloudStackConfFilePath = HomeDir.getPath() + File.separator
               + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.fakeZoneId = properties.getProperty(CloudStackComputePlugin.ZONE_ID_KEY);
    }

    @Before
    public void setUp() {
        initializeProperties();
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.plugin = new CloudStackComputePlugin();

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin.setClient(this.client);
    }

    @Test
    public void getTestTemp() throws UnexpectedException, FogbowRasException {
    }

    @Test
    public void createTestTemp() throws UnexpectedException, FogbowRasException {
    }
}
