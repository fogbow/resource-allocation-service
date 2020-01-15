package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.RestException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureClientCacheManager.class, AzureVirtualMachineSDK.class,})
public class AzureVirtualMachineOperationSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AzureVirtualMachineOperationSDK azureVirtualMachineOperationSDK;
    private AzureUser azureUser;
    private Azure azure;

    @Before
    public void setUp() {
        this.azureVirtualMachineOperationSDK =
                Mockito.spy(new AzureVirtualMachineOperationSDK());
        this.azureUser = Mockito.mock(AzureUser.class);
        this.azure = null;

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
    }

    // test case: When calling the doGetInstance method with methods mocked,
    // it must verify if It returns the right AzureGetVirtualMachineRef.
    @Test
    public void testDoGetInstanceSuccessfully() throws NoAvailableResourcesException, UnexpectedException,
            UnauthenticatedUserException, InstanceNotFoundException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        VirtualMachineSizeTypes virtualMachineSizeTypes = VirtualMachineSizeTypes.BASIC_A0;
        String virtualMachineSizeName = virtualMachineSizeTypes.toString();
        Mockito.when(virtualMachine.size()).thenReturn(virtualMachineSizeTypes);
        String cloudState = "cloudState";
        Mockito.when(virtualMachine.provisioningState()).thenReturn(cloudState);
        String name = "name";
        Mockito.when(virtualMachine.name()).thenReturn(name);
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        String privateIp = "privateIp";
        List<String> ipAddresses = Arrays.asList(privateIp);
        Mockito.when(networkInterface.primaryPrivateIP()).thenReturn(privateIp);
        Mockito.when(virtualMachine.getPrimaryNetworkInterface()).thenReturn(networkInterface);
        int diskSize = 3;
        Mockito.when(virtualMachine.osDiskSize()).thenReturn(diskSize);

        String regionName = this.azureUser.getRegionName();

        VirtualMachineSize virtualMachineSize = Mockito.mock(VirtualMachineSize.class);
        int memory = 1;
        Mockito.when(virtualMachineSize.memoryInMB()).thenReturn(memory);
        Integer vCPU = 2;
        Mockito.when(virtualMachineSize.numberOfCores()).thenReturn(vCPU);
        Mockito.doReturn(virtualMachineSize).when(this.azureVirtualMachineOperationSDK)
                .findVirtualMachineSizeByName(Mockito.eq(virtualMachineSizeName),
                        Mockito.eq(regionName), Mockito.eq(this.azure));

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(virtualMachine);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineById(
                Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineOptional);

        AzureGetVirtualMachineRef azureGetVirtualMachineRefExpected = AzureGetVirtualMachineRef.builder()
                .cloudState(cloudState)
                .ipAddresses(ipAddresses)
                .disk(diskSize)
                .memory(memory)
                .name(name)
                .vCPU(vCPU)
                .build();

        // exercise
        AzureGetVirtualMachineRef azureGetVirtualMachineRef =
                this.azureVirtualMachineOperationSDK.doGetInstance(instanceId, this.azureUser);

        // verify
        Assert.assertEquals(azureGetVirtualMachineRefExpected, azureGetVirtualMachineRef);
    }

    // test case: When calling the doGetInstance method with methods mocked and throw any exception,
    // it must verify if It retrows the same exception.
    @Test
    public void testDoGetInstanceFailWhenThrowException()
            throws UnauthenticatedUserException, UnexpectedException,
            InstanceNotFoundException, NoAvailableResourcesException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineById(
                Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenThrow(new UnexpectedException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        this.azureVirtualMachineOperationSDK.doGetInstance(instanceId, this.azureUser);
    }

    // test case: When calling the findVirtualMachineSizeByName method and find one the virtual machine size,
    // it must verify if It returns the right virtual machine size.
    @Test
    public void testFindVirtualMachineSizeByNameSuccessfully()
            throws UnauthenticatedUserException, NoAvailableResourcesException, UnexpectedException {
        // set up
        mockGetAzureClient();
        String virtualMachineSizeNameExpected = "nameExpected";

        Region region = Region.US_EAST;
        String regionName = region.name();
        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        VirtualMachineSize virtualMachineSizeNotMactchOne = buildVirtualMachineSizeMock("notmatch");
        VirtualMachineSize virtualMachineSizeMatch = buildVirtualMachineSizeMock(virtualMachineSizeNameExpected);
        VirtualMachineSize virtualMachineSizeNotMactchTwo = buildVirtualMachineSizeMock("notmatch");

        virtualMachines.add(virtualMachineSizeNotMactchOne);
        virtualMachines.add(virtualMachineSizeMatch);
        virtualMachines.add(virtualMachineSizeNotMactchTwo);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(this.azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // exercise
        VirtualMachineSize virtualMachineSize = this.azureVirtualMachineOperationSDK
                .findVirtualMachineSizeByName(virtualMachineSizeNameExpected, regionName, this.azure);

        // verify
        Assert.assertEquals(virtualMachineSizeMatch.name(), virtualMachineSize.name());
    }

    // test case: When calling the findVirtualMachineSizeByName method and does not find the virtual machine size,
    // it must verify if It throws a NoAvailableResourcesException exception.
    @Test
    public void testFindVirtualMachineSizeByNameFail()
            throws UnauthenticatedUserException, NoAvailableResourcesException, UnexpectedException {
        // set up
        mockGetAzureClient();
        String virtualMachineSizeNameExpected = "nameExpected";

        Region region = Region.US_EAST;
        String regionName = region.name();
        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        VirtualMachineSize virtualMachineSizeNotMactchOne = buildVirtualMachineSizeMock("notmatch");
        VirtualMachineSize virtualMachineSizeNotMactchTwo = buildVirtualMachineSizeMock("notmatch");

        virtualMachines.add(virtualMachineSizeNotMactchOne);
        virtualMachines.add(virtualMachineSizeNotMactchTwo);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(this.azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.azureVirtualMachineOperationSDK
                .findVirtualMachineSizeByName(virtualMachineSizeNameExpected, regionName, this.azure);
    }

    private void mockGetAzureClient() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);
    }

    private PagedList<VirtualMachineSize> getVirtualMachineSizesMock() {
        return new PagedList<VirtualMachineSize>() {
            @Override
            public Page<VirtualMachineSize> nextPage(String s) throws RestException {
                return null;
            }
        };
    }

    private VirtualMachineSize buildVirtualMachineSizeMock(String name) {
        return buildVirtualMachineSizeMock(0, 0, name);
    }

    private VirtualMachineSize buildVirtualMachineSizeMock(int memory, int vcpu, String name) {
        VirtualMachineSize virtualMachineSize = Mockito.mock(VirtualMachineSize.class);
        Mockito.when(virtualMachineSize.memoryInMB()).thenReturn(memory);
        Mockito.when(virtualMachineSize.numberOfCores()).thenReturn(vcpu);
        Mockito.when(virtualMachineSize.name()).thenReturn(name);
        return virtualMachineSize;
    }

}
