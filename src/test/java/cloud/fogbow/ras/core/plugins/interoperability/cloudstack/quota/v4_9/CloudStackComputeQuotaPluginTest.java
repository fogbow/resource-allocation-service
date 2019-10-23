package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@PrepareForTest({CloudStackUrlUtil.class, DatabaseManager.class, ListResourceLimitsResponse.class,
            CloudStackCloudUtils.class})
public class CloudStackComputeQuotaPluginTest extends BaseUnitTests {

    private CloudStackComputeQuotaPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackComputeQuotaPlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the getUserQuota method with secondary methods mocked,
    // it must verify if It returns the right computeQuota.
    @Test
    public void testGetUserQuotaSuccessfully() throws FogbowException {
        // set up
        ComputeAllocation totalComputeAllocationExpected = Mockito.mock(ComputeAllocation.class);
        Mockito.doReturn(totalComputeAllocationExpected)
                .when(this.plugin).buildTotalComputeAllocation(Mockito.eq(this.cloudStackUser));

        ComputeAllocation userComputeAllocationExpected = Mockito.mock(ComputeAllocation.class);
        Mockito.doReturn(userComputeAllocationExpected)
                .when(this.plugin).buildUsedComputeAllocation(Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeQuota computeQuota = this.plugin.getUserQuota(this.cloudStackUser);

        // verify
        Assert.assertEquals(totalComputeAllocationExpected, computeQuota.getTotalQuota());
        Assert.assertEquals(userComputeAllocationExpected, computeQuota.getUsedQuota());
    }

    // test case: When calling the getUserQuota method with secondary methods mocked and
    // it occurs a FogbowException on getting used quota, it must verify if
    // It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetUserQuotaFailWhenGettingUsedQuota() throws FogbowException {
        // set up
        ComputeAllocation totalComputeAllocationExpected = Mockito.mock(ComputeAllocation.class);
        Mockito.doReturn(totalComputeAllocationExpected)
                .when(this.plugin).buildTotalComputeAllocation(Mockito.eq(this.cloudStackUser));

        Mockito.doThrow(new FogbowException())
                .when(this.plugin).buildUsedComputeAllocation(Mockito.eq(this.cloudStackUser));

        // exercise
        try {
            this.plugin.getUserQuota(this.cloudStackUser);
            Assert.fail();
        } finally {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .buildUsedComputeAllocation(Mockito.eq(this.cloudStackUser));
        }
    }

    // test case: When calling the getUserQuota method with secondary methods mocked and
    // it occurs a FogbowException on getting total quota, it must verify if
    // It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetUserQuotaFailWhenGettingTotalQuota() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException())
                .when(this.plugin).buildTotalComputeAllocation(Mockito.eq(this.cloudStackUser));

        // exercise
        try {
            this.plugin.getUserQuota(this.cloudStackUser);
        } finally {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN))
                    .buildUsedComputeAllocation(Mockito.any());
        }
    }

    // test case: When calling the requestResourcesLimits method with secondary methods mocked,
    // it must verify if It returns the right ListResourceLimitsResponse.
    @Test
    public void testRequestResourcesLimitsSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder().build("");

        String responseStr = "anything";
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).thenReturn(responseStr);

        ListResourceLimitsResponse responseExpected = Mockito.mock(ListResourceLimitsResponse.class);
        PowerMockito.mockStatic(ListResourceLimitsResponse.class);
        PowerMockito.when(ListResourceLimitsResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(responseExpected);

        // exercise
        ListResourceLimitsResponse response = this.plugin.requestResourcesLimits(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: When calling the requestResourcesLimits method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testRequestResourcesLimitsFail() throws FogbowException, HttpResponseException {
        // set up
        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser)))
                .thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.requestResourcesLimits(request, this.cloudStackUser);
    }

    // test case: When calling the buildTotalComputeAllocation method with secondary methods mocked,
    // it must verify if It returns the right computeAllocation.
    @Test
    public void testBuildTotalComputeAllocationSuccessfully() throws FogbowException {
        // set up
        int instanceMaxExpected = 1;
        ListResourceLimitsResponse.ResourceLimit resourceLimitInstance = buildResourceLimitMock(
                CloudStackCloudUtils.INSTANCES_LIMIT_TYPE, instanceMaxExpected);
        int cpuMaxExpected = 2;
        ListResourceLimitsResponse.ResourceLimit resourceLimitCpu = buildResourceLimitMock(
                CloudStackCloudUtils.CPU_LIMIT_TYPE, cpuMaxExpected);
        int memoryMaxExpected = 3;
        ListResourceLimitsResponse.ResourceLimit resourceLimitMemory = buildResourceLimitMock(
                CloudStackCloudUtils.MEMORY_LIMIT_TYPE, memoryMaxExpected);

        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = new ArrayList<>();
        resourceLimits.add(resourceLimitInstance);
        resourceLimits.add(resourceLimitCpu);
        resourceLimits.add(resourceLimitMemory);

        Mockito.doReturn(resourceLimits).when(this.plugin).getResourcesLimits(Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.buildTotalComputeAllocation(this.cloudStackUser);

        // verify
        Assert.assertEquals(instanceMaxExpected, computeAllocation.getInstances());
        Assert.assertEquals(cpuMaxExpected, computeAllocation.getvCPU());
        Assert.assertEquals(memoryMaxExpected, computeAllocation.getRam());
    }

    // test case: When calling the buildTotalComputeAllocation method with secondary methods mocked
    // and some resource limit type does not match with types expected, it must verify if
    // It returns a computeAllocation with values default.
    @Test
    public void testBuildTotalComputeAllocationWhenTypeNoMatch() throws FogbowException {
        // set up
        int instanceMax = 1;
        String unknownResourceLimitType = "anytype";
        ListResourceLimitsResponse.ResourceLimit resourceLimitInstance = buildResourceLimitMock(
                unknownResourceLimitType, instanceMax);

        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = new ArrayList<>();
        resourceLimits.add(resourceLimitInstance);

        Mockito.doReturn(resourceLimits).when(this.plugin).getResourcesLimits(Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.buildTotalComputeAllocation(this.cloudStackUser);

        // verify
        Assert.assertEquals(Integer.MAX_VALUE, computeAllocation.getInstances());
        Assert.assertEquals(Integer.MAX_VALUE, computeAllocation.getvCPU());
        Assert.assertEquals(Integer.MAX_VALUE, computeAllocation.getRam());
    }

    // test case: When calling the buildTotalComputeAllocation method with secondary methods mocked
    // and a resource limit fail in the normalization, it must verify if
    // It returns a computeAllocation value default value related to the Resource limit failed.
    @Test
    public void testBuildTotalComputeAllocationWhenResourceLimitFail() throws FogbowException {
        // set up
        int instanceMaxExpected = 1;
        ListResourceLimitsResponse.ResourceLimit resourceLimitInstance = buildResourceLimitMock(
                CloudStackCloudUtils.INSTANCES_LIMIT_TYPE, instanceMaxExpected);
        int cpuMaxExpected = 2;
        ListResourceLimitsResponse.ResourceLimit resourceLimitCpu = buildResourceLimitMock(
                CloudStackCloudUtils.CPU_LIMIT_TYPE, cpuMaxExpected);
        int memoryMaxExpected = CloudStackCloudUtils.UNLIMITED_ACCOUNT_QUOTA;
        ListResourceLimitsResponse.ResourceLimit resourceLimitMemoryFail = buildResourceLimitMock(
                CloudStackCloudUtils.MEMORY_LIMIT_TYPE, memoryMaxExpected);

        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = new ArrayList<>();
        resourceLimits.add(resourceLimitInstance);
        resourceLimits.add(resourceLimitCpu);
        resourceLimits.add(resourceLimitMemoryFail);

        Mockito.doThrow(new FogbowException()).when(this.plugin).normalizeResourceLimit(
                Mockito.eq(resourceLimitMemoryFail), Mockito.eq(this.cloudStackUser));

        Mockito.doReturn(resourceLimits).when(this.plugin).getResourcesLimits(Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.buildTotalComputeAllocation(this.cloudStackUser);

        // verify
        Assert.assertEquals(instanceMaxExpected, computeAllocation.getInstances());
        Assert.assertEquals(cpuMaxExpected, computeAllocation.getvCPU());
        Assert.assertEquals(Integer.MAX_VALUE, computeAllocation.getRam());
        // TODO(chico) - Check Log
    }

    // test case: When calling the buildTotalComputeAllocation method with occurs a FogbowException,
    // it must verify if It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testBuildTotalComputeAllocationFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin).
                getResourcesLimits(Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.buildTotalComputeAllocation(this.cloudStackUser);
    }

    // test case: When calling the buildUsedComputeAllocation method with secondary methods mocked,
    // it must verify if It returns the right computeAllocation.
    @Test
    public void testBuildUsedComputeAllocationSuccessfully() throws FogbowException {
        // set up
        int cpuOne = 1;
        int memoryOne = 1000;
        GetVirtualMachineResponse.VirtualMachine virtualMachineOne = buildVirtualMachineMock(
                memoryOne, cpuOne);

        int cpuTwo = 1;
        int memoryTwo = 1000;
        GetVirtualMachineResponse.VirtualMachine virtualMachineTwo = buildVirtualMachineMock(
                memoryTwo, cpuTwo);

        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();
        virtualMachines.add(virtualMachineOne);
        virtualMachines.add(virtualMachineTwo);

        Mockito.doReturn(virtualMachines).when(this.plugin).getVirtualMachines(
                Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.buildUsedComputeAllocation(this.cloudStackUser);

        // verify
        Assert.assertEquals(cpuOne + cpuTwo, computeAllocation.getvCPU());
        Assert.assertEquals(memoryOne + memoryTwo, computeAllocation.getRam());
        Assert.assertEquals(virtualMachines.size(), computeAllocation.getInstances());
    }

    // test case: When calling the buildUsedComputeAllocation method with secondary methods mocked
    // and there is no virtual machine, it must verify if It returns the right computeAllocation
    // with initial values.
    @Test
    public void testBuildUsedComputeAllocationWhenNoVirtualMachine() throws FogbowException {
        // set up
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = new ArrayList<>();

        Mockito.doReturn(virtualMachines).when(this.plugin).getVirtualMachines(
                Mockito.eq(this.cloudStackUser));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.buildUsedComputeAllocation(this.cloudStackUser);

        // verify
        Assert.assertEquals(0, computeAllocation.getvCPU());
        Assert.assertEquals(0, computeAllocation.getRam());
        Assert.assertEquals(virtualMachines.size(), computeAllocation.getInstances());
    }

    // test case: When calling the buildUsedComputeAllocation method with occurs a FogbowException,
    // it must verify if It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testBuildUsedComputeAllocationFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin).getVirtualMachines(
                Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.buildUsedComputeAllocation(this.cloudStackUser);
    }

    // test case: When calling the getResourcesLimits method with secondary methods mocked,
    // it must verify if the requestResourcesLimits is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void  testGetResourcesLimitsSuccessfully() throws FogbowException {
        // set up
        ListResourceLimitsResponse response = Mockito.mock(ListResourceLimitsResponse.class);
        ArrayList<ListResourceLimitsResponse.ResourceLimit> resourceLimitsExpected = new ArrayList<>();
        Mockito.when(response.getResourceLimits()).thenReturn(resourceLimitsExpected);
        Mockito.doReturn(response).when(this.plugin).requestResourcesLimits(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .build(this.cloudStackUrl);

        // exercise
        List<ListResourceLimitsResponse.ResourceLimit> resourcesLimits =
                this.plugin.getResourcesLimits(this.cloudStackUser);

        // verify
        Assert.assertEquals(resourceLimitsExpected, resourcesLimits);
        RequestMatcher<ListResourceLimitsRequest> matcher = new RequestMatcher.ListResourceLimits(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).requestResourcesLimits(
                Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getVirtualMachines method with secondary methods mocked,
    // it must verify if the CloudStackCloudUtils.requestGetVirtualMachine is called with the
    // right parameters; this includes the checking of the Cloudstack request.
    @Test
    public void  testGetVirtualMachinesSuccessfully() throws FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        GetVirtualMachineResponse response = Mockito.mock(GetVirtualMachineResponse.class);
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachinesExpected = new ArrayList<>();
        Mockito.when(response.getVirtualMachines()).thenReturn(virtualMachinesExpected);
        PowerMockito.when(CloudStackCloudUtils.requestGetVirtualMachine(
                Mockito.eq(this.client), Mockito.any(), Mockito.eq(this.cloudStackUser)))
                .thenReturn(response);

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .build(this.cloudStackUrl);

        // exercise
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines =
                this.plugin.getVirtualMachines(this.cloudStackUser);

        // verify
        Assert.assertEquals(virtualMachinesExpected, virtualMachines);
        RequestMatcher<GetVirtualMachineRequest> matcher = new RequestMatcher.GetVirtualMachine(request);
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackCloudUtils.requestGetVirtualMachine(
                Mockito.eq(this.client), Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getDomainResourceLimit method with secondary methods mocked,
    // it must verify if the requestResourcesLimits is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetDomainResourceLimitSuccessfully() throws FogbowException {
        // set up
        String resourceType = "resourceType";
        String domainId = "doaminId";

        ListResourceLimitsResponse response = Mockito.mock(ListResourceLimitsResponse.class);
        ListResourceLimitsResponse.ResourceLimit resourceLimitExpected = Mockito.mock(
                ListResourceLimitsResponse.ResourceLimit.class);
        List<ListResourceLimitsResponse.ResourceLimit> resourcesLimit = new ArrayList<>();
        resourcesLimit.add(resourceLimitExpected);
        Mockito.when(response.getResourceLimits()).thenReturn(resourcesLimit);
        Mockito.doReturn(response).when(this.plugin).requestResourcesLimits(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(this.cloudStackUrl);

        // exercise
        ListResourceLimitsResponse.ResourceLimit domainResourceLimit =
                this.plugin.getDomainResourceLimit(resourceType, domainId, this.cloudStackUser);

        // verify
        Assert.assertEquals(resourceLimitExpected, domainResourceLimit);
        RequestMatcher<ListResourceLimitsRequest> matcher = new RequestMatcher.ListResourceLimits(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).requestResourcesLimits(
                Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getDomainResourceLimit method with secondary methods mocked and
    // it occurs an FogbowException, it must verify if It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetDomainResourceLimitFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin).requestResourcesLimits(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.getDomainResourceLimit("resourceType", "domainId", this.cloudStackUser);
    }

    // test case: When calling the normalizeResourceLimit method with secondary methods mocked and
    // the unlimited value in the Max parameter, it must verify if it returns the domain resource limit.
    @Test
    public void testNormalizeResourceLimitWhenMaxIsUnlimited() throws FogbowException {
        // set up
        Integer unlimited = CloudStackCloudUtils.UNLIMITED_ACCOUNT_QUOTA;
        String resourceType = "resourceType";
        String domainId = "domainId";

        ListResourceLimitsResponse.ResourceLimit resourceLimit =
                Mockito.mock(ListResourceLimitsResponse.ResourceLimit.class);
        Mockito.when(resourceLimit.getMax()).thenReturn(unlimited);
        Mockito.when(resourceLimit.getResourceType()).thenReturn(resourceType);
        Mockito.when(resourceLimit.getDomainId()).thenReturn(domainId);

        ListResourceLimitsResponse.ResourceLimit domainResourceLimitExpected =
                Mockito.mock(ListResourceLimitsResponse.ResourceLimit.class);
        Mockito.doReturn(domainResourceLimitExpected).when(this.plugin).getDomainResourceLimit(
                Mockito.eq(resourceType), Mockito.eq(domainId), Mockito.eq(this.cloudStackUser));

        // exercise
        ListResourceLimitsResponse.ResourceLimit domainResourceLimit =
                this.plugin.normalizeResourceLimit(resourceLimit, this.cloudStackUser);

        // verify
        Assert.assertEquals(domainResourceLimitExpected, domainResourceLimit);
    }

    // test case: When calling the normalizeResourceLimit method with secondary methods mocked and
    // a common value in the Max parameter, it must verify if it returns the some resource limit
    // passed in the method.
    @Test
    public void testNormalizeResourceLimitWhenMaxIslimited() throws FogbowException {
        // set up
        Integer limited = 0;

        ListResourceLimitsResponse.ResourceLimit resourceLimit =
                Mockito.mock(ListResourceLimitsResponse.ResourceLimit.class);
        Mockito.when(resourceLimit.getMax()).thenReturn(limited);

        // exercise
        ListResourceLimitsResponse.ResourceLimit domainResourceLimit =
                this.plugin.normalizeResourceLimit(resourceLimit, this.cloudStackUser);

        // verify
        Assert.assertEquals(resourceLimit, domainResourceLimit);
    }

    // test case: When calling the normalizeResourceLimit method with secondary methods mocked and
    // the method getDomainResourceLimit throws a FogbowException, it must verify if it throws
    // a FogbowException.
    @Test(expected = FogbowException.class)
    public void testNormalizeResourceLimitFail() throws FogbowException {
        // set up
        Integer unlimited = CloudStackCloudUtils.UNLIMITED_ACCOUNT_QUOTA;
        String resourceType = "resourceType";
        String domainId = "domainId";

        ListResourceLimitsResponse.ResourceLimit resourceLimit =
                Mockito.mock(ListResourceLimitsResponse.ResourceLimit.class);
        Mockito.when(resourceLimit.getMax()).thenReturn(unlimited);
        Mockito.when(resourceLimit.getResourceType()).thenReturn(resourceType);
        Mockito.when(resourceLimit.getDomainId()).thenReturn(domainId);

        Mockito.doThrow(new FogbowException()).when(this.plugin).getDomainResourceLimit(
                Mockito.eq(resourceType), Mockito.eq(domainId), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.normalizeResourceLimit(resourceLimit, this.cloudStackUser);
    }

    private ListResourceLimitsResponse.ResourceLimit buildResourceLimitMock(String type, int maxValue) {
        ListResourceLimitsResponse.ResourceLimit resourceLimitMocked = Mockito.mock(
                ListResourceLimitsResponse.ResourceLimit.class);
        Mockito.when(resourceLimitMocked.getResourceType()).thenReturn(type);
        Mockito.when(resourceLimitMocked.getMax()).thenReturn(maxValue);
        return resourceLimitMocked;
    }

    private GetVirtualMachineResponse.VirtualMachine buildVirtualMachineMock(int memory, int cpu) {
        GetVirtualMachineResponse.VirtualMachine virtualMachineMocked = Mockito.mock(
                GetVirtualMachineResponse.VirtualMachine.class);
        Mockito.when(virtualMachineMocked.getMemory()).thenReturn(memory);
        Mockito.when(virtualMachineMocked.getCpuNumber()).thenReturn(cpu);
        return virtualMachineMocked;
    }

}
