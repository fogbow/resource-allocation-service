package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import java.io.File;
import java.util.Properties;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackComputeQuotaPluginTest {

	private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String FAKE_USERNAME = "fake-username";
	private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
	private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
	private static final String REQUEST_FORMAT = "%s?command=%s";
	private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";
	private static final String FAKE_VIRTUAL_MACHINE_NAME = "fake-virtual-machine-name";
	private static final String FAKE_IP_ADDRESS = "fake-ip-address";
	private static final String DEFAULT_STATE = "Ready";
	private static final String INSTANCE_RESOURCE_TYPE = "0";
	private static final String CPU_RESOURCE_TYPE = "8";
	private static final String RAM_RESOURCE_TYPE = "9";
	private static final int MAX_NUMBER_INSTANCES = 100;
	private static final int MAX_NUMBER_VCPU = 8;
	private static final int MAX_NUMBER_RAM = 32768;
	private static final int INSTANCE_RESOURCE_USED = 1;
	private static final int VCPU_RESOURCE_USED = 2;
	private static final int RAM_RESOURCE_USED = 6144;
	private static final int INSTANCES_AVAILABLE = 99;
	private static final int VCPU_AVAILABLE = 6;
	private static final int RAM_AVAILABLE = 26624;

	private CloudStackComputeQuotaPlugin plugin;
	private HttpRequestClientUtil client;
	private CloudStackToken token;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(HttpRequestUtil.class);

		this.client = Mockito.mock(HttpRequestClientUtil.class);
		this.plugin = new CloudStackComputeQuotaPlugin();
		this.plugin.setClient(this.client);
		this.token = new CloudStackToken(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_USERNAME);
	}

	// test case: When calling the getUserQuota method, HTTP GET requests must be
	// made with a signed token, one to get the total resource limit, and another to
	// list the virtual machines in use, comparing them to calculate the number of
	// available resources, and returning a Quota object .
	@Test
	public void testGetUserQuotaSuccessful() throws HttpResponseException, FogbowRasException, UnexpectedException {
		// set up
		PowerMockito.mockStatic(CloudStackUrlUtil.class);
		PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
				.thenCallRealMethod();

		String urlFormat = REQUEST_FORMAT;
		String baseEndpoint = getBaseEndpointFromCloudStackConf();
		String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
		String resourceLimitRequest = String.format(urlFormat, baseEndpoint, command);

		String instanceLimitResponse = getResourceLimitResponse(INSTANCE_RESOURCE_TYPE, MAX_NUMBER_INSTANCES);
		String vCpuLimitResponse = getResourceLimitResponse(CPU_RESOURCE_TYPE, MAX_NUMBER_VCPU);
		String ramLimitResponse = getResourceLimitResponse(RAM_RESOURCE_TYPE, MAX_NUMBER_RAM);
		String resourceLimitResponse = getListResourceLimitsResponse(instanceLimitResponse, vCpuLimitResponse,
				ramLimitResponse);

		Mockito.when(this.client.doGetRequest(resourceLimitRequest, this.token)).thenReturn(resourceLimitResponse);

		urlFormat = REQUEST_FORMAT;
		command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
		String vmRequest = String.format(urlFormat, baseEndpoint, command);

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

	// test case: When calling the getUserQuota method with a user without
	// permission, an UnauthorizedRequestException must be thrown.
	@Test(expected = UnauthorizedRequestException.class)
	public void testGetUserQuotaThrowUnauthorizedRequestException()
			throws HttpResponseException, FogbowRasException, UnexpectedException {

		// set up
		PowerMockito.mockStatic(CloudStackUrlUtil.class);
		PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
				.thenCallRealMethod();

		Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
				.thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

		try {
			// exercise
			this.plugin.getUserQuota(this.token);
		} finally {
			// verify
			PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
			CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

			Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
					Mockito.any(CloudStackToken.class));
		}
	}

	// test case: When calling the getUserQuota method and not found any resource
	// limits, an InstanceNotFoundException must be thrown.
	@Test(expected = InstanceNotFoundException.class)
	public void testGetUserQuotaWithoutResourceLimitsThrowInstanceNotFoundException()
			throws HttpResponseException, FogbowRasException, UnexpectedException {
		// set up
		PowerMockito.mockStatic(CloudStackUrlUtil.class);
		PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
				.thenCallRealMethod();

		Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
				.thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

		try {
			// exercise
			this.plugin.getUserQuota(this.token);
		} finally {
			// verify
			PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
			CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

			Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
					Mockito.any(CloudStackToken.class));
		}
	}

	// test case: When calling the getUserQuota method and not found any virtual
	// machines, an InstanceNotFoundException must be thrown.
	@Test(expected = InstanceNotFoundException.class)
	public void testGetUserQuotaWithoutVirtualMachinesThrowInstanceNotFoundException()
			throws HttpResponseException, FogbowRasException, UnexpectedException {
		// set up
		PowerMockito.mockStatic(CloudStackUrlUtil.class);
		PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
				.thenCallRealMethod();

		String urlFormat = REQUEST_FORMAT;
		String baseEndpoint = getBaseEndpointFromCloudStackConf();
		String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
		String resourceLimitRequest = String.format(urlFormat, baseEndpoint, command);

		String instanceLimitResponse = getResourceLimitResponse(INSTANCE_RESOURCE_TYPE, MAX_NUMBER_INSTANCES);
		String vCpuLimitResponse = getResourceLimitResponse(CPU_RESOURCE_TYPE, MAX_NUMBER_VCPU);
		String ramLimitResponse = getResourceLimitResponse(RAM_RESOURCE_TYPE, MAX_NUMBER_RAM);
		String resourceLimitResponse = getListResourceLimitsResponse(instanceLimitResponse, vCpuLimitResponse,
				ramLimitResponse);

		Mockito.when(this.client.doGetRequest(resourceLimitRequest, this.token)).thenReturn(resourceLimitResponse);

		urlFormat = REQUEST_FORMAT;
		command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
		String vmRequest = String.format(urlFormat, baseEndpoint, command);

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
			throws UnexpectedException, FogbowRasException, HttpResponseException {

		// set up
		PowerMockito.mockStatic(CloudStackUrlUtil.class);
		PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
				.thenCallRealMethod();

		Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackToken.class)))
				.thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

		try {
			// exercise
			this.plugin.getUserQuota(this.token);
		} finally {
			// verify
			PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
			CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

			Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
					Mockito.any(CloudStackToken.class));
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

	private String getListResourceLimitsResponse(String instanceLimit, String cpuLimit, String ramLimit) {
		String responseFormat = "{\"listresourcelimitsresponse\": {" + "\"resourcelimit\": [%s, %s, %s]}}";
		return String.format(responseFormat, instanceLimit, cpuLimit, ramLimit);
	}

	private String getBaseEndpointFromCloudStackConf() {
		String filePath = HomeDir.getPath() + File.separator + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

		Properties properties = PropertiesUtil.readProperties(filePath);
		return properties.getProperty(BASE_ENDPOINT_KEY);
	}

}