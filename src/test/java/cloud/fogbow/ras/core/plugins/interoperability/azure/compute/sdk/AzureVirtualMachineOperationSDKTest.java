package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureNetworkSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import com.microsoft.rest.RestException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureClientCacheManager.class, AzureVirtualMachineSDK.class,
        AzureNetworkSDK.class, AzureVolumeSDK.class})
public class AzureVirtualMachineOperationSDKTest {

    private static final Logger LOGGER_CLASS_MOCK = Logger.getLogger(AzureVirtualMachineOperationSDK.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LoggerAssert loggerAssert = new LoggerAssert(AzureVirtualMachineOperationSDK.class);
    private AzureVirtualMachineOperationSDK azureVirtualMachineOperationSDK;
    private AzureUser azureCloudUser;
    private Azure azure;

    @Before
    public void setUp() {
        this.azureVirtualMachineOperationSDK =
                Mockito.spy(new AzureVirtualMachineOperationSDK(AzureTestUtils.DEFAULT_REGION_NAME));
        this.azureCloudUser = Mockito.mock(AzureUser.class);
        this.azure = null;
        makeTheObservablesSynchronous();

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
    }


    // test case: When calling the doGetInstance method with methods mocked,
    // it must verify if It returns the right AzureGetVirtualMachineRef.
    @Test
    public void testDoGetInstanceSuccessfully() throws FogbowException {

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

        String regionName = AzureTestUtils.DEFAULT_REGION_NAME;

        VirtualMachineSize virtualMachineSize = Mockito.mock(VirtualMachineSize.class);
        int memory = 1;
        Mockito.when(virtualMachineSize.memoryInMB()).thenReturn(memory);
        Integer vCPU = 2;
        Mockito.when(virtualMachineSize.numberOfCores()).thenReturn(vCPU);
        Mockito.doReturn(virtualMachineSize).when(this.azureVirtualMachineOperationSDK)
                .findVirtualMachineSize(Mockito.eq(virtualMachineSizeName),
                        Mockito.eq(regionName), Mockito.eq(this.azure));

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(virtualMachine);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachine(
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
                this.azureVirtualMachineOperationSDK.doGetInstance(instanceId, this.azureCloudUser);

        // verify
        Assert.assertEquals(azureGetVirtualMachineRefExpected, azureGetVirtualMachineRef);
    }

    // test case: When calling the doGetInstance method with methods mocked and throw any exception,
    // it must verify if It retrows the same exception.
    @Test
    public void testDoGetInstanceFailWhenThrowException() throws FogbowException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachine(
                Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenThrow(new UnexpectedException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        this.azureVirtualMachineOperationSDK.doGetInstance(instanceId, this.azureCloudUser);
    }

    // test case: When calling the findVirtualMachineSize method and find one the virtual machine size,
    // it must verify if It returns the right virtual machine size.
    @Test
    public void testFindVirtualMachineSizeSuccessfully() throws FogbowException {
        // set up
        mockGetAzureClient();
        String virtualMachineSizeExpected = "nameExpected";

        Region region = Region.US_EAST;
        String regionName = region.name();
        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        VirtualMachineSize virtualMachineSizeNotMactchOne = buildVirtualMachineSizeMock("notmatch");
        VirtualMachineSize virtualMachineSizeMatch = buildVirtualMachineSizeMock(virtualMachineSizeExpected);
        VirtualMachineSize virtualMachineSizeNotMactchTwo = buildVirtualMachineSizeMock("notmatch");

        virtualMachines.add(virtualMachineSizeNotMactchOne);
        virtualMachines.add(virtualMachineSizeMatch);
        virtualMachines.add(virtualMachineSizeNotMactchTwo);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(this.azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // exercise
        VirtualMachineSize virtualMachineSize = this.azureVirtualMachineOperationSDK
                .findVirtualMachineSize(virtualMachineSizeExpected, regionName, this.azure);

        // verify
        Assert.assertEquals(virtualMachineSizeMatch.name(), virtualMachineSize.name());
    }

    // test case: When calling the findVirtualMachineSize method and does not find the virtual machine size,
    // it must verify if It throws a NoAvailableResourcesException exception.
    @Test
    public void testFindVirtualMachineSizeFail()
            throws FogbowException {
        // set up
        mockGetAzureClient();
        String virtualMachineSizeExpected = "nameExpected";

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
                .findVirtualMachineSize(virtualMachineSizeExpected, regionName, this.azure);
    }


    // test case: When calling the findVirtualMachineSize method with two virtual machines size name that
    // fits in the requirements, it must verify if It returns the smaller virtual machines size.
    @Test
    public void testFindVirtualMachineSizeWithTwoVirtualMachinesSize() throws FogbowException {

        // set up
        mockGetAzureClient();

        int memory = 1;
        int vcpu = 2;
        Region region = Region.US_EAST;
        String regionName = region.name();

        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        int lessThanMemoryRequired = memory - 1;
        int lessThanCpuRequired = vcpu - 1;
        VirtualMachineSize virtualMachineSizeNotFits = buildVirtualMachineSizeMock(lessThanMemoryRequired, lessThanCpuRequired);
        VirtualMachineSize virtualMachineSizeFitsSmaller = buildVirtualMachineSizeMock(memory, vcpu);
        VirtualMachineSize virtualMachineSizeFitsBigger = buildVirtualMachineSizeMock(Integer.MAX_VALUE, Integer.MAX_VALUE);

        virtualMachines.add(virtualMachineSizeNotFits);
        virtualMachines.add(virtualMachineSizeFitsSmaller);
        virtualMachines.add(virtualMachineSizeFitsBigger);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(this.azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // exercise
        VirtualMachineSize virtualMachineSize = this.azureVirtualMachineOperationSDK
                .findVirtualMachineSize(memory, vcpu, regionName, this.azureCloudUser);

        // verify
        Assert.assertNotEquals(virtualMachineSizeFitsBigger, virtualMachineSize);
        Assert.assertEquals(virtualMachineSizeFitsSmaller, virtualMachineSize);
    }

    // test case: When calling the findVirtualMachineSize method with any virtual machine size name that
    // fits in the requirements, it must verify if It throws a NoAvailableResourcesException.
    @Test
    public void testFindVirtualMachineSizeFailWhenSizeThatNotFits() throws FogbowException {

        // set up
        mockGetAzureClient();

        int memory = 1;
        int vcpu = 2;
        Region region = Region.US_EAST;
        String regionName = region.name();

        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        int lessThanMemoryRequired = memory - 1;
        int lessThanCpuRequired = vcpu - 1;
        VirtualMachineSize virtualMachineSizeNotFits = buildVirtualMachineSizeMock(lessThanMemoryRequired, lessThanCpuRequired);
        virtualMachines.add(virtualMachineSizeNotFits);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(this.azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.azureVirtualMachineOperationSDK.findVirtualMachineSize(memory, vcpu, regionName, this.azureCloudUser);
    }

    // test case: When calling the findVirtualMachineSizeName method with throws an Unauthorized
    // exception, it must verify if It throws an Unauthorized exception.
    @Test
    public void testFindVirtualMachineSizeFailWhenThrowUnauthorized() throws FogbowException {

        // set up
        mockGetAzureClientUnauthorized();
        int memory = 1;
        int vcpu = 2;
        String regionName = Region.US_EAST.name();

        // verify
        this.expectedException.expect(UnauthenticatedUserException.class);

        // exercise
        this.azureVirtualMachineOperationSDK.findVirtualMachineSize(
                memory, vcpu, regionName, this.azureCloudUser);
    }

    // test case: When calling the subscribeCreateVirtualMachine method and the observable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeCreateVirtualMachineSuccessfully() {
        // set up
        Observable<Indexable> virtualMachineObservable = createSimpleObservableSuccess();

        // exercise
        this.azureVirtualMachineOperationSDK.subscribeCreateVirtualMachine(virtualMachineObservable);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeCreateVirtualMachine method and the observable executes
    // with an error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeCreateVirtualMachineFail() {
        // set up
        Observable<Indexable> virtualMachineObservable = createSimpleObservableFail();

        // exercise
        this.azureVirtualMachineOperationSDK.subscribeCreateVirtualMachine(virtualMachineObservable);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_CREATE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doCreateInstance method, it must verify if It finishes without error.
    @Test
    public void testDoCreateInstanceSuccessfully() throws FogbowException {

        // set up
        mockGetAzureClient();
        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .build();

        Observable<Indexable> observableMocked = Mockito.mock(Observable.class);
        Mockito.doReturn(observableMocked).when(this.azureVirtualMachineOperationSDK).buildAzureVirtualMachineObservable(
                Mockito.eq(azureCreateVirtualMachineRef), Mockito.eq(this.azure));

        Mockito.doNothing().when(this.azureVirtualMachineOperationSDK)
                .subscribeCreateVirtualMachine(Mockito.any());

        // exercise
        this.azureVirtualMachineOperationSDK.doCreateInstance(azureCreateVirtualMachineRef, this.azureCloudUser);

        // verify
        Mockito.verify(this.azureVirtualMachineOperationSDK, Mockito.times(1))
                .subscribeCreateVirtualMachine(Mockito.eq(observableMocked));
    }

    // test case: When calling the buildAzureVirtualMachineObservable method, it must verify if
    // It calls the method with the right parameters.
    @Test
    public void testBuildAzureVirtualMachineObservable() throws FogbowException {
        // set up
        String imagePublishedExpected = "publisher";
        String imageSkuExpected = "sku";
        String imageOfferExpected = "offer";
        AzureGetImageRef azureVirtualMachineImageExpected =
                new AzureGetImageRef(imagePublishedExpected, imageOfferExpected, imageSkuExpected);
        String virtualMachineNameExpected = "virtualMachineNameExpected";
        String networkInterfaceIdExpected = "networkInterfaceIdExpected";
        int diskSize = 1;
        String virtualMachineSizeNameExpected = "virtualMachineSizeNameExpected";
        String osComputeNameExpected = "osComputeNameExpected";
        String osUserNameExpected = "osUserNameExpected";
        String osUserPasswordExpected = "osUserPasswordExpected";
        String regionNameExpected = "regionNameExpected";
        String resourceGroupNameExpected = "resourceGroupNameExpected";
        String userDataExpected = "userDataExpected";

        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .virtualMachineName(virtualMachineNameExpected)
                .azureGetImageRef(azureVirtualMachineImageExpected)
                .networkInterfaceId(networkInterfaceIdExpected)
                .diskSize(diskSize)
                .size(virtualMachineSizeNameExpected)
                .osComputeName(osComputeNameExpected)
                .osUserName(osUserNameExpected)
                .osUserPassword(osUserPasswordExpected)
                .regionName(regionNameExpected)
                .resourceGroupName(resourceGroupNameExpected)
                .userData(userDataExpected)
                .checkAndBuild();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        NetworkInterface networkInterfaceExcepted = Mockito.mock(NetworkInterface.class);
        Optional<NetworkInterface> networkInterfaceExpectedOptional = Optional.ofNullable(networkInterfaceExcepted);
        PowerMockito.when(AzureNetworkSDK
                .getNetworkInterface(Mockito.eq(this.azure), Mockito.eq(networkInterfaceIdExpected)))
                .thenReturn(networkInterfaceExpectedOptional);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        Region regionExpected = Region.fromName(regionNameExpected);

        // exercise
        this.azureVirtualMachineOperationSDK.buildAzureVirtualMachineObservable(
                azureCreateVirtualMachineRef, this.azure);

        // verify
        PowerMockito.verifyStatic(AzureVirtualMachineSDK.class, VerificationModeFactory.times(1));
        AzureVirtualMachineSDK.buildVirtualMachineObservable(
                Mockito.eq(this.azure), Mockito.eq(virtualMachineNameExpected), Mockito.eq(regionExpected),
                Mockito.eq(resourceGroupNameExpected), Mockito.eq(networkInterfaceExcepted),
                Mockito.eq(imagePublishedExpected), Mockito.eq(imageOfferExpected), Mockito.eq(imageSkuExpected),
                Mockito.eq(osUserNameExpected), Mockito.eq(osUserPasswordExpected), Mockito.eq(osComputeNameExpected),
                Mockito.eq(userDataExpected), Mockito.eq(diskSize), Mockito.eq(virtualMachineSizeNameExpected));
    }

    // test case: When calling the doDeleteInstance method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        String completableMessageOne = "completableMessageOne";
        Completable completableOne = createSimpleCompletableSuccess(completableMessageOne);
        mockDeleteVirtualMachineCompletable(instanceId, completableOne);

        String completableMessageTwo = "completableMessageOne";
        Completable completableTwo = createSimpleCompletableSuccess(completableMessageTwo);
        mockDeleteVirtualMachineDiskCompletable(instanceId, completableTwo);

        // exercise
        this.azureVirtualMachineOperationSDK.doDeleteInstance(instanceId, this.azureCloudUser);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, completableMessageOne)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.DEBUG, completableMessageTwo)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doDeleteInstance method and the completable executes
    // with error in the delete virtual machine, it must verify if It returns the right logs and
    // does not execute the delete virtual machine disk.
    @Test
    public void testDoDeleteInstanceFailOnVirtualMachineDeletion() throws FogbowException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        String completableMessageOne = "completableMessageOne";
        Completable completableOne = createSimpleCompletableFail(completableMessageOne);
        mockDeleteVirtualMachineCompletable(instanceId, completableOne);

        String completableMessageTwo = "completableMessageOne";
        Completable completableTwo = createSimpleCompletableSuccess(completableMessageTwo);
        mockDeleteVirtualMachineDiskCompletable(instanceId, completableTwo);

        // exercise
        this.azureVirtualMachineOperationSDK.doDeleteInstance(instanceId, this.azureCloudUser);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, completableMessageOne)
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_VM_ASYNC_BEHAVIOUR)
                .verifyLogEnd();
    }

    // test case: When calling the doDeleteInstance method and the completable executes
    // with error in the delete virtual machine disk, it must verify if It returns the right logs.
    @Test
    public void testDoDeleteInstanceFailOnVirtualMachineDiskDeletion() throws FogbowException {

        // set up
        mockGetAzureClient();
        String instanceId = "instanceId";

        String completableMessageOne = "completableMessageOne";
        Completable completableOne = createSimpleCompletableSuccess(completableMessageOne);
        mockDeleteVirtualMachineCompletable(instanceId, completableOne);

        String completableMessageTwo = "completableMessageOne";
        Completable completableTwo = createSimpleCompletableFail(completableMessageTwo);
        mockDeleteVirtualMachineDiskCompletable(instanceId, completableTwo);

        // exercise
        this.azureVirtualMachineOperationSDK.doDeleteInstance(instanceId, this.azureCloudUser);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, completableMessageOne)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.DEBUG, completableMessageTwo)
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR)
                .verifyLogEnd();
    }


    // test case: When calling the buildDeleteVirtualMachineDiskCompletable method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualMachineDiskCompletableSuccessfully() throws FogbowException {

        // set up
        String instanceId = "instanceId";
        String compĺetableMessage = "compĺetableMessage";
        Completable simpleCompletableSuccess = createSimpleCompletableSuccess(compĺetableMessage);
        mockDeleteVirtualMachineDiskCompletable(instanceId, simpleCompletableSuccess);

        // exercise
        Completable completable = this.azureVirtualMachineOperationSDK.buildDeleteVirtualMachineDiskCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, compĺetableMessage)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualMachineDiskCompletable method and the completable executes
    // with error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualMachineDiskCompletableFail() throws FogbowException {

        // set up
        String instanceId = "instanceId";
        Completable simpleCompletableFail = createSimpleCompletableFail();
        mockDeleteVirtualMachineDiskCompletable(instanceId, simpleCompletableFail);

        // exercise
        Completable completable = this.azureVirtualMachineOperationSDK.buildDeleteVirtualMachineDiskCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualMachineDiskCompletable method and throws a exception because
    // it does not found the disk, it must verify if It throws a InstanceNotFoundException.
    @Test
    public void testBuildDeleteVirtualMachineDiskCompletableFailWhenNotFindDisk() throws FogbowException {

        // set up
        String instanceId = "instanceId";

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(null);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachine(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineOptional);

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.azureVirtualMachineOperationSDK.buildDeleteVirtualMachineDiskCompletable(this.azure, instanceId);
    }

    // test case: When calling the buildDeleteVirtualMachineCompletable method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualMachineCompletableSuccessfully() {
        // set up
        String instanceId = "instanceId";
        Completable virtualMachineCompletableSuccess = createSimpleCompletableSuccess();

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK
                .buildDeleteVirtualMachineCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineCompletableSuccess);

        // exercise
        Completable completable = this.azureVirtualMachineOperationSDK
                .buildDeleteVirtualMachineCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualMachineCompletable method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualMachineCompletableFail() {
        // set up
        String instanceId = "instanceId";

        Completable virtualMachineCompletableFail = createSimpleCompletableFail();
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK
                .buildDeleteVirtualMachineCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineCompletableFail);

        // exercise
        Completable completable = this.azureVirtualMachineOperationSDK
                .buildDeleteVirtualMachineCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_VM_ASYNC_BEHAVIOUR);
    }


    private PagedList<VirtualMachineSize> getVirtualMachineSizesMock() {
        return new PagedList<VirtualMachineSize>() {
            @Override
            public Page<VirtualMachineSize> nextPage(String s) throws RestException {
                return null;
            }
        };
    }

    private void mockGetAzureClient() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureCloudUser)))
                .thenReturn(azure);
    }

    private void mockGetAzureClientUnauthorized() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureCloudUser)))
                .thenThrow(new UnauthenticatedUserException());
    }

    private void makeTheObservablesSynchronous() {
        // The scheduler trampolime makes the subscriptions execute in the current thread
        this.azureVirtualMachineOperationSDK.setScheduler(Schedulers.trampoline());
    }

    private Completable createSimpleCompletableSuccess() {
        return Completable.complete();
    }

    private Completable createSimpleCompletableSuccess(String message) {
        return Completable.create((completableSubscriber) -> {
            LOGGER_CLASS_MOCK.debug(message);
            completableSubscriber.onCompleted();
        });
    }

    private Completable createSimpleCompletableFail() {
        return Completable.error(new RuntimeException());
    }

    private Completable createSimpleCompletableFail(String message) {
        return Completable.create((completableSubscriber) -> {
            LOGGER_CLASS_MOCK.debug(message);
            completableSubscriber.onError(new RuntimeException());
        });
    }

    private Observable<Indexable> createSimpleObservableSuccess() {
        return Observable.defer(() -> {
            Indexable indexable = Mockito.mock(Indexable.class);
            return Observable.just(indexable);
        });
    }

    private Observable<Indexable> createSimpleObservableFail() {
        return Observable.defer(() -> {
            throw new RuntimeException();
        });
    }

    private VirtualMachineSize buildVirtualMachineSizeMock(int memory, int vcpu) {
        String name = RandomStringUtils.randomAlphabetic(10);
        return buildVirtualMachineSizeMock(memory, vcpu, name);
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

    private void mockDeleteVirtualMachineDiskCompletable(String instanceId, Completable deleteDiskCompletable)
            throws UnexpectedException {

        VirtualMachine virtalMachine = Mockito.mock(VirtualMachine.class);
        String osDiskId = "osDiskId";
        Mockito.when(virtalMachine.osDiskId()).thenReturn(osDiskId);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(virtalMachine);
        PowerMockito.when(AzureVirtualMachineSDK
                .getVirtualMachine(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineOptional);

        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.when(AzureVolumeSDK
                .buildDeleteDiskCompletable(Mockito.eq(this.azure), Mockito.eq(osDiskId)))
                .thenReturn(deleteDiskCompletable);
    }

    private void mockDeleteVirtualMachineCompletable(String instanceId, Completable deleteVMCompletable) {
        PowerMockito.when(AzureVirtualMachineSDK.buildDeleteVirtualMachineCompletable(
                Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(deleteVMCompletable);
    }

}
