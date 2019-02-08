package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;


import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestUtil;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackNetworkPluginTest {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_NAME = "fake-name";
    public static final String FAKE_GATEWAY = "10.0.0.1";
    public static final String FAKE_ADDRESS = "10.0.0.0/24";
    public static final String FAKE_STATE = "Allocated";
    public static final String FAKE_MEMBER = "fake-member";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_SIGNATURE = "fake-signature";

    public static final CloudToken FAKE_TOKEN = new CloudToken(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_TOKEN_VALUE);
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUD_NAME = "cloudstack";

    public static final String JSON = "json";
    public static final String RESPONSE_KEY = "response";
    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String DISPLAY_TEXT_KEY = "displaytext";
    public static final String NETWORK_OFFERING_ID_KEY = "networkofferingid";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String START_IP_KEY = "startip";
    public static final String END_IP_KEY = "endip";
    public static final String GATEWAY_KEY = "gateway";
    public static final String NETMASK_KEY = "netmask";
    public static final String COMMAND_KEY = "command";

    private String fakeOfferingId;
    private String fakeZoneId;

    private CloudStackNetworkPlugin plugin;
    private AuditableHttpRequestClient client;
    private Properties properties;

    @Before
    public void setUp() {
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.fakeOfferingId = this.properties.getProperty(CloudStackNetworkPlugin.NETWORK_OFFERING_ID);
        this.fakeZoneId = this.properties.getProperty(CloudStackNetworkPlugin.ZONE_ID);

        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.plugin = new CloudStackNetworkPlugin(cloudStackConfFilePath);

        this.client = Mockito.mock(AuditableHttpRequestClient.class);
        this.plugin.setClient(this.client);
    }

    // test case: when creating a network with the required fields must return the network resource id
    // that was created by the cloudstack cloud orchestrator
    @Test
    public void testSuccessfulNetworkCreation() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(FAKE_ADDRESS).getInfo();
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CreateNetworkRequest.CREATE_NETWORK_COMMAND);
        expectedParams.put(RESPONSE_KEY, JSON);
        expectedParams.put(ZONE_ID_KEY, fakeZoneId);
        expectedParams.put(NETWORK_OFFERING_ID_KEY, fakeOfferingId);
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(START_IP_KEY, subnetInfo.getLowAddress());
        expectedParams.put(END_IP_KEY, subnetInfo.getHighAddress());
        expectedParams.put(NETMASK_KEY, subnetInfo.getNetmask());
        expectedParams.put(GATEWAY_KEY, FAKE_GATEWAY);
        expectedParams.put(DISPLAY_TEXT_KEY, FAKE_NAME);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, NAME_KEY, DISPLAY_TEXT_KEY);

        String successfulResponse = generateSuccessfulCreateNetworkResponse(FAKE_ID);
        Mockito.when(this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN))).thenReturn(successfulResponse);

        // exercise
        NetworkOrder order = new NetworkOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                "default", FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.DYNAMIC);
        String createdNetworkId = this.plugin.requestInstance(order, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, createdNetworkId);

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(FAKE_TOKEN));
    }

    @Test
    // test case: when getting a network, the token should be signed and an HTTP GET request should be made
    public void testGettingAValidNetwork() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = GetNetworkRequest.LIST_NETWORKS_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command, RESPONSE_KEY, JSON, ID_KEY, FAKE_ID);

        String successfulResponse = generateSuccessfulGetNetworkResponse(FAKE_ID, FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, FAKE_STATE);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, FAKE_TOKEN)).thenReturn(successfulResponse);

        // exercise
        NetworkInstance retrievedInstance = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        // verify
        Assert.assertEquals(FAKE_ID, retrievedInstance.getId());
        Assert.assertEquals(FAKE_ADDRESS, retrievedInstance.getCidr());
        Assert.assertEquals(FAKE_GATEWAY, retrievedInstance.getGateway());
        Assert.assertEquals(FAKE_NAME, retrievedInstance.getName());
        Assert.assertEquals(InstanceState.CREATING, retrievedInstance.getState());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    // test case: getting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNonExistingNetwork() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
        }
    }

    // test case: when deleting a network, the token should be signed and an HTTP GET request should be made
    @Test
    public void testDeleteNetworkSignsTokenBeforeMakingRequest() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String command = DeleteNetworkRequest.DELETE_NETWORK_COMMAND;
        String expectedRequestUrl = generateExpectedUrl(endpoint, command, RESPONSE_KEY, JSON, ID_KEY, FAKE_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // exercise
        this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    // test case: an UnauthorizedRequestException should be thrown when the user tries to delete
    // a network and was not allowed to do that
    @Test(expected = UnauthorizedRequestException.class)
    public void testUnauthorizedExceptionIsThrownWhenOrchestratorForbids() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
        }
    }

    // test case: deleting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteNonExistingNetwork() throws FogbowException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
        }
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

    private String generateSuccessfulCreateNetworkResponse(String id) {
        String format = "{\"createnetworkresponse\":{\"network\":{\"id\":\"%s\"}}}";
        return String.format(format, id);
    }

    private String generateSuccessfulGetNetworkResponse(String id, String name, String gateway, String cidr, String state) {
        String format = "{\"listnetworksresponse\":{\"count\":1" +
                ",\"network\":[" +
                "{\"id\":\"%s\"" +
                ",\"name\":\"%s\"" +
                ",\"gateway\":\"%s\"" +
                ",\"cidr\":\"%s\"" +
                ",\"state\":\"%s\"" +
                "}]}}";

        return String.format(format, id, name, gateway, cidr, state);
    }

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CLOUDSTACK_URL);
    }

}
