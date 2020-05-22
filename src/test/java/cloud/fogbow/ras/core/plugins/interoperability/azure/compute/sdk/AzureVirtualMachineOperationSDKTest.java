package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureNetworkSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.compute.implementation.VirtualMachineInner;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import com.microsoft.rest.RestException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Level;
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

import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Azure.class,
    AzureClientCacheManager.class,
    AzureGeneralUtil.class,
    AzureNetworkSDK.class,
    AzureResourceGroupOperationUtil.class,
    AzureVirtualMachineSDK.class,
    AzureVolumeSDK.class
})
public class AzureVirtualMachineOperationSDKTest {

    private static final Logger LOGGER_CLASS_MOCK = Logger.getLogger(AzureVirtualMachineOperationSDK.class);

    private LoggerAssert loggerAssert = new LoggerAssert(AzureVirtualMachineOperationSDK.class);
    private AzureVirtualMachineOperationSDK operation;
    private AzureUser azureUser;
    private String regionName;
    private String defaultResourceGroupName;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureUser();
        this.regionName = AzureTestUtils.DEFAULT_REGION_NAME;
        this.defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        this.operation = Mockito.spy(new AzureVirtualMachineOperationSDK(this.regionName,
                this.defaultResourceGroupName));

        makeTheObservablesSynchronous();
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
    }

    // test case: When calling the doGetInstance method with methods mocked,
    // it must verify if It returns the right AzureGetVirtualMachineRef.
    @Test
    public void testDoGetInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        Mockito.doReturn(resourceId).when(this.operation).buildResourceId(Mockito.eq(azure),
                Mockito.eq(subscriptionId), Mockito.eq(resourceName));

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(virtualMachine);
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(virtualMachineOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(resourceId));

        VirtualMachineSizeTypes virtualMachineSizeTypes = VirtualMachineSizeTypes.BASIC_A0;
        Mockito.when(virtualMachine.size()).thenReturn(virtualMachineSizeTypes);

        String cloudState = "cloudState";
        Mockito.when(virtualMachine.provisioningState()).thenReturn(cloudState);

        VirtualMachineInner virtualMachineInner = Mockito.mock(VirtualMachineInner.class);
        Mockito.when(virtualMachineInner.id()).thenReturn(resourceId);
        Mockito.when(virtualMachine.inner()).thenReturn(virtualMachineInner);

        String primaryPrivateIp = "primaryPrivateIp";
        List<String> ipAddresses = Arrays.asList(primaryPrivateIp);
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Mockito.when(networkInterface.primaryPrivateIP()).thenReturn(primaryPrivateIp);
        Mockito.when(virtualMachine.getPrimaryNetworkInterface()).thenReturn(networkInterface);

        String orderName = AzureTestUtils.ORDER_NAME;
        Map<String, String> tags = Collections.singletonMap(AzureConstants.TAG_NAME, orderName);
        Mockito.when(virtualMachine.tags()).thenReturn(tags);

        VirtualMachineSize virtualMachineSize = Mockito.mock(VirtualMachineSize.class);
        Mockito.doReturn(virtualMachineSize).when(this.operation).findVirtualMachineSize(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(Azure.class));

        int vCPU = 2;
        Mockito.when(virtualMachineSize.numberOfCores()).thenReturn(vCPU);

        int memory = 1;
        Mockito.when(virtualMachineSize.memoryInMB()).thenReturn(memory);

        int diskSize = 3;
        Mockito.when(virtualMachine.osDiskSize()).thenReturn(diskSize);

        AzureGetVirtualMachineRef expected = AzureGetVirtualMachineRef.builder()
                .id(resourceId )
                .cloudState(cloudState)
                .vCPU(vCPU)
                .memory(memory)
                .disk(diskSize)
                .ipAddresses(ipAddresses)
                .tags(tags)
                .build();

        // exercise
        AzureGetVirtualMachineRef virtualMachineRef = this.operation.doGetInstance(this.azureUser, resourceName );

        // verify
        Assert.assertEquals(expected, virtualMachineRef);
    }

    // test case: When calling the doGetInstance method and an error occurs,
    // it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testDoGetInstanceFail() throws Exception {
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        Mockito.doReturn(resourceId).when(this.operation).buildResourceId(Mockito.eq(azure),
                Mockito.eq(subscriptionId), Mockito.eq(resourceName));

        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(virtualMachineOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(resourceId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.operation.doGetInstance(this.azureUser, resourceName);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the findVirtualMachineSize method and find one the virtual machine size,
    // it must verify if It returns the right virtual machine size.
    @Test
    public void testFindVirtualMachineSizeSuccessfully() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineSizeExpected = "virtualMachineSizeName";

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
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // exercise
        VirtualMachineSize virtualMachineSize = this.operation
                .findVirtualMachineSize(virtualMachineSizeExpected, regionName, azure);

        // verify
        Assert.assertEquals(virtualMachineSizeMatch.name(), virtualMachineSize.name());
    }

    // test case: When calling the findVirtualMachineSize method and does not find the virtual machine size,
    // it must verify if It throws a NoAvailableResourcesException exception.
    @Test
    public void testFindVirtualMachineSizeFail() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineSizeExpected = "virtualMachineSizeName";

        Region region = Region.US_EAST;
        String regionName = region.name();
        PagedList<VirtualMachineSize> virtualMachines = getVirtualMachineSizesMock();

        VirtualMachineSize virtualMachineSizeNotMactchOne = buildVirtualMachineSizeMock("notmatch");
        VirtualMachineSize virtualMachineSizeNotMactchTwo = buildVirtualMachineSizeMock("notmatch");

        virtualMachines.add(virtualMachineSizeNotMactchOne);
        virtualMachines.add(virtualMachineSizeNotMactchTwo);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.operation.findVirtualMachineSize(virtualMachineSizeExpected, regionName, azure);
    }

    // test case: When calling the findVirtualMachineSize method with two virtual machines size name that
    // fits in the requirements, it must verify if It returns the smaller virtual machines size.
    @Test
    public void testFindVirtualMachineSizeWithTwoVirtualMachinesSize() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

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
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // exercise
        VirtualMachineSize virtualMachineSize = this.operation
                .findVirtualMachineSize(memory, vcpu, regionName, this.azureUser);

        // verify
        Assert.assertNotEquals(virtualMachineSizeFitsBigger, virtualMachineSize);
        Assert.assertEquals(virtualMachineSizeFitsSmaller, virtualMachineSize);
    }

    // test case: When calling the findVirtualMachineSize method with any virtual machine size name that
    // fits in the requirements, it must verify if It throws a NoAvailableResourcesException.
    @Test
    public void testFindVirtualMachineSizeFailWhenSizeThatNotFits() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

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
        PowerMockito.when(AzureVirtualMachineSDK.getVirtualMachineSizes(Mockito.eq(azure), Mockito.eq(region)))
                .thenReturn(virtualMachines);

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.operation.findVirtualMachineSize(memory, vcpu, regionName, this.azureUser);
    }

    // test case: When calling the findVirtualMachineSizeName method with throws an Unauthorized
    // exception, it must verify if It throws an Unauthorized exception.
    @Test
    public void testFindVirtualMachineSizeFailWhenThrowUnauthorized() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenThrow(new UnauthenticatedUserException());

        int memory = 1;
        int vcpu = 2;
        String regionName = Region.US_EAST.name();

        // verify
        this.expectedException.expect(UnauthenticatedUserException.class);

        // exercise
        this.operation.findVirtualMachineSize(memory, vcpu, regionName, this.azureUser);
    }

    // test case: When calling the buildResourceId method, it must verify that
    // the resource ID was assembled correctly.
    @Test
    public void testBuildResourceIdSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(resourceName), Mockito.eq(resourceGroupName));

        String expected = createResourceId();

        // exercise
        String resourceId = this.operation.buildResourceId(azure, subscriptionId, resourceName);

        // verify
        Assert.assertEquals(expected, resourceId);
    }

    // test case: When calling the subscribeCreateVirtualMachine method and the observable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeCreateVirtualMachineSuccessfully() {
        // set up
        Observable<Indexable> virtualMachineObservable = AzureTestUtils.createSimpleObservableSuccess();
        Runnable doOnComplete = Mockito.mock(Runnable.class);

        // exercise
        this.operation.subscribeCreateVirtualMachine(virtualMachineObservable, doOnComplete);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_VM_ASYNC_BEHAVIOUR);
        Mockito.verify(doOnComplete, Mockito.times(TestUtils.RUN_ONCE)).run();
    }

    // test case: When calling the buildNetworkId method, it must verify that
    // the resource ID was assembled correctly.
    @Test
    public void testBuildNetworkIdSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(resourceName), Mockito.eq(resourceGroupName));

        String expected = createNetworkId();

        // exercise
        String resourceId = this.operation.buildNetworkId(azure, subscriptionId, resourceName);

        // verify
        Assert.assertEquals(expected, resourceId);
    }

    // test case: When calling the subscribeCreateVirtualMachine method and the observable executes
    // with an error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeCreateVirtualMachineFail() {
        // set up
        Observable<Indexable> virtualMachineObservable = AzureTestUtils.createSimpleObservableFail();
        Runnable doOnComplete = Mockito.mock(Runnable.class);

        // exercise
        this.operation.subscribeCreateVirtualMachine(virtualMachineObservable, doOnComplete);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_CREATE_VM_ASYNC_BEHAVIOUR);
        Mockito.verify(doOnComplete, Mockito.times(TestUtils.RUN_ONCE)).run();
    }

    // test case: When calling the doCreateInstance method, it must verify if It finishes without error.
    @Test
    public void testDoCreateInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        String virtualNetworkName = AzureTestUtils.RESOURCE_NAME;
        AzureCreateVirtualMachineRef virtualMachineRef = Mockito.mock(AzureCreateVirtualMachineRef.class);
        Mockito.when(virtualMachineRef.getVirtualNetworkName()).thenReturn(virtualNetworkName);

        String subscriptionId = this.azureUser.getSubscriptionId();
        String networkId = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(virtualNetworkName)
                .build();

        Mockito.doReturn(networkId).when(this.operation).buildNetworkId(Mockito.eq(azure),
                Mockito.eq(subscriptionId), Mockito.eq(virtualNetworkName));

        Observable<Indexable> virtualMachineObservable = Mockito.mock(Observable.class);
        Mockito.doReturn(virtualMachineObservable).when(this.operation).buildAzureVirtualMachineObservable(
                Mockito.eq(azure), Mockito.eq(virtualMachineRef), Mockito.eq(networkId));

        Runnable doOnComplete = Mockito.mock(Runnable.class);
        Mockito.doNothing().when(this.operation).subscribeCreateVirtualMachine(Mockito.eq(virtualMachineObservable),
                Mockito.eq(doOnComplete));

        // exercise
        this.operation.doCreateInstance(virtualMachineRef, doOnComplete, this.azureUser);
        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeCreateVirtualMachine(Mockito.eq(virtualMachineObservable), Mockito.eq(doOnComplete));
    }

    // test case: When calling the buildAzureVirtualMachineObservable method and an
    // error occurs, it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testBuildAzureVirtualMachineObservableFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        AzureCreateVirtualMachineRef virtualMachineRef = Mockito.mock(AzureCreateVirtualMachineRef.class);

        String virtualNetworkName = AzureTestUtils.RESOURCE_NAME;
        String subscriptionId = this.azureUser.getSubscriptionId();
        String networkId = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(virtualNetworkName)
                .build();

        Optional<Network> networkOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.doReturn(networkOptional).when(AzureNetworkSDK.class, "getNetwork",
                Mockito.any(Azure.class), Mockito.eq(networkId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.operation.buildAzureVirtualMachineObservable(azure, virtualMachineRef, networkId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the buildAzureVirtualMachineObservable method, it must verify if
    // It calls the method with the right parameters.
    @Test
    public void testBuildAzureVirtualMachineObservableSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String imagePublishedExpected = "publisher";
        String imageSkuExpected = "sku";
        String imageOfferExpected = "offer";
        AzureGetImageRef imageExpected = new AzureGetImageRef(imagePublishedExpected, imageOfferExpected, imageSkuExpected);
        String resourceNameExpected = "resourceNameExpected";
        String virtualNetworkNameExpected = "virtualNetworkNameExpected";
        int diskSize = 1;
        String virtualMachineSizeNameExpected = "virtualMachineSizeNameExpected";
        String osComputeNameExpected = "osComputeNameExpected";
        String osUserNameExpected = "osUserNameExpected";
        String osUserPasswordExpected = "osUserPasswordExpected";
        String regionNameExpected = "regionNameExpected";
        String resourceGroupNameExpected = this.defaultResourceGroupName;
        String subnetNameExpected = "subnetNameExpected";
        String userDataExpected = "userDataExpected";
        Map expectedTags = Collections.singletonMap(AzureConstants.TAG_NAME, "virtualMachineNameExpected");

        AzureCreateVirtualMachineRef virtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .resourceName(resourceNameExpected)
                .azureGetImageRef(imageExpected)
                .virtualNetworkName(virtualNetworkNameExpected)
                .diskSize(diskSize)
                .size(virtualMachineSizeNameExpected)
                .osComputeName(osComputeNameExpected)
                .osUserName(osUserNameExpected)
                .osUserPassword(osUserPasswordExpected)
                .regionName(regionNameExpected)
                .userData(userDataExpected)
                .tags(expectedTags)
                .checkAndBuild();

        Network networkExpected = Mockito.mock(Network.class);
        Subnet subnet = Mockito.mock(Subnet.class);
        Mockito.when(subnet.name()).thenReturn(subnetNameExpected);
        Map<String, Subnet> subnetMap = Collections.singletonMap(subnetNameExpected, subnet);
        Mockito.when(networkExpected.subnets()).thenReturn(subnetMap);

        Optional<Network> optional = Optional.ofNullable(networkExpected);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.doReturn(optional).when(AzureNetworkSDK.class, "getNetwork", Mockito.eq(azure), Mockito.anyString());

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        Region regionExpected = Region.fromName(regionNameExpected);

        // exercise
        this.operation.buildAzureVirtualMachineObservable(azure, virtualMachineRef, virtualNetworkNameExpected);

        // verify
        PowerMockito.verifyStatic(AzureVirtualMachineSDK.class, VerificationModeFactory.times(1));
        AzureVirtualMachineSDK.buildVirtualMachineObservable(Mockito.eq(azure),
                Mockito.eq(resourceNameExpected), Mockito.eq(regionExpected),
                Mockito.eq(resourceGroupNameExpected), Mockito.eq(networkExpected), Mockito.eq(subnetNameExpected),
                Mockito.eq(imagePublishedExpected), Mockito.eq(imageOfferExpected), Mockito.eq(imageSkuExpected),
                Mockito.eq(osUserNameExpected), Mockito.eq(osUserPasswordExpected), Mockito.eq(osComputeNameExpected),
                Mockito.eq(userDataExpected), Mockito.eq(diskSize), Mockito.eq(virtualMachineSizeNameExpected),
                Mockito.eq(expectedTags));
    }

    // test case: When calling the doDeleteInstance method whose existing
    // resource group has the same name as the resource, it must verify among
    // others that the doDeleteResourceGroupAsync method has been called.
    @Test
    public void testDoDeleteInstanceThanExistsResourceGroupWithSameResourceName() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.operation).doDeleteResourceGroupAsync(Mockito.eq(azure),
                Mockito.eq(resourceName));

        // exercise
        this.operation.doDeleteInstance(this.azureUser, resourceName);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteResourceGroupAsync(Mockito.eq(azure), Mockito.eq(resourceName));
    }

    // test case: When calling the doDeleteInstance method whose existing
    // resource group has not the same name as the resource, it must verify
    // among others that the doDeleteVirtualMachineAndResourcesAsync method has
    // been called.
    @Test
    public void testDoDeleteInstanceThanNonExistsResourceGroupWithSameResourceName() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        String resourceId = createResourceId();
        Mockito.doReturn(resourceId).when(this.operation).buildResourceId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.operation).doDeleteVirtualMachineAndResourcesAsync(Mockito.eq(azure),
                Mockito.eq(resourceId));

        // exercise
        this.operation.doDeleteInstance(this.azureUser, resourceName);

        // verify
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(resourceName));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteVirtualMachineAndResourcesAsync(Mockito.eq(azure), Mockito.eq(resourceId));
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method
    // and the observables execute without any error, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteVirtualMachineAndResourcesAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(virtualMachine);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(virtualMachineOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(resourceId));

        Completable deleteVirtualMachine = AzureTestUtils.createSimpleCompletableSuccess();
        Mockito.doReturn(deleteVirtualMachine).when(this.operation)
                .buildDeleteVirtualMachineCompletable(Mockito.eq(azure), Mockito.eq(resourceId));

        Completable deleteVirtualMachineDisk = AzureTestUtils.createSimpleCompletableSuccess();
        Mockito.doReturn(deleteVirtualMachineDisk).when(this.operation)
                .buildDeleteDiskCompletable(Mockito.eq(azure), Mockito.eq(virtualMachine));

        Completable deleteVirtualMachineNic = AzureTestUtils.createSimpleCompletableSuccess();
        Mockito.doReturn(deleteVirtualMachineNic).when(this.operation)
                .buildDeleteNicCompletable(Mockito.eq(azure), Mockito.eq(virtualMachine));

        // exercise
        this.operation.doDeleteVirtualMachineAndResourcesAsync(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzureVirtualMachineSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVirtualMachineSDK.getVirtualMachine(Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .buildDeleteVirtualMachineCompletable(Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .buildDeleteDiskCompletable(Mockito.eq(azure), Mockito.eq(virtualMachine));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .buildDeleteNicCompletable(Mockito.eq(azure), Mockito.eq(virtualMachine));
    }

    // test case: When calling the doDeleteVirtualMachineAndResourcesAsync
    // method with an invalid resource ID, it must verify if an
    // InstanceNotFoundException has been thrown.
    @Test
    public void testDoDeleteVirtualMachineAndResourcesAsyncFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();

        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(virtualMachineOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(resourceId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.operation.doDeleteVirtualMachineAndResourcesAsync(azure, resourceId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doDeleteResourceGroupAsync method, it must
    // verify that is call was successful.
    @Test
    public void testDoDeleteResourceGroupAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(completable).when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.operation).subscribeDeleteVirtualMachine(Mockito.eq(completable));

        // exercise
        this.operation.doDeleteResourceGroupAsync(azure, resourceName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDeleteVirtualMachine(Mockito.eq(completable));
    }

    // test case: When calling the subscribeDeleteVirtualMachine method and the
    // completable executes without any error, it must verify than returns the
    // right logs.
    @Test
    public void testsubscribeDeleteVirtualMachineSuccessfully() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();

        // exercise
        this.operation.subscribeDeleteVirtualMachine(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeleteVirtualMachine method and the
    // completable executes with an error, it must verify if It returns the
    // right logs.
    @Test
    public void testsubscribeDeleteVirtualMachineFail() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableFail();

        // exercise
        this.operation.subscribeDeleteVirtualMachine(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteDiskCompletable method and the
    // completable executes without any error, it must verify than it returns the
    // right logs.
    @Test
    public void testBuildDeleteDiskCompletableSuccessfully() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String osDiskId = "osDiskId";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.osDiskId()).thenReturn(osDiskId);

        String expectedMessage = "completableMessage";
        Completable deleteDiskCompletableSuccess = createSimpleCompletableSuccess(expectedMessage);

        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.when(AzureVolumeSDK.buildDeleteDiskCompletable(Mockito.eq(azure), Mockito.eq(osDiskId)))
                .thenReturn(deleteDiskCompletableSuccess);

        // exercise
        Completable completable = this.operation.buildDeleteDiskCompletable(azure, virtualMachine);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteDiskCompletable method and the
    // completable executes with error, it must verify than it returns the right
    // logs.
    @Test
    public void testBuildDeleteDiskCompletableFail() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String osDiskId = "osDiskId";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.osDiskId()).thenReturn(osDiskId);

        String expectedMessage = "completableFail";
        Completable deleteDiskCompletableFail = createSimpleCompletableFail(expectedMessage);

        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.when(AzureVolumeSDK.buildDeleteDiskCompletable(Mockito.eq(azure), Mockito.eq(osDiskId)))
                .thenReturn(deleteDiskCompletableFail);

        // exercise
        Completable completable = this.operation.buildDeleteDiskCompletable(azure, virtualMachine);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteNicCompletable method and the
    // completable executes without any error, it must verify than it returns the
    // right logs.
    @Test
    public void testBuildDeleteNicCompletableSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String nicId = "nicId";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.primaryNetworkInterfaceId()).thenReturn(nicId);

        String expectedMessage = "completableSuccess";
        Completable deleteNicCompletableSuccess = createSimpleCompletableSuccess(expectedMessage);

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.buildDeleteNetworkInterfaceCompletable(Mockito.eq(azure), Mockito.eq(nicId)))
                .thenReturn(deleteNicCompletableSuccess);

        // exercise
        Completable completable = this.operation.buildDeleteNicCompletable(azure, virtualMachine);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_NIC_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteNicCompletable method and the
    // completable executes with error, it must verify than it returns the right
    // logs.
    @Test
    public void testBuildDeleteNicCompletableFail() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String nicId = "nicId";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.primaryNetworkInterfaceId()).thenReturn(nicId);

        String expectedMessage = "completableFail";
        Completable deleteNicCompletableFail = createSimpleCompletableFail(expectedMessage);

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.buildDeleteNetworkInterfaceCompletable(Mockito.eq(azure), Mockito.eq(nicId)))
                .thenReturn(deleteNicCompletableFail);

        // exercise
        Completable completable = this.operation.buildDeleteNicCompletable(azure, virtualMachine);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_NIC_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualMachineCompletable method and
    // the completable executes without any error, it must verify than it returns
    // the right logs.
    @Test
    public void testBuildDeleteVirtualMachineCompletableSuccessfully() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = "instanceId";
        String expectedMessage = "completableSuccess";
        Completable virtualMachineCompletableSuccess = createSimpleCompletableSuccess(expectedMessage);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK.buildDeleteVirtualMachineCompletable(Mockito.eq(azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineCompletableSuccess);

        // exercise
        Completable completable = this.operation.buildDeleteVirtualMachineCompletable(azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualMachineCompletable method and
    // the completable executes with error, it must verify than it returns the right
    // logs.
    @Test
    public void testBuildDeleteVirtualMachineCompletableFail() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = "instanceId";
        String expectedMessage = "completableFail";
        Completable virtualMachineCompletableFail = createSimpleCompletableFail(expectedMessage);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.when(AzureVirtualMachineSDK .buildDeleteVirtualMachineCompletable(Mockito.eq(azure), Mockito.eq(instanceId)))
                .thenReturn(virtualMachineCompletableFail);

        // exercise
        Completable completable = this.operation.buildDeleteVirtualMachineCompletable(azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.DEBUG, expectedMessage)
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

    private void makeTheObservablesSynchronous() {
        // The scheduler trampolime makes the subscriptions execute in the current thread
        this.operation.setScheduler(Schedulers.trampoline());
    }

    private Completable createSimpleCompletableSuccess(String message) {
        return Completable.create((completableSubscriber) -> {
            LOGGER_CLASS_MOCK.debug(message);
            completableSubscriber.onCompleted();
        });
    }

    private Completable createSimpleCompletableFail(String message) {
        return Completable.create((completableSubscriber) -> {
            LOGGER_CLASS_MOCK.debug(message);
            completableSubscriber.onError(new RuntimeException());
        });
    }

    private String createResourceId() {
        String virtualMachineIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";
        return String.format(virtualMachineIdFormat,
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }

    private String createNetworkId() {
        String networkIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networks/%s";
        return String.format(networkIdFormat,
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
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

}
