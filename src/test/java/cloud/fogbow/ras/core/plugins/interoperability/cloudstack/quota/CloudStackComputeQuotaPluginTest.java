package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestUtil;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.quotas.ComputeQuota;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
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
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackComputeQuotaPluginTest {

    private static final String CLOUD_NAME = "cloudstack";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_SIGNATURE = "fake-signature";
    private static final String FAKE_DOMAIN_ID = "fake-domain-id";
    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String RESPONSE_FORMAT = "&response=%s";
    private static final String DOMAIN_ID_PARAM_FORMAT = "&domainid=%s";
    private static final String RESOURCE_TYPE_PARAM_FORMAT = "&resourcetype=%s";
    private static final String JSON_FORMAT = "json";
    private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";
    private static final String FAKE_VIRTUAL_MACHINE_NAME = "fake-virtual-machine-name";
    private static final String FAKE_IP_ADDRESS = "fake-ip-address";
    private static final String DEFAULT_STATE = "Ready";
    private static final String INSTANCE_RESOURCE_TYPE = "0";
    private static final String CPU_RESOURCE_TYPE = "8";
    private static final String RAM_RESOURCE_TYPE = "9";
    private static final int MAX_NUMBER_INSTANCES = 100;
    private static final int UNLIMITED_NUMBER_INSTANCES = -1;
    private static final int MAX_NUMBER_VCPU = 8;
    private static final int MAX_NUMBER_RAM = 32768;
    private static final int INSTANCE_RESOURCE_USED = 1;
    private static final int VCPU_RESOURCE_USED = 2;
    private static final int RAM_RESOURCE_USED = 6144;
    private static final int INSTANCES_AVAILABLE = 99;
    private static final int VCPU_AVAILABLE = 6;
    private static final int RAM_AVAILABLE = 26624;

    private CloudStackComputeQuotaPlugin plugin;
    private CloudStackHttpClient client;
    private CloudToken token;
    private Properties properties;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = new CloudStackComputeQuotaPlugin(cloudStackConfFilePath);
        this.plugin.setClient(this.client);
        this.token = new CloudToken(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_TOKEN_VALUE);
    }

    // test case: When calling the getUserQuota method, HTTP GET requests must be
    // made with a signed token, one to get the total resource limit, and another to
    // list the virtual machines in use, comparing them to calculate the number of
    // available resources, and returning a Quota object .
    @Test
    public void testGetUserQuotaSuccessful() throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String resourceLimitRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);

        String instanceLimitResponse = getResourceLimitResponse(INSTANCE_RESOURCE_TYPE, MAX_NUMBER_INSTANCES);
        String vCpuLimitResponse = getResourceLimitResponse(CPU_RESOURCE_TYPE, MAX_NUMBER_VCPU);
        String ramLimitResponse = getResourceLimitResponse(RAM_RESOURCE_TYPE, MAX_NUMBER_RAM);
        String resourceLimitResponse = getListResourceLimitsResponse(instanceLimitResponse, vCpuLimitResponse,
                ramLimitResponse);

        Mockito.when(this.client.doGetRequest(resourceLimitRequest, this.token)).thenReturn(resourceLimitResponse);

        urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String vmRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);

        String id = FAKE_VIRTUAL_MACHINE_ID;
        String name = FAKE_VIRTUAL_MACHINE_NAME;
        String state = DEFAULT_STATE;
        int cpu = VCPU_RESOURCE_USED;
        int ram = RAM_RESOURCE_USED;
        String ipAddress = FAKE_IP_ADDRESS;
        String nic = getNicResponse(ipAddress);
        String virtualMachine = getVirtualMachineResponse(id, name, state, cpu, ram, nic);
        String vmResponse = getListVirtualMachinesResponse(virtualMachine);

        Mockito.when(this.client.doGetRequest(vmRequest, this.token)).thenReturn(vmResponse);

        // exercise
        ComputeQuota quota = this.plugin.getUserQuota(this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(resourceLimitRequest),
                Mockito.eq(this.token));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(vmRequest), Mockito.eq(this.token));

        Assert.assertEquals(MAX_NUMBER_INSTANCES, quota.getTotalQuota().getInstances());
        Assert.assertEquals(MAX_NUMBER_VCPU, quota.getTotalQuota().getvCPU());
        Assert.assertEquals(MAX_NUMBER_RAM, quota.getTotalQuota().getRam());
        Assert.assertEquals(INSTANCE_RESOURCE_USED, quota.getUsedQuota().getInstances());
        Assert.assertEquals(VCPU_RESOURCE_USED, quota.getUsedQuota().getvCPU());
        Assert.assertEquals(RAM_RESOURCE_USED, quota.getUsedQuota().getRam());
        Assert.assertEquals(INSTANCES_AVAILABLE, quota.getAvailableQuota().getInstances());
        Assert.assertEquals(VCPU_AVAILABLE, quota.getAvailableQuota().getvCPU());
        Assert.assertEquals(RAM_AVAILABLE, quota.getAvailableQuota().getRam());
    }

    // test case: when account has unlimited resource limit (-1 value), then an additional request to gather the domain
    // resource limit should be made
    @Test
    public void testGetDomainQuotaSuccessful() throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String domainUrlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + DOMAIN_ID_PARAM_FORMAT + RESOURCE_TYPE_PARAM_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String domainId = FAKE_DOMAIN_ID;
        String resourceLimitRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);
        String domainResourceLimitRequest = String.format(domainUrlFormat, baseEndpoint, command, jsonFormat, domainId,
                INSTANCE_RESOURCE_TYPE);

        String unlimitedInstanceLimitResponse = getDomainResourceLimitResponse(INSTANCE_RESOURCE_TYPE,
                UNLIMITED_NUMBER_INSTANCES, FAKE_DOMAIN_ID);
        String domainInstanceLimitResponse = getResourceLimitResponse(INSTANCE_RESOURCE_TYPE, MAX_NUMBER_INSTANCES);
        String vCpuLimitResponse = getResourceLimitResponse(CPU_RESOURCE_TYPE, MAX_NUMBER_VCPU);
        String ramLimitResponse = getResourceLimitResponse(RAM_RESOURCE_TYPE, MAX_NUMBER_RAM);
        String resourceLimitResponse = getListResourceLimitsResponse(unlimitedInstanceLimitResponse, vCpuLimitResponse,
                ramLimitResponse);
        String domainResourceLimitResponse = getListResourceLimitsResponse(domainInstanceLimitResponse, vCpuLimitResponse,
                ramLimitResponse);

        Mockito.when(this.client.doGetRequest(resourceLimitRequest, this.token)).thenReturn(resourceLimitResponse);
        Mockito.when(this.client.doGetRequest(domainResourceLimitRequest, this.token)).thenReturn(domainResourceLimitResponse);

        urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String vmRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);

        String id = FAKE_VIRTUAL_MACHINE_ID;
        String name = FAKE_VIRTUAL_MACHINE_NAME;
        String state = DEFAULT_STATE;
        int cpu = VCPU_RESOURCE_USED;
        int ram = RAM_RESOURCE_USED;
        String ipAddress = FAKE_IP_ADDRESS;
        String nic = getNicResponse(ipAddress);
        String virtualMachine = getVirtualMachineResponse(id, name, state, cpu, ram, nic);
        String vmResponse = getListVirtualMachinesResponse(virtualMachine);

        Mockito.when(this.client.doGetRequest(vmRequest, this.token)).thenReturn(vmResponse);

        // exercise
        ComputeQuota quota = this.plugin.getUserQuota(this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(3));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(resourceLimitRequest),
                Mockito.eq(this.token));
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(domainResourceLimitRequest),
                Mockito.eq(this.token));
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(vmRequest), Mockito.eq(this.token));

        Assert.assertEquals(MAX_NUMBER_INSTANCES, quota.getTotalQuota().getInstances());
        Assert.assertEquals(MAX_NUMBER_VCPU, quota.getTotalQuota().getvCPU());
        Assert.assertEquals(MAX_NUMBER_RAM, quota.getTotalQuota().getRam());
        Assert.assertEquals(INSTANCE_RESOURCE_USED, quota.getUsedQuota().getInstances());
        Assert.assertEquals(VCPU_RESOURCE_USED, quota.getUsedQuota().getvCPU());
        Assert.assertEquals(RAM_RESOURCE_USED, quota.getUsedQuota().getRam());
        Assert.assertEquals(INSTANCES_AVAILABLE, quota.getAvailableQuota().getInstances());
        Assert.assertEquals(VCPU_AVAILABLE, quota.getAvailableQuota().getvCPU());
        Assert.assertEquals(RAM_AVAILABLE, quota.getAvailableQuota().getRam());
    }

    // test case: When calling the getUserQuota method with a user without
    // permission, an UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetUserQuotaThrowUnauthorizedRequestException()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            this.plugin.getUserQuota(this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    // test case: When calling the getUserQuota method and not found any resource
    // limits, an InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetUserQuotaWithoutResourceLimitsThrowInstanceNotFoundException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.getUserQuota(this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    // test case: When calling the getUserQuota method and not found any virtual
    // machines, an InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetUserQuotaWithoutVirtualMachinesThrowInstanceNotFoundException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String resourceLimitRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);

        String instanceLimitResponse = getResourceLimitResponse(INSTANCE_RESOURCE_TYPE, MAX_NUMBER_INSTANCES);
        String vCpuLimitResponse = getResourceLimitResponse(CPU_RESOURCE_TYPE, MAX_NUMBER_VCPU);
        String ramLimitResponse = getResourceLimitResponse(RAM_RESOURCE_TYPE, MAX_NUMBER_RAM);
        String resourceLimitResponse = getListResourceLimitsResponse(instanceLimitResponse, vCpuLimitResponse,
                ramLimitResponse);

        Mockito.when(this.client.doGetRequest(resourceLimitRequest, this.token)).thenReturn(resourceLimitResponse);

        urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String vmRequest = String.format(urlFormat, baseEndpoint, command, jsonFormat);

        Mockito.when(this.client.doGetRequest(vmRequest, this.token))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            this.plugin.getUserQuota(this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(resourceLimitRequest),
                    Mockito.eq(this.token));

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(vmRequest), Mockito.eq(this.token));
        }
    }

    // test case: When calling the getUserQuota method with a unauthenticated user,
    // an UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetUserQuotaThrowUnauthenticatedUserException()
            throws FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            this.plugin.getUserQuota(this.token);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    private String getNicResponse(String ipAddress) {
        String responseFormat = "{\"ipaddress\": \"%s\"}";
        return String.format(responseFormat, ipAddress);
    }

    private String getVirtualMachineResponse(String id, String name, String state, int cpu, int ram, String nic) {
        String responseFormat = "{\"id\": \"%s\","
                + " \"name\": \"%s\","
                + " \"state\": \"%s\","
                + " \"cpunumber\": %s,"
                + " \"memory\": %s,"
                + " \"nic\": [%s]}";
        return String.format(responseFormat, id, name, state, cpu, ram, nic);
    }

    private String getListVirtualMachinesResponse(String arg) {
        String responseFormat = "{\"listvirtualmachinesresponse\": {"
                + "\"virtualmachine\": [%s]}}";
        return String.format(responseFormat, arg);
    }

    private String getResourceLimitResponse(String arg, int value) {
        String responseFormat = "{\"resourcetype\": \"%s\", "
                + "\"max\": %s}";
        return String.format(responseFormat, arg, value);
    }

    private String getDomainResourceLimitResponse(String arg, int value, String domainId) {
        String responseFormat = "{\"resourcetype\": \"%s\", "
                + "\"max\": %s,"
                + "\"domainid\": \"%s\"}";

        return String.format(responseFormat, arg, value, domainId);
    }

    private String getListResourceLimitsResponse(String instanceLimit, String cpuLimit, String ramLimit) {
        String responseFormat = "{\"listresourcelimitsresponse\": {" + "\"resourcelimit\": [%s, %s, %s]}}";
        return String.format(responseFormat, instanceLimit, cpuLimit, ramLimit);
    }

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CLOUDSTACK_URL);
    }

}
