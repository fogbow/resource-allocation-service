package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Properties;


@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackNetworkPluginTest {

    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_TOKEN_VALUE = "fake-token-value";

    public static final CloudStackToken FAKE_TOKEN = new CloudStackToken(FAKE_TOKEN_VALUE);

    private CloudStackNetworkPlugin plugin;
    private HttpRequestClientUtil client;

    @Before
    public void setUp() {
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestUtil.class);

        HomeDir.getInstance().setPath("src/test/resources/private");
        this.plugin = new CloudStackNetworkPlugin();

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin.setClient(this.client);
    }

    @Test
    @Ignore
    // FIXME Remove this
    public void testGetTemp() throws UnexpectedException, FogbowManagerException {
        CloudStackNetworkPlugin p = new CloudStackNetworkPlugin();

        String apiKey = "Xp2TRynMLZpFAchsTpLpcj8zW_omWCWaP6NNxmb2fV9Nv_Ga6J8QcNRbPCcZUSY2NDS83d7svGhdikV7XrVkcQ";
        String secretKey = "pnDkW2amt9w-9pjn5tB4DAIc50bCdK3m6CO99r_r5xoDTTJjpormiTfj_5QEbbkVhdE5mHbTq5t8X-fKhHcJeg";

        String tokenValue = apiKey + CloudStackTokenGenerator.TOKEN_VALUE_SEPARATOR + secretKey;
        CloudStackToken token = new CloudStackToken(tokenValue);
        String networkId = "6fc82e5a-1d96-4d10-b9bf-96693c89dda8";
        NetworkInstance i = p.getInstance(networkId, token);
    }

    @Test
    public void test() {
        // create success
    }

    @Test
    public void test2() {
        // create fail
    }

    @Test
    // test case: when getting a network, the token should be signed and an HTTP GET request should be made
    public void testGettingAValidNetwork() throws FogbowManagerException, UnexpectedException, HttpResponseException {
        // set up
        String expectedRequestUrl = String.format("%s?command=%s&id=%s",
                getBaseEndpointFromCloudStackConf(), GetNetworkRequest.LIST_NETWORKS_COMMAND, FAKE_ID);

        String id = FAKE_ID;
        String name = "fake-name";
        String gateway = "10.0.0.1";
        String cidr = "10.0.0.0/24";
        String state = "Allocated";
        String successfulResponse = getSuccessfulGetNetworkResponse(id, name, gateway, cidr, state);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(expectedRequestUrl, FAKE_TOKEN)).thenReturn(successfulResponse);

        // exercise
        NetworkInstance retrievedInstance = this.plugin.getInstance(FAKE_ID, FAKE_TOKEN);

        // verify
        Assert.assertEquals(id, retrievedInstance.getId());
        Assert.assertEquals(id, retrievedInstance.getId());
        Assert.assertEquals(cidr, retrievedInstance.getAddress());
        Assert.assertEquals(gateway, retrievedInstance.getGateway());
        Assert.assertEquals(name, retrievedInstance.getLabel());

        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(expectedRequestUrl, FAKE_TOKEN);
    }

    // test case: getting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetNonExistingNetwork() throws FogbowManagerException, HttpResponseException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class));
        }
    }

    // test case: when deleting a network, the token should be signed and an HTTP GET request should be made
    @Test
    public void testDeleteNetworkSignsTokenBeforeMakingRequest() throws FogbowManagerException, UnexpectedException, HttpResponseException {
        // set up
        String expectedRequestUrl = String.format("%s?command=%s&id=%s",
                getBaseEndpointFromCloudStackConf(), DeleteNetworkRequest.DELETE_NETWORK_COMMAND, FAKE_ID);

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
    public void testUnauthorizedExceptionIsThrownWhenOrchestratorForbids() throws UnexpectedException, FogbowManagerException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
               .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class));
        }
    }

    // test case: deleting a non-existing network should throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteNonExistingNetwork() throws UnexpectedException, FogbowManagerException, HttpResponseException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.deleteInstance(FAKE_ID, FAKE_TOKEN);
        } finally {
            // verify
            Mockito.verify(this.client, Mockito.times(1))
                    .doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class));
        }
    }

    private String getSuccessfulGetNetworkResponse(String id, String name, String gateway, String cidr, String state) {
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
        String filePath = HomeDir.getInstance().getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(CloudStackTokenGenerator.CLOUDSTACK_URL);
    }

}
