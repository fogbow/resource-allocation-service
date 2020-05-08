package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.network.implementation.PublicIPAddressInner;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIPAddressOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIPAddressSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import rx.Completable;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ 
    Azure.class,
    AzureClientCacheManager.class,
    AzureGeneralUtil.class,
    AzurePublicIPAddressSDK.class,
    AzureResourceGroupOperationUtil.class,
    AzureVirtualMachineSDK.class
})
public class AzurePublicIpPluginTest {

    private String defaultRegionName;
    private String defaultResourceGroupName;
    private AzurePublicIPAddressOperationSDK operation;
    private AzurePublicIpPlugin plugin;
    private AzureUser azureUser;

    @Before
    public void setUp() throws Exception {
        String azureConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = Mockito.spy(new AzurePublicIPAddressOperationSDK(this.defaultResourceGroupName));
        this.plugin = Mockito.spy(new AzurePublicIpPlugin(azureConfFilePath));
        this.plugin.setOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the isReady method and the instance state is
    // succeeded, it must verify than returns true value.
    @Test
    public void testIsReadyWhenInstanceStateIsSucceeded() {
        // set up
        String instanceState = AzureStateMapper.SUCCEEDED_STATE;

        // exercise
        boolean status = this.plugin.isReady(instanceState);

        // verify
        Assert.assertTrue(status);
    }

    // test case: When calling the isReady method and the instance state is not
    // succeeded, it must verify than returns false value.
    @Test
    public void testIsReadyWhenInstanceStateIsNotSucceeded() {
        // set up
        String[] instanceStates = { AzureStateMapper.CREATING_STATE, AzureStateMapper.FAILED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.isReady(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is
    // failed, it must verify than returns true value.
    @Test
    public void testHasFailedWhenInstanceStateIsFailed() {
        // set up
        String instanceState = AzureStateMapper.FAILED_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(instanceState);

        // exercise and verify
        Assert.assertTrue(status);
    }

    // test case: When calling the hasFailed method and the instance state is not
    // failed, it must verify than returns false value.
    @Test
    public void testHasFailedWhenInstanceStateIsNotFailed() {
        // set up
        String[] instanceStates = { AzureStateMapper.CREATING_STATE, AzureStateMapper.SUCCEEDED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.hasFailed(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When calling the requestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testRequestInstanceSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PublicIpOrder publicIpOrder = mockPublicIpOrder(resourceName);

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "generateResourceName");

        PowerMockito.doReturn(this.defaultResourceGroupName).when(AzureGeneralUtil.class, "defineResourceGroupName",
                Mockito.eq(azure), Mockito.eq(this.defaultRegionName), Mockito.eq(resourceName),
                Mockito.eq(this.defaultResourceGroupName));

        String virtualMachineId = createVirtualMachineId();
        Mockito.doReturn(virtualMachineId).when(this.plugin).buildVirtualMachineId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        Mockito.when(azure.publicIPAddresses()).thenReturn(publicIPAddresses);

        PublicIPAddress.DefinitionStages.Blank define = Mockito.mock(PublicIPAddress.DefinitionStages.Blank.class);
        Mockito.when(publicIPAddresses.define(Mockito.anyString())).thenReturn(define);

        PublicIPAddress.DefinitionStages.WithGroup withGroup = Mockito
                .mock(PublicIPAddress.DefinitionStages.WithGroup.class);

        Mockito.when(define.withRegion(Mockito.eq(this.defaultRegionName))).thenReturn(withGroup);

        PublicIPAddress.DefinitionStages.WithCreate withCreate = Mockito
                .mock(PublicIPAddress.DefinitionStages.WithCreate.class);

        Mockito.when(withGroup.withExistingResourceGroup(Mockito.eq(this.defaultResourceGroupName)))
                .thenReturn(withCreate);

        Mockito.when(withCreate.withDynamicIP()).thenReturn(withCreate);

        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        Mockito.doReturn(instanceId).when(this.plugin).doRequestInstance(Mockito.eq(azure), Mockito.anyString(),
                Mockito.any(Creatable.class));

        // exercise
        this.plugin.requestInstance(publicIpOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.generateResourceName();

        Mockito.verify(publicIpOrder, Mockito.times(TestUtils.RUN_ONCE)).getComputeId();
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildVirtualMachineId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(azure),
                Mockito.anyString(), Mockito.any(Creatable.class));
    }

    // test case: When calling the getInstance method, it must verify that is call
    // was successful.
    @Test
    public void testGetInstanceSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PublicIpOrder publicIpOrder = mockPublicIpOrder(resourceName);

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        String resourceId = createResourceId();
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(azure), Mockito.eq(subscriptionId),
                Mockito.eq(resourceName));

        PublicIpInstance publicIpInstance = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstance).when(this.plugin).doGetInstance(Mockito.eq(azure), Mockito.eq(resourceId));

        // exercise
        this.plugin.getInstance(publicIpOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(azure),
                Mockito.eq(subscriptionId), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(azure),
                Mockito.eq(resourceId));
    }

    // test case: When calling the deleteInstance method, with the default
    // resource group, it must verify among others if the doDeleteInstance
    // method has been called.
    @Test
    public void testDeleteInstanceWithDefaultResourceGroup() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PublicIpOrder publicIpOrder = mockPublicIpOrder(resourceName);

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        String resourceId = createResourceId();
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(azure), Mockito.anyString(),
                Mockito.eq(resourceName));

        String virtualMachineId = createVirtualMachineId();
        Mockito.doReturn(virtualMachineId).when(this.plugin).buildVirtualMachineId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(azure), Mockito.eq(resourceId),
                Mockito.eq(virtualMachineId));

        // exercise
        this.plugin.deleteInstance(publicIpOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildVirtualMachineId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(azure),
                Mockito.eq(resourceId), Mockito.eq(virtualMachineId));
    }

    // test case: When calling the deleteInstance method, without the default
    // resource group, it must verify among others if the doDeleteResourceGroup
    // method has been called.
    @Test
    public void testDeleteInstanceWithoutDefaultResourceGroup() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PublicIpOrder publicIpOrder = mockPublicIpOrder(resourceName);

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.plugin).doDeleteResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName),
                Mockito.anyString());

        // exercise
        this.plugin.deleteInstance(publicIpOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteResourceGroup(Mockito.eq(azure),
                Mockito.eq(resourceName), Mockito.anyString());
    }

    // test case: When calling the doDeleteResourceGroup method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteResourceGroupSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String virtualMachineId = createVirtualMachineId();

        Observable<NetworkInterface> observable = Observable.defer(() -> {
            NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
            return Observable.just(networkInterface);
        });
        Mockito.doReturn(observable).when(this.plugin).disassociateResourcesAsync(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(completable).when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.operation).subscribeDisassociateAndDeleteResources(Mockito.eq(observable),
                Mockito.eq(completable));

        // exercise
        this.plugin.doDeleteResourceGroup(azure, resourceName, virtualMachineId);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .disassociateResourcesAsync(Mockito.eq(azure), Mockito.eq(virtualMachineId));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDisassociateAndDeleteResources(Mockito.eq(observable), Mockito.eq(completable));
    }

    // test case: When calling the doDeleteInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();
        String virtualMachineId = createVirtualMachineId();
        String networkSecurityGroupId = createNetworkSecurityGroupId();

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(virtualMachine).when(this.plugin).doGetVirtualMachineSDK(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Mockito.doReturn(networkInterface).when(this.plugin)
                .doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkSecurityGroup.id()).thenReturn(networkSecurityGroupId);

        Mockito.doReturn(networkSecurityGroup).when(this.plugin)
                .doGetNetworkSecurityGroupFrom(Mockito.eq(networkInterface));

        Observable observable = Mockito.mock(Observable.class);
        Mockito.doReturn(observable).when(this.plugin).doDisassociateResourcesFrom(Mockito.eq(networkInterface));

        Completable completable = Mockito.mock(Completable.class);
        Mockito.doReturn(completable).when(this.plugin).doDeleteResourcesAsync(Mockito.eq(azure),
                Mockito.eq(resourceId), Mockito.eq(networkSecurityGroupId));

        Mockito.doNothing().when(this.operation).subscribeDisassociateAndDeleteResources(Mockito.eq(observable),
                Mockito.eq(completable));

        // exercise
        this.plugin.doDeleteInstance(azure, resourceId, virtualMachineId);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetVirtualMachineSDK(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetNetworkSecurityGroupFrom(Mockito.eq(networkInterface));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDisassociateResourcesFrom(Mockito.eq(networkInterface));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteResourcesAsync(Mockito.eq(azure),
                Mockito.eq(resourceId), Mockito.eq(networkSecurityGroupId));
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDisassociateAndDeleteResources(Mockito.eq(observable), Mockito.eq(completable));
    }

    // test case: When calling the doDeleteResourcesAsync method, it must
    // verify that is call was successful.
    @Test
    public void testDoDeleteResourcesAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String networkSecurityGroupId = createNetworkSecurityGroupId();
        String resourceId = createResourceId();

        Completable deleteSecurityGroup = Mockito.mock(Completable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(deleteSecurityGroup).when(AzurePublicIPAddressSDK.class,
                "deleteNetworkSecurityGroupAsync", Mockito.eq(azure), Mockito.eq(networkSecurityGroupId));

        Completable deletePublicIp = Mockito.mock(Completable.class);
        PowerMockito.doReturn(deletePublicIp).when(AzurePublicIPAddressSDK.class,
                "deletePublicIpAddressAsync", Mockito.eq(azure), Mockito.eq(resourceId));

        // exercise
        this.plugin.doDeleteResourcesAsync(azure, resourceId, networkSecurityGroupId);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.deleteNetworkSecurityGroupAsync(Mockito.eq(azure), Mockito.eq(networkSecurityGroupId));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.deletePublicIpAddressAsync(Mockito.eq(azure), Mockito.eq(resourceId));
    }

    // test case: When calling the disassociateResourcesAsync method, it must
    // verify that is call was successful.
    @Test
    public void testDisassociateResourcesAsyncSuccessfully() throws FogbowException {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineId = createVirtualMachineId();

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(virtualMachine).when(this.plugin).doGetVirtualMachineSDK(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Mockito.doReturn(networkInterface).when(this.plugin)
                .doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));

        Observable<NetworkInterface> observable = Observable.defer(() -> {
            return Observable.just(networkInterface);
        });
        Mockito.doReturn(observable).when(this.plugin).doDisassociateResourcesFrom(Mockito.eq(networkInterface));

        // exercise
        this.plugin.disassociateResourcesAsync(azure, virtualMachineId);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetVirtualMachineSDK(Mockito.eq(azure), Mockito.eq(virtualMachineId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDisassociateResourcesFrom(Mockito.eq(networkInterface));
    }

    // test case: When calling the doDisassociateResourcesFrom method, with a
    // valid network instance, it must verify that is call was successful.
    @Test
    public void testDoDisassociateResourcesFromNetworkInstanceSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        Observable<NetworkInterface> disassociateSecurityGroup = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(disassociateSecurityGroup).when(AzurePublicIPAddressSDK.class,
                "disassociateNetworkSecurityGroupAsync", Mockito.eq(networkInterface));

        Observable<NetworkInterface> disassociatePublicIp = Mockito.mock(Observable.class);
        PowerMockito.doReturn(disassociatePublicIp).when(AzurePublicIPAddressSDK.class,
                "disassociatePublicIPAddressAsync", Mockito.eq(networkInterface));

        // exercise
        this.plugin.doDisassociateResourcesFrom(networkInterface);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.disassociateNetworkSecurityGroupAsync(Mockito.eq(networkInterface));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.disassociatePublicIPAddressAsync(Mockito.eq(networkInterface));
    }

    // test case: When calling the doGetNetworkSecurityGroupFrom method, it must
    // verify that is call was successful.
    @Test
    public void testDoGetNetworkSecurityGroupFromNetworkInterfaceSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);

        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupFrom",
                Mockito.eq(networkInterface));

        // exercise
        this.plugin.doGetNetworkSecurityGroupFrom(networkInterface);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getNetworkSecurityGroupFrom(Mockito.eq(networkInterface));
    }

    // test case: When calling the doGetNetworkSecurityGroupFrom method with an
    // invalid network interface, it must verify if an InstanceNotFoundException has
    // been thrown.
    @Test
    public void testDoGetNetworkSecurityGroupFromNetworkInterfaceFail() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupFrom",
                Mockito.eq(networkInterface));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.doGetNetworkSecurityGroupFrom(networkInterface);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doGetPrimaryNetworkInterfaceFrom method, it must
    // verify that is call was successful.
    @Test
    public void testDoGetPrimaryNetworkInterfaceFromVirtualMachineSuccessfully() throws Exception {
        // set up
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Optional<NetworkInterface> nicOptional = Optional.ofNullable(networkInterface);

        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(nicOptional).when(AzurePublicIPAddressSDK.class, "getPrimaryNetworkInterfaceFrom",
                Mockito.eq(virtualMachine));

        // exercise
        this.plugin.doGetPrimaryNetworkInterfaceFrom(virtualMachine);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));
    }

    // test case: When calling the doGetPrimaryNetworkInterfaceFrom method with an invalid
    // virtual machine, it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testDoGetPrimaryNetworkInterfaceFromVirtualMachineFail() throws Exception {
        // set up
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Optional<NetworkInterface> nicOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(nicOptional).when(AzurePublicIPAddressSDK.class, "getPrimaryNetworkInterfaceFrom",
                Mockito.eq(virtualMachine));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.doGetPrimaryNetworkInterfaceFrom(virtualMachine);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doGetInstance method, it must verify that is call
    // was successful.
    @Test
    public void testDoGetInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.doReturn(publicIPAddress).when(this.plugin).doGetPublicIPAddressSDK(Mockito.eq(azure),
                Mockito.eq(resourceId));

        PublicIpInstance publicIpInstance = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstance).when(this.plugin).buildPublicIpInstance(Mockito.eq(publicIPAddress));

        // exercise
        this.plugin.doGetInstance(azure, resourceId);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetPublicIPAddressSDK(Mockito.eq(azure),
                Mockito.eq(resourceId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildPublicIpInstance(Mockito.eq(publicIPAddress));
    }

    // test case: When calling the buildPublicIpInstance method, it must verify that
    // is call was successful and created the public IP instance.
    @Test
    public void testBuildPublicIpInstanceSuccessfully() throws Exception {
        // set up
        String resourceId = createResourceId();
        String state = AzureStateMapper.SUCCEEDED_STATE;
        String ipAddress = "0.0.0.0";

        PublicIPAddressInner publicIPAddressInner = Mockito.mock(PublicIPAddressInner.class);
        Mockito.when(publicIPAddressInner.id()).thenReturn(resourceId);
        Mockito.when(publicIPAddressInner.provisioningState()).thenReturn(state);
        Mockito.when(publicIPAddressInner.ipAddress()).thenReturn(ipAddress);

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddress.inner()).thenReturn(publicIPAddressInner);

        PublicIpInstance expected = createPublicIpInstance();

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.buildPublicIpInstance(publicIPAddress);

        // verify
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).inner();
        Mockito.verify(publicIPAddressInner, Mockito.times(TestUtils.RUN_ONCE)).id();
        Mockito.verify(publicIPAddressInner, Mockito.times(TestUtils.RUN_ONCE)).provisioningState();
        Mockito.verify(publicIPAddressInner, Mockito.times(TestUtils.RUN_ONCE)).ipAddress();

        Assert.assertEquals(expected, publicIpInstance);
    }

    // test case: When calling the doGetPublicIPAddressSDK method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetPublicIPAddressSDKSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Optional<PublicIPAddress> publicIPAddressOptional = Optional.ofNullable(publicIPAddress);

        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(publicIPAddressOptional).when(AzurePublicIPAddressSDK.class, "getPublicIpAddress",
                Mockito.eq(azure), Mockito.eq(resourceId));

        // exercise
        this.plugin.doGetPublicIPAddressSDK(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getPublicIpAddress(Mockito.eq(azure), Mockito.eq(resourceId));
    }

    // test case: When calling the doGetPublicIPAddressSDK method with an invalid
    // resource ID, it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testDoGetPublicIPAddressSDKFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = TestUtils.ANY_VALUE;

        Optional<PublicIPAddress> publicIPAddressOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(publicIPAddressOptional).when(AzurePublicIPAddressSDK.class, "getPublicIpAddress",
                Mockito.eq(azure), Mockito.eq(resourceId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.doGetPublicIPAddressSDK(azure, resourceId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the buildResourceId method, it must verify that the
    // resource ID was assembled correctly.
    @Test
    public void testBuildResourceIdSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(defaultResourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(resourceName), Mockito.eq(defaultResourceGroupName));

        String expected = createResourceId();

        // exercise
        String resourceId = this.plugin.buildResourceId(azure, subscriptionId, resourceName);

        // verify
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.selectResourceGroupName(Mockito.eq(azure), Mockito.eq(resourceName),
                Mockito.eq(defaultResourceGroupName));

        Assert.assertEquals(expected, resourceId);
    }

    // test case: When calling the doRequestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoRequestInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineId = createVirtualMachineId();

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        Creatable<PublicIPAddress> publicIPAddressCreatable = Mockito.mock(Creatable.class);
        Mockito.when(publicIPAddressCreatable.name()).thenReturn(resourceName);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(virtualMachine).when(this.plugin).doGetVirtualMachineSDK(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Mockito.doReturn(networkInterface).when(this.plugin).doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(observable).when(AzurePublicIPAddressSDK.class, "associatePublicIPAddressAsync",
                Mockito.eq(networkInterface), Mockito.eq(publicIPAddressCreatable));

        Mockito.doNothing().when(this.operation).subscribeAssociatePublicIPAddress(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(observable));

        // exercise
        this.plugin.doRequestInstance(azure, virtualMachineId, publicIPAddressCreatable);

        // verify
        Mockito.verify(this.plugin, Mockito.timeout(TestUtils.RUN_ONCE)).doGetVirtualMachineSDK(Mockito.eq(azure),
                Mockito.eq(virtualMachineId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetPrimaryNetworkInterfaceFrom(Mockito.eq(virtualMachine));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.associatePublicIPAddressAsync(Mockito.eq(networkInterface),
                Mockito.eq(publicIPAddressCreatable));

        Mockito.verify(this.operation, Mockito.timeout(TestUtils.RUN_ONCE))
                .subscribeAssociatePublicIPAddress(Mockito.eq(azure), Mockito.anyString(), Mockito.eq(observable));
    }

    // test case: When calling the doGetVirtualMachineSDK method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetVirtualMachineSDKSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineId = createVirtualMachineId();

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Optional<VirtualMachine> publicIPAddressOptional = Optional.ofNullable(virtualMachine);

        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(publicIPAddressOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(virtualMachineId));

        // exercise
        this.plugin.doGetVirtualMachineSDK(azure, virtualMachineId);

        // verify
        PowerMockito.verifyStatic(AzureVirtualMachineSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVirtualMachineSDK.getVirtualMachine(Mockito.eq(azure), Mockito.eq(virtualMachineId));
    }

    // test case: When calling the doGetVirtualMachineSDK method with an invalid
    // resource ID, it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testDoGetVirtualMachineSDKFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String virtualMachineId = TestUtils.ANY_VALUE;

        Optional<VirtualMachine> virtualMachineOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzureVirtualMachineSDK.class);
        PowerMockito.doReturn(virtualMachineOptional).when(AzureVirtualMachineSDK.class, "getVirtualMachine",
                Mockito.eq(azure), Mockito.eq(virtualMachineId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.doGetVirtualMachineSDK(azure, virtualMachineId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the buildVirtualMachineId method, it must verify that
    // the virtual machine ID was assembled correctly.
    @Test
    public void testBuildVirtualMachineIdSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(defaultResourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(resourceName), Mockito.eq(defaultResourceGroupName));

        String expected = createVirtualMachineId();

        // exercise
        String virtualMachineId = this.plugin.buildVirtualMachineId(azure, subscriptionId, resourceName);

        // verify
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.selectResourceGroupName(Mockito.eq(azure), Mockito.eq(resourceName),
                Mockito.eq(defaultResourceGroupName));

        Assert.assertEquals(expected, virtualMachineId);
    }

    private PublicIpInstance createPublicIpInstance() {
        String resourceId = createResourceId();
        String ip = "0.0.0.0";
        return new PublicIpInstance(resourceId, AzureStateMapper.SUCCEEDED_STATE, ip);
    }

    private String createNetworkSecurityGroupId() {
        String publicIPAddressIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkSecurityGroups/%s";
        return String.format(publicIPAddressIdFormat,
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }

    private String createVirtualMachineId() {
        String virtualMachineIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";
        return String.format(virtualMachineIdFormat, 
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }

    private String createResourceId() {
        String publicIPAddressIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses/%s";
        return String.format(publicIPAddressIdFormat, 
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }

    private PublicIpOrder mockPublicIpOrder(String resourceName) {
        String computeId = "compute-id";
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        Mockito.when(publicIpOrder.getComputeId()).thenReturn(computeId);
        Mockito.when(publicIpOrder.getInstanceId()).thenReturn(instanceId);
        return publicIpOrder;
    }
}
