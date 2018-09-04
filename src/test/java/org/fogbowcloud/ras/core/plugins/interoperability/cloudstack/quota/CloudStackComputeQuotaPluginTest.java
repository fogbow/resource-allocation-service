package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import java.io.File;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
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

	private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
	private static final String FAKE_USER_ATTRIBUTES = "fake-apikey:fake-secretKey";
	private static final String REQUEST_FORMAT = "%s?command=%s";
	private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";
	private static final String FAKE_VIRTUAL_MACHINE_NAME = "fake-virtual-machine-name";
	private static final String FAKE_IP_ADDRESS = "fake-ip-address";
	private static final String DEFAULT_STATE = "Ready";
	private static final String INSTANCE_RESOURCE_TYPE = "0";
	private static final String CPU_RESOURCE_TYPE = "8";
	private static final String RAM_RESOURCE_TYPE = "9";
    private static final int INSTANCES_MAX_NUMBER = 100;
    private static final int CPUS_MAX_NUMBER = 8;
	private static final int RAMS_MAX_NUMBER = 32768;
	private static final int CPU_RESOURCE_USED = 2;
	private static final int RAM_RESOURCE_USED = 6144;
    
    private CloudStackComputeQuotaPlugin plugin;
    private HttpRequestClientUtil client;
    private CloudStackToken token;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = new CloudStackComputeQuotaPlugin();
        this.plugin.setClient(this.client);
        this.token = new CloudStackToken(FAKE_USER_ATTRIBUTES);
    }
    
    // test case: ...
    @Test
    public void test() throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        
        String urlFormat = REQUEST_FORMAT;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = ListResourceLimitsRequest.LIST_RESOURCE_LIMITS_COMMAND;
        String limitRequest = String.format(urlFormat, baseEndpoint, command);
        
        int max = INSTANCES_MAX_NUMBER;
        String resourceType = INSTANCE_RESOURCE_TYPE;
        String instanceResourceLimit = getResourceLimitResponse(resourceType, max);
        
        max = CPUS_MAX_NUMBER;
        resourceType = CPU_RESOURCE_TYPE;
        String cpuResourceLimit = getResourceLimitResponse(resourceType, max);
        
        max = RAMS_MAX_NUMBER;
        resourceType = RAM_RESOURCE_TYPE;
        String ramResourceLimit = getResourceLimitResponse(resourceType, max);
        
        String limitResponse = getListResourceLimitsResponse(instanceResourceLimit, cpuResourceLimit, ramResourceLimit);
        
        Mockito.when(this.client.doGetRequest(limitRequest, this.token)).thenReturn(limitResponse);
        
        urlFormat = REQUEST_FORMAT;
        command = GetVirtualMachineRequest.LIST_VMS_COMMAND;
        String vmRequest = String.format(urlFormat, baseEndpoint, command);
        
        String id = FAKE_VIRTUAL_MACHINE_ID;
        String name = FAKE_VIRTUAL_MACHINE_NAME;
        String state = DEFAULT_STATE;
        int cpu = CPU_RESOURCE_USED;
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

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(limitRequest),
                Mockito.eq(this.token));
        
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(vmRequest),
                Mockito.eq(this.token));
        
        Assert.assertNotNull(quota);
        System.out.println(quota.getTotalQuota().getInstances());
        System.out.println(quota.getTotalQuota().getvCPU());
        System.out.println(quota.getTotalQuota().getRam());
        System.out.println(quota.getUsedQuota().getInstances());
        System.out.println(quota.getUsedQuota().getvCPU());
        System.out.println(quota.getUsedQuota().getRam());
        System.out.println(quota.getAvailableQuota().getInstances());
        System.out.println(quota.getAvailableQuota().getvCPU());
        System.out.println(quota.getAvailableQuota().getRam());
    }
    
    private String getNicResponse(String ipAddress) {
    	String responseFormat = "{\"ipaddress\": \"%s\"}";
    	return String.format(responseFormat, ipAddress);
    }
    
    private String getVirtualMachineResponse(String id,  String name, String state, int cpu, int ram,  String nic) {
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
    	String responseFormat = "{\"listresourcelimitsresponse\": {" 
    			+ "\"resourcelimit\": [%s, %s, %s]}}";
		return String.format(responseFormat, instanceLimit, cpuLimit, ramLimit);
	}

	private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(BASE_ENDPOINT_KEY);
    }
	
}