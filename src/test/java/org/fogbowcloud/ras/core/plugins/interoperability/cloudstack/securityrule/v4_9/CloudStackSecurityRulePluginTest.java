package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.CloudStackComputePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.DestroyVirtualMachineRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, DefaultLaunchCommandGenerator.class, HttpRequestUtil.class})
public class CloudStackSecurityRulePluginTest {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_INSTANCE_NAME = "fake-name";
    public static final String FAKE_STATE = "Running";
    public static final String FAKE_CPU_NUMBER = "4";
    public static final String FAKE_MEMORY = "2024";
    public static final String FAKE_DISK = "25";
    public static final String FAKE_ADDRESS = "10.0.0.0/24";
    public static final String FAKE_NETWORK_ID = "fake-network-id";
    public static final String FAKE_TYPE = "ROOT";
    public static final String FAKE_EXPUNGE = "true";
    public static final String FAKE_MEMBER = "fake-member";
    public static final String FAKE_PUBLIC_KEY = "fake-public-key";

    private static final int JOB_STATUS_COMPLETE = 1;
    private static final String FAKE_JOB_ID = "fake-job-id";
    private static final String DELETE_FIREWALL_RULE_RESPONSE_KEY = "deletefirewallruleresponse";
    public static final String JSON = "json";
    public static final String RESPONSE_KEY = "response";
    public static final String ID_KEY = "id";
    public static final String VIRTUAL_MACHINE_ID_KEY = "virtualmachineid";
    public static final String TYPE_KEY = "type";
    public static final String EXPUNGE_KEY = "expunge";
    public static final String COMMAND_KEY = "command";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String SERVICE_OFFERING_ID_KEY = "serviceofferingid";
    public static final String TEMPLATE_ID_KEY = "templateid";
    public static final String DISK_OFFERING_ID_KEY = "diskofferingid";
    public static final String NETWORK_IDS_KEY = "networkids";
    public static final String USER_DATA_KEY = "userdata";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_SIGNATURE = "fake-signature";

    public static final CloudStackToken FAKE_TOKEN = new CloudStackToken(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE,
            FAKE_USER_ID, FAKE_USERNAME, FAKE_SIGNATURE);

    private HttpRequestClientUtil client;
    private CloudStackSecurityRulePlugin plugin;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);
        this.client = Mockito.mock(HttpRequestClientUtil.class);

        this.plugin = new CloudStackSecurityRulePlugin();
        this.plugin.setClient(this.client);
    }

    @Test
    public void testDeleteFirewallRule() throws FogbowRasException, HttpResponseException, UnexpectedException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String deleteCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;

        String expectedComputeRequestUrl = generateExpectedUrl(endpoint, deleteCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_ID);

        int status = JOB_STATUS_COMPLETE;
        String jobId = FAKE_JOB_ID;
        String attributeKey = DELETE_FIREWALL_RULE_RESPONSE_KEY;
        String response = getSecurityRuleResponse(status, attributeKey, jobId);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN)).thenReturn(response);

        // exercise
        this.plugin.deleteSecurityRule(FAKE_ID, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedComputeRequestUrl, FAKE_TOKEN);
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + SystemConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(CLOUDSTACK_URL);
    }

    private String generateExpectedUrl(String endpoint, String command, String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            // there should be one value for each key
            return null;
        }

        String url = String.format("%s?command=%s", endpoint, command);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            url += String.format("&%s=%s", key, value);
        }

        return url;
    }

    private String getSecurityRuleResponse(int status, String attributeKey, String jobId) {
        String responseFormat;
        if (status == JOB_STATUS_COMPLETE) {
            responseFormat = "{\"%s\":{"
                    + "\"jobid\": \"%s\""
                    + "}}";

            return String.format(responseFormat, attributeKey, jobId);
        } else {
            responseFormat = "{\"%s\":{}}";
            return String.format(responseFormat, attributeKey);
        }
    }
}
