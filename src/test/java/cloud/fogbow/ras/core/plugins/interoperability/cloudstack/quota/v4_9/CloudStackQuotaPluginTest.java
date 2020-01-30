package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse.VirtualMachine;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9.GetNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9.GetNetworkResponse.Network;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse.Volume;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9.ListResourceLimitsResponse.ResourceLimit;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9.ListPublicIpAddressResponse.PublicIpAddress;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CloudStackHttpToFogbowExceptionMapper.class,
        CloudStackUrlUtil.class,
        GetNetworkResponse.class,
        GetVirtualMachineResponse.class,
        GetVolumeResponse.class,
        ListResourceLimitsResponse.class,
        ListPublicIpAddressResponse.class})
public class CloudStackQuotaPluginTest {
    private static final String CLOUD_NAME = "cloudstack";

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();
    private static final String FAKE_DOMAIN_ID = "fake-domain-id";

    private static final String INSTANCE_RESOURCE_TYPE = "0";
    private static final String PUBLIC_IP_RESOURCE_TYPE = "1";
    private static final String NETWORK_RESOURCE_TYPE = "6";
    private static final String CPU_RESOURCE_TYPE = "8";
    private static final String RAM_RESOURCE_TYPE = "9";
    private static final String STORAGE_RESOURCE_TYPE = "10";

    private static final int UNLIMITED_RESOURCE = -1;

    private static final int MAX_INSTANCES = 30;
    private static final int MAX_CORES = 100;
    private static final int MAX_RAM = 16384;
    private static final int MAX_DISK = 30000;
    private static final int MAX_NETWORKS = 40;
    private static final int MAX_PUBLIC_IP_ADDRESSES = 20;

    private static final int USED_INSTANCES = 6;
    private static final int USED_CORES = 10;
    private static final int USED_RAM = 8192;
    private static final int USED_DISK = 672;
    private static final int USED_NETWORKS = 8;
    private static final int USED_PUBLIC_IP_ADDRESSES = 9;

    private static final String ANY_URL = "http://localhost:5056";
    private static final String ANY_JSON = "{\"foo\": \"bar\"}";
    private static final int ANY_STATUS_CODE = 400;
    private static final String FAKE_RESOURCE_TYPE = "0";
    private static final int DOMAIN_LIMIT_NOT_FOUND_VALUE = 0;
    private static final long ONE_GIGABYTE_IN_BYTES = (long) Math.pow(1024, 3);

    private CloudStackQuotaPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudUser;
    private List<ResourceLimit> resourcesAccountLimited;

    @Before
    public void setUp() throws InvalidParameterException {
        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackQuotaPlugin(cloudStackConfFilePath));
        this.plugin.setClient(client);
        this.cloudUser = new CloudStackUser(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);

        this.resourcesAccountLimited = this.buildAccountLimitedResources(MAX_DISK);

        this.ignoringCloudStackUrl();
    }

    // test case: when given a cloud user, it should return the user resource quota (total, available and used quota)
    @Test
    public void testGetUserQuotaSuccessful() throws FogbowException {
        // set up
        ResourceAllocation totalQuota = Mockito.mock(ResourceAllocation.class);
        Mockito.doReturn(totalQuota).when(this.plugin).getTotalQuota(Mockito.eq(this.cloudUser));

        ResourceAllocation usedQuota = Mockito.mock(ResourceAllocation.class);
        Mockito.doReturn(usedQuota).when(this.plugin).getUsedQuota(Mockito.eq(this.cloudUser));

        ResourceQuota expectedUserQuota = new ResourceQuota(totalQuota, usedQuota);

        // exercise
        ResourceQuota userQuota = this.plugin.getUserQuota(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalQuota(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalQuota(Mockito.eq(this.cloudUser));

        Assert.assertEquals(expectedUserQuota, userQuota);
    }

    // test case: when given a cloud user, it should return the user total resource quota (limit)
    @Test
    public void testGetUserTotalQuotaSuccessful() throws FogbowException {
        // set up
        List<ResourceLimit> resourceLimits = new ArrayList<>();
        ResourceLimit resourceLimit = Mockito.mock(ResourceLimit.class);
        resourceLimits.add(resourceLimit);

        Mockito.doReturn(resourceLimits).when(this.plugin).getResourceLimits(Mockito.eq(this.cloudUser));

        ResourceAllocation totalQuota = Mockito.mock(ResourceAllocation.class);
        Mockito.doReturn(totalQuota).when(this.plugin)
                .getTotalAllocation(Mockito.eq(resourceLimits), Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.getTotalQuota(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getResourceLimits(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getTotalAllocation(Mockito.eq(resourceLimits), Mockito.eq(this.cloudUser));
    }

    // test case: when a cloud user is given, return a list of resource limits
    @Test
    public void testGetResourceLimitsSuccessful() throws FogbowException {
        // set up
        ListResourceLimitsResponse response = Mockito.mock(ListResourceLimitsResponse.class);
        Mockito.doReturn(this.resourcesAccountLimited).when(response).getResourceLimits();

        PowerMockito.mockStatic(ListResourceLimitsResponse.class);
        BDDMockito.given(ListResourceLimitsResponse.fromJson(Mockito.anyString()))
                .willReturn(response);

        Mockito.doReturn(TestUtils.ANY_VALUE).when(this.plugin).doGetRequest(Mockito.eq(cloudUser), Mockito.anyString());

        // exercise
        List<ResourceLimit> actualResourceLimits = this.plugin.getResourceLimits(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());
        Assert.assertEquals(this.resourcesAccountLimited, actualResourceLimits);
    }

    // test case: When calling getTotalAllocation method and none of the resource limits is unlimited for the given
    // account, a ResourceAllocation instance must be built and returned
    @Test
    public void testGetTotalAllocationSuccessful() {
        // set up
        ResourceAllocation expectedAllocation = new ResourceAllocation.Builder()
                .instances(MAX_INSTANCES)
                .ram(MAX_RAM)
                .vCPU(MAX_CORES)
                .disk(MAX_DISK)
                .networks(MAX_NETWORKS)
                .publicIps(MAX_PUBLIC_IP_ADDRESSES)
                .build();

        // exercise
        ResourceAllocation totalAllocation = this.plugin.getTotalAllocation(this.resourcesAccountLimited, this.cloudUser);

        // verify
        Assert.assertEquals(totalAllocation, expectedAllocation);
    }

    // test case: When calling getTotalAllocation method and one of the resource limits (in this case, disk resource)
    // is unlimited for that account, a extra request should be made to get the domain resource limits and
    // build a ResourceAllocation object with the new resource limit replaced
    @Test
    public void testGetTotalAllocationSuccessfulLimitedResources() {
        // set up
        List<ResourceLimit> resourceLimits = this.buildAccountLimitedResources(UNLIMITED_RESOURCE);
        ResourceLimit resourceLimit = new ResourceLimit(STORAGE_RESOURCE_TYPE, FAKE_DOMAIN_ID, UNLIMITED_RESOURCE);
        ResourceLimit mockedResponse = new ResourceLimit(STORAGE_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_DISK);

        Mockito.doReturn(mockedResponse).when(this.plugin).getDomainResourceLimit(Mockito.eq(resourceLimit),
                Mockito.eq(this.cloudUser));

        // exercise
        ResourceAllocation totalAllocation = this.plugin.getTotalAllocation(resourceLimits, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDomainResourceLimit(Mockito.eq(resourceLimit),
                Mockito.eq(this.cloudUser));

        Assert.assertEquals(MAX_DISK, totalAllocation.getDisk());
    }

    // test case: When calling getTotalAllocation method and got a unexpected exception when was attempted to get the
    // domain resource limit (in this case, disk resource), the resource limit must be zero
    @Test
    public void testGetTotalAllocationUnsuccessfulLimitedResources() throws FogbowException {
        // set up
        List<ResourceLimit> resourceLimits = this.buildAccountLimitedResources(UNLIMITED_RESOURCE);
        ResourceLimit resourceLimit = new ResourceLimit(STORAGE_RESOURCE_TYPE, FAKE_DOMAIN_ID, UNLIMITED_RESOURCE);

        FogbowException exception = new FogbowException();
        Mockito.doThrow(exception).when(this.plugin).doGetDomainResourceLimit(Mockito.eq(resourceLimit.getResourceType()),
                Mockito.eq(FAKE_DOMAIN_ID), Mockito.eq(this.cloudUser));

        // exercise
        ResourceAllocation totalAllocation = this.plugin.getTotalAllocation(resourceLimits, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetDomainResourceLimit(
                Mockito.eq(resourceLimit.getResourceType()), Mockito.eq(FAKE_DOMAIN_ID), Mockito.eq(this.cloudUser));

        Assert.assertEquals(DOMAIN_LIMIT_NOT_FOUND_VALUE, totalAllocation.getDisk());
    }

    // test case: When calling doGetRequest, it must verify if the call was successful
    @Test
    public void testDoGetRequestSuccessful() throws FogbowException, HttpResponseException {
        // set up
        Mockito.when(this.client.doGetRequest(Mockito.eq(ANY_URL), Mockito.eq(cloudUser)))
                .thenReturn(ANY_JSON);

        // exercise
        String response = this.plugin.doGetRequest(this.cloudUser, ANY_URL);

        // verify
        Assert.assertEquals(ANY_JSON, response);
    }

    // test case: When calling doGetRequest and a unexpected exception occurs, it must verify
    // if the get method from the CloudStackHttpToFogbowExceptionMapper class was called
    @Test
    public void testDoGetRequestUnsuccessful() throws Exception {
        // set up
        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);
        Mockito.when(this.client.doGetRequest(Mockito.eq(ANY_URL), Mockito.eq(this.cloudUser))).thenThrow(exception);

        PowerMockito.mockStatic(CloudStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(CloudStackHttpToFogbowExceptionMapper.class, "get", Mockito.any());

        // exercise
        try {
            this.plugin.doGetRequest(this.cloudUser, ANY_URL);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            PowerMockito.verifyStatic(CloudStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            CloudStackHttpToFogbowExceptionMapper.get(Mockito.eq(exception));
            Assert.assertEquals(exception.getMessage(), e.getMessage());
        }

    }

    // test case: When calling the getDomainResourceLimit method, a request should be made to get the domain resource
    // limit of the given resource domain
    @Test
    public void testGetDomainResourceLimit() throws FogbowException {
        // set up
        List<ResourceLimit> resourceLimits = new ArrayList<>();
        resourceLimits.add(new ResourceLimit(STORAGE_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_DISK));

        ListResourceLimitsResponse response = Mockito.mock(ListResourceLimitsResponse.class);
        Mockito.doReturn(resourceLimits).when(response).getResourceLimits();

        PowerMockito.mockStatic(ListResourceLimitsResponse.class);
        BDDMockito.given(ListResourceLimitsResponse.fromJson(Mockito.anyString()))
                .willReturn(response);

        Mockito.doReturn(TestUtils.ANY_VALUE).when(this.plugin).doGetRequest(Mockito.eq(cloudUser), Mockito.anyString());

        // exercise
        ResourceLimit resourceLimitResponse = this.plugin.doGetDomainResourceLimit(
                FAKE_RESOURCE_TYPE, FAKE_DOMAIN_ID, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());
        Assert.assertEquals(resourceLimits.get(0), resourceLimitResponse);
    }

    // test case: When calling getUsedQuota with secondary methods mocked it must verify
    // if it is returned the ResourceAllocation correct
    @Test
    public void testGetUsedQuota() throws FogbowException {
        // set up
        List<VirtualMachine> virtualMachines = getVirtualMachines();
        List<Volume> volumes = getVolumes();
        List<Network> networks = getNetworks();
        List<PublicIpAddress> publicIpAddresses = getPublicIpAddresses();

        ResourceAllocation resourceAllocation = Mockito.mock(ResourceAllocation.class);
        Mockito.doReturn(resourceAllocation).when(this.plugin).getUsedAllocation(Mockito.eq(virtualMachines), Mockito.eq(volumes),
                Mockito.eq(networks), Mockito.eq(publicIpAddresses));

        // exercise
        ResourceAllocation response = this.plugin.getUsedQuota(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachines(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVolumes(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworks(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPublicIpAddresses(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedAllocation(Mockito.eq(virtualMachines), Mockito.eq(volumes),
                Mockito.eq(networks), Mockito.eq(publicIpAddresses));

        Assert.assertEquals(resourceAllocation, response);
    }

    private List<VirtualMachine> getVirtualMachines() throws FogbowException {
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        VirtualMachine mock = Mockito.mock(VirtualMachine.class);
        virtualMachines.add(mock);
        Mockito.doReturn(virtualMachines).when(this.plugin).getVirtualMachines(Mockito.eq(this.cloudUser));
        return virtualMachines;
    }

    private List<PublicIpAddress> getPublicIpAddresses() throws FogbowException {
        List<PublicIpAddress> publicIpAddresses = new ArrayList<>();
        PublicIpAddress mock = Mockito.mock(PublicIpAddress.class);
        publicIpAddresses.add(mock);
        Mockito.doReturn(publicIpAddresses).when(this.plugin).getPublicIpAddresses(Mockito.eq(this.cloudUser));
        return publicIpAddresses;
    }

    private List<Network> getNetworks() throws FogbowException {
        List<Network> networks = new ArrayList<>();
        Network mock = Mockito.mock(Network.class);
        networks.add(mock);
        Mockito.doReturn(networks).when(this.plugin).getNetworks(Mockito.eq(this.cloudUser));
        return networks;
    }

    private List<Volume> getVolumes() throws FogbowException {
        List<Volume> volumes = new ArrayList<>();
        Volume mock = Mockito.mock(Volume.class);
        volumes.add(mock);
        Mockito.doReturn(volumes).when(this.plugin).getVolumes(Mockito.eq(this.cloudUser));
        return volumes;
    }

    // test case: When calling getVirtualMachines with secondary methods mocked, it must verify if it is returned
    // the list of virtual machines correctly
    @Test
    public void testGetVirtualMachinesSuccessful() throws FogbowException, HttpResponseException {
        // set up
        GetVirtualMachineResponse mockedResponse = Mockito.mock(GetVirtualMachineResponse.class);

        List<VirtualMachine> virtualMachines = new ArrayList<>();
        virtualMachines.add(Mockito.mock(VirtualMachine.class));
        Mockito.doReturn(virtualMachines).when(mockedResponse).getVirtualMachines();

        PowerMockito.mockStatic(GetVirtualMachineResponse.class);
        BDDMockito.given(GetVirtualMachineResponse.fromJson(Mockito.anyString())).willReturn(mockedResponse);

        Mockito.doReturn(TestUtils.ANY_VALUE).when(this.plugin).doGetRequest(Mockito.eq(cloudUser), Mockito.anyString());

        // exercise
        List<VirtualMachine> response = this.plugin.getVirtualMachines(this.cloudUser);

        // verify
        Mockito.verify(mockedResponse, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachines();

        PowerMockito.verifyStatic(GetVirtualMachineResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetVirtualMachineResponse.fromJson(Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                Mockito.anyString());

        Assert.assertEquals(virtualMachines, response);
    }

    // test case: When calling getVirtualMachines with secondary methods mocked and a unexpected exception is threw,
    // it must verify if the threw exception is mapped by CloudStackHttpToFogbowExceptionMapper correctly
    @Test
    public void testGetVirtualMachinesUnsuccessful() throws Exception {
        // set up
        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);

        PowerMockito.mockStatic(GetVirtualMachineResponse.class);
        BDDMockito.when(GetVirtualMachineResponse.fromJson(Mockito.anyString())).thenThrow(exception);

        PowerMockito.mockStatic(CloudStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(CloudStackHttpToFogbowExceptionMapper.class, "get", Mockito.any());

        try {
            // exercise
            this.plugin.getVirtualMachines(this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            PowerMockito.verifyStatic(GetVirtualMachineResponse.class, Mockito.times(TestUtils.RUN_ONCE));
            GetVirtualMachineResponse.fromJson(Mockito.anyString());

            PowerMockito.verifyStatic(CloudStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            CloudStackHttpToFogbowExceptionMapper.get(Mockito.eq(exception));

            Assert.assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    // test case: When calling getVolumes with secondary methods mocked, it must verify if is returned the list of
    // volumes correctly
    @Test
    public void testGetVolumesSuccessful() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        PowerMockito.mockStatic(GetVolumeResponse.class);

        GetVolumeResponse mockedResponse = Mockito.mock(GetVolumeResponse.class);
        List<Volume> mockedVolumes = new ArrayList<>();

        Mockito.doReturn(mockedVolumes).when(mockedResponse).getVolumes();

        BDDMockito.given(GetVolumeResponse.fromJson(Mockito.anyString())).willReturn(mockedResponse);

        // exercise
        List<Volume> volumes = this.plugin.getVolumes(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                Mockito.anyString());

        PowerMockito.verifyStatic(GetVolumeResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetVolumeResponse.fromJson(Mockito.anyString());

        Assert.assertEquals(mockedVolumes, volumes);
    }

    // test case: When calling getVolumes with secondary methods mocked and a exception occurs,
    // it must map with CloudStackHttpToFogbowExceptionMapper
    @Test
    public void testGetVolumesUnsuccessful() throws Exception {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);
        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.class, "fromJson", Mockito.anyString()).thenThrow(exception);

        PowerMockito.mockStatic(CloudStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(CloudStackHttpToFogbowExceptionMapper.class, "get", Mockito.eq(exception));

        try {
            // exercise
            this.plugin.getVolumes(this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            PowerMockito.verifyStatic(CloudStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            CloudStackHttpToFogbowExceptionMapper.get(Mockito.eq(exception));

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                    Mockito.anyString());

            PowerMockito.verifyStatic(GetVolumeResponse.class, Mockito.times(TestUtils.RUN_ONCE));
            GetVolumeResponse.fromJson(Mockito.anyString());

            Assert.assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    // test case: When calling getNetworks with secondary methods mocked, it must verify if is returned the list of
    // networks correctly
    @Test
    public void testGetNetworksSuccessful() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        PowerMockito.mockStatic(GetNetworkResponse.class);

        GetNetworkResponse mockedResponse = Mockito.mock(GetNetworkResponse.class);
        List<Network> mockedNetworks = new ArrayList<>();

        Mockito.doReturn(mockedNetworks).when(mockedResponse).getNetworks();

        BDDMockito.given(GetNetworkResponse.fromJson(Mockito.anyString())).willReturn(mockedResponse);

        // exercise
        List<Network> networks = this.plugin.getNetworks(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                Mockito.anyString());

        PowerMockito.verifyStatic(GetNetworkResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetNetworkResponse.fromJson(Mockito.anyString());

        Assert.assertEquals(mockedNetworks, networks);
    }

    // test case: When calling getNetworks with secondary methods mocked and a exception occurs,
    // it must map with CloudStackHttpToFogbowExceptionMapper
    @Test
    public void testGetNetworksUnsuccessful() throws Exception {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);
        PowerMockito.mockStatic(GetNetworkResponse.class);
        PowerMockito.when(GetNetworkResponse.class, "fromJson", Mockito.anyString()).thenThrow(exception);

        PowerMockito.mockStatic(CloudStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(CloudStackHttpToFogbowExceptionMapper.class, "get", Mockito.eq(exception));

        try {
            // exercise
            this.plugin.getNetworks(this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            PowerMockito.verifyStatic(CloudStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            CloudStackHttpToFogbowExceptionMapper.get(Mockito.eq(exception));

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                    Mockito.anyString());

            PowerMockito.verifyStatic(GetNetworkResponse.class, Mockito.times(TestUtils.RUN_ONCE));
            GetNetworkResponse.fromJson(Mockito.anyString());

            Assert.assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    // test case: When calling getPublicIps with secondary methods mocked, it must verify if is returned the list of
    // public ip addresses correctly
    @Test
    public void testGetPublicIpsSuccessful() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        PowerMockito.mockStatic(ListPublicIpAddressResponse.class);

        ListPublicIpAddressResponse mockedResponse = Mockito.mock(ListPublicIpAddressResponse.class);
        List<PublicIpAddress> mockedPublicIps = new ArrayList<>();

        Mockito.doReturn(mockedPublicIps).when(mockedResponse).getPublicIpAddresses();

        BDDMockito.given(ListPublicIpAddressResponse.fromJson(Mockito.anyString())).willReturn(mockedResponse);

        // exercise
        List<PublicIpAddress> publicIps = this.plugin.getPublicIpAddresses(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                Mockito.anyString());

        PowerMockito.verifyStatic(ListPublicIpAddressResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        ListPublicIpAddressResponse.fromJson(Mockito.anyString());

        Assert.assertEquals(mockedPublicIps, publicIps);
    }

    // test case: When calling getPublicIps with secondary methods mocked and a exception occurs,
    // it must map with CloudStackHttpToFogbowExceptionMapper
    @Test
    public void testGetPublicIpsUnsuccessful() throws Exception {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetRequest(Mockito.eq(this.cloudUser), Mockito.anyString());

        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);
        PowerMockito.mockStatic(ListPublicIpAddressResponse.class);
        PowerMockito.when(ListPublicIpAddressResponse.class, "fromJson", Mockito.anyString()).thenThrow(exception);

        PowerMockito.mockStatic(CloudStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(CloudStackHttpToFogbowExceptionMapper.class, "get", Mockito.eq(exception));

        try {
            // exercise
            this.plugin.getPublicIpAddresses(this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            PowerMockito.verifyStatic(CloudStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            CloudStackHttpToFogbowExceptionMapper.get(Mockito.eq(exception));

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(this.cloudUser),
                    Mockito.anyString());

            PowerMockito.verifyStatic(ListPublicIpAddressResponse.class, Mockito.times(TestUtils.RUN_ONCE));
            ListPublicIpAddressResponse.fromJson(Mockito.anyString());

            Assert.assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    // test case: When calling getUsedAllocation with secondary methods mocked, it must verify if it is returned the
    // ResourceAllocation object correctly
    @Test
    public void testGetUsedAllocation() {
        // set up
        ArrayList<VirtualMachine> vms = new ArrayList<>();
        ArrayList<Volume> volumes = new ArrayList<>();
        ArrayList<Network> networks= new ArrayList<>();
        ArrayList<PublicIpAddress> publicIps = new ArrayList<>();

        ComputeAllocation computeAllocation = new ComputeAllocation(USED_CORES, USED_RAM, USED_INSTANCES);

        Mockito.doReturn(computeAllocation).when(this.plugin).buildComputeAllocation(Mockito.anyListOf(VirtualMachine.class));
        Mockito.doReturn(USED_DISK).when(this.plugin).getVolumeAllocation(Mockito.anyListOf(Volume.class));
        Mockito.doReturn(USED_NETWORKS).when(this.plugin).getNetworkAllocation(Mockito.anyListOf(Network.class));
        Mockito.doReturn(USED_PUBLIC_IP_ADDRESSES).when(this.plugin).getPublicIpAllocation(Mockito.anyListOf(PublicIpAddress.class));

        // exercise
        ResourceAllocation usedAllocation = this.plugin.getUsedAllocation(vms, volumes, networks, publicIps);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildComputeAllocation(Mockito.anyListOf(VirtualMachine.class));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVolumeAllocation(Mockito.anyListOf(Volume.class));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkAllocation(Mockito.anyListOf(Network.class));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPublicIpAllocation(Mockito.anyListOf(PublicIpAddress.class));

        Assert.assertEquals(USED_INSTANCES, usedAllocation.getInstances());
        Assert.assertEquals(USED_CORES, usedAllocation.getvCPU());
        Assert.assertEquals(USED_RAM, usedAllocation.getRam());
        Assert.assertEquals(USED_DISK, usedAllocation.getDisk());
        Assert.assertEquals(USED_NETWORKS, usedAllocation.getNetworks());
        Assert.assertEquals(USED_PUBLIC_IP_ADDRESSES, usedAllocation.getPublicIps());
    }

    // test case: When calling buildComputeAllocation method and given a list of virtual machines, it must returns a
    // ResourceAllocation.Builder with the total allocation (cores, ram and instances) of all virtual machines
    @Test
    public void testBuildComputeAllocation() {
        // set up
        VirtualMachine vm1 = Mockito.mock(VirtualMachine.class);
        VirtualMachine vm2 = Mockito.mock(VirtualMachine.class);

        int vm1Ram = 512;
        int vm1Cores = 1;

        int vm2Ram = 2048;
        int vm2Cores = 2;

        Mockito.doReturn(vm1Ram).when(vm1).getMemory();
        Mockito.doReturn(vm1Cores).when(vm1).getCpuNumber();

        Mockito.doReturn(vm2Ram).when(vm2).getMemory();
        Mockito.doReturn(vm2Cores).when(vm2).getCpuNumber();

        List<VirtualMachine> vms = new ArrayList<>();
        vms.add(vm1);
        vms.add(vm2);

        // exercise
        ComputeAllocation allocation = this.plugin.buildComputeAllocation(vms);

        // verify
        Assert.assertEquals(vms.size(), allocation.getInstances());
        Assert.assertEquals(vm1Cores + vm2Cores, allocation.getvCPU());
        Assert.assertEquals(vm1Ram + vm2Ram, allocation.getRam());
    }

    // test case: When calling getVolumeAllocation method given a list of volumes, it must returns the total disk
    // allocation of the volumes in gigabytes
    @Test
    public void testGetVolumeAllocation() {
        // set up
        Volume volume1 = Mockito.mock(Volume.class);
        Volume volume2 = Mockito.mock(Volume.class);
        Volume volume3 = Mockito.mock(Volume.class);

        // IN GIGABYTES
        long volume1Disk = 1;
        long volume2Disk = 2;
        long volume3Disk = 3;

        Mockito.doReturn(volume1Disk * ONE_GIGABYTE_IN_BYTES).when(volume1).getSize();
        Mockito.doReturn(volume2Disk * ONE_GIGABYTE_IN_BYTES).when(volume2).getSize();
        Mockito.doReturn(volume3Disk * ONE_GIGABYTE_IN_BYTES).when(volume3).getSize();

        List<Volume> volumes = new ArrayList<>();
        volumes.add(volume1);
        volumes.add(volume2);
        volumes.add(volume3);

        // exercise
        int volumeAllocation = this.plugin.getVolumeAllocation(volumes);

        // verify
        Assert.assertEquals(volume1Disk + volume2Disk + volume3Disk, volumeAllocation);
    }

    // test case: When calling getNetworkAllocation method given a list of networks, it must returns the total networks
    @Test
    public void testGetNetworkAllocation() {
        // set up
        Network network1 = Mockito.mock(Network.class);
        Network network2 = Mockito.mock(Network.class);
        Network network3 = Mockito.mock(Network.class);

        List<Network> networks = new ArrayList<>();
        networks.add(network1);
        networks.add(network2);
        networks.add(network3);

        // exercise
        int networkAllocation = this.plugin.getNetworkAllocation(networks);

        // verify
        Assert.assertEquals(networks.size(), networkAllocation);
    }

    // test case: When calling getPublicIpkAllocation method given a list of networks, it must returns the total networks
    @Test
    public void testGetPublicIpAllocation() {
        // set up
        PublicIpAddress publicIp1 = Mockito.mock(PublicIpAddress.class);
        PublicIpAddress publicIp2 = Mockito.mock(PublicIpAddress.class);
        PublicIpAddress publicIp3 = Mockito.mock(PublicIpAddress.class);

        List<PublicIpAddress> publicIps = new ArrayList<>();
        publicIps.add(publicIp1);
        publicIps.add(publicIp2);
        publicIps.add(publicIp3);

        // exercise
        int publicIpAllocation = this.plugin.getPublicIpAllocation(publicIps);

        // verify
        Assert.assertEquals(publicIps.size(), publicIpAllocation);
    }

    private List<ResourceLimit> buildAccountLimitedResources(int diskLimit) {
        List<ResourceLimit> resourceLimits = new ArrayList<>();

        resourceLimits.add(new ResourceLimit(INSTANCE_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_INSTANCES));
        resourceLimits.add(new ResourceLimit(CPU_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_CORES));
        resourceLimits.add(new ResourceLimit(RAM_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_RAM));
        resourceLimits.add(new ResourceLimit(STORAGE_RESOURCE_TYPE, FAKE_DOMAIN_ID, diskLimit));
        resourceLimits.add(new ResourceLimit(NETWORK_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_NETWORKS));
        resourceLimits.add(new ResourceLimit(PUBLIC_IP_RESOURCE_TYPE, FAKE_DOMAIN_ID, MAX_PUBLIC_IP_ADDRESSES));

        return resourceLimits;
    }

    private void ignoringCloudStackUrl() throws InvalidParameterException {
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();
    }
}
