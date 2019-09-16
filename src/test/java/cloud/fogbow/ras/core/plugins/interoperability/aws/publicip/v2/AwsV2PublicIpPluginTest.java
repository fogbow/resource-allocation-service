package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.Tag;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2PublicIpPluginTest extends BaseUnitTests {

	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_ALLOCATION_ID = "fake-allocation-id";
	private static final String FAKE_ASSOCIATION_ID = "fake-association-id";
	private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
	private static final String FAKE_GROUP_ID = "fake-group-id";
	private static final String FAKE_NETWORK_INTERFACE_ID = "fake-network-interface-id";
	
	private AwsV2PublicIpPlugin plugin;
	private Ec2Client client;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        String awsConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + CLOUD_NAME 
                + File.separator 
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2PublicIpPlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }
	
    // test case: When calling the requestInstance method, with a public IP order
    // and cloud user valid, a client is invoked to create a public IP instance,
    // returning the public IP ID.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        PublicIpOrder order = createPublicIpOrder();
        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin)
                .doRequestInstance(Mockito.eq(order.getComputeId()), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.eq(order.getComputeId()), Mockito.eq(this.client));
    }
    
    // test case: When calling the deleteInstance method, with a public IP order and
    // cloud user valid, the elastic IP in the AWS cloud must be released.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        PublicIpOrder order = createPublicIpOrder();

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));
    }
    
    // test case: When calling the getInstance method, with a public IP order and
    // cloud user valid, a client is invoked to request an address in the cloud, and
    // mount a public IP instance.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        PublicIpOrder order = createPublicIpOrder();
        PublicIpInstance instance = createPublicIpInstance();
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the isReady method with the cloud state AVAILABLE,
    // this means that the state of public IP is READY and it must return true.
    @Test
    public void testIsReady() {
        // set up
        String cloudState = AwsV2StateMapper.AVAILABLE_STATE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertTrue(status);
    }
	
    // test case: When calling the isReady method with the cloud states different
    // than AVAILABLE, this means that the state of public IP is not READY and it
    // must return false.
    @Test
    public void testIsNotReady() {
        // set up
        String cloudState = AwsV2StateMapper.ERROR_STATE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertFalse(status);
    }
	
    // test case: When calling the hasFailed method with the cloud state ERROR,
    // this means that the state of public IP failed and it must return true.
    @Test
    public void testHasFailed() {
        // set up
        String cloudState = AwsV2StateMapper.ERROR_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(cloudState);

        // verify
        Assert.assertTrue(status);
    }
	
    // test case: When calling the hasFailed method with the cloud states different
    // than ERROR, this means that the state of public IP failed and it must return
    // true.
    @Test
    public void testHasNotFailed() {
        // set up
        String cloudState = AwsV2StateMapper.AVAILABLE_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(cloudState);

        // verify
        Assert.assertFalse(status);
    }
    
    // Test case: When calling the doDeleteInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteInstance() throws Exception {
        // setup
        String allocationId = FAKE_ALLOCATION_ID;
        Address address = buildAddress();
        Mockito.doReturn(address).when(this.plugin).getAddressById(Mockito.anyString(), Mockito.eq(this.client));

        String associationId = FAKE_ASSOCIATION_ID;
//        Mockito.doReturn(associationId).when(this.plugin).getResourceIdByAddressTag(
//                Mockito.eq(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID), Mockito.eq(allocationId),
//                Mockito.eq(this.client));

        String groupId = FAKE_GROUP_ID;
//        Mockito.doReturn(groupId).when(this.plugin).getResourceIdByAddressTag(
//                Mockito.eq(AwsV2CloudUtil.AWS_TAG_GROUP_ID), Mockito.eq(allocationId), Mockito.eq(this.client));

        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;
        Mockito.doNothing().when(plugin).doModifyNetworkInterfaceAttributes(Mockito.eq(allocationId),
                Mockito.eq(groupId), Mockito.eq(networkInterfaceId), Mockito.eq(this.client));

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, "doDeleteSecurityGroup", Mockito.eq(groupId),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doDisassociateAddresses(Mockito.eq(associationId),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doReleaseAddresses(Mockito.eq(allocationId), Mockito.eq(this.client));

        // exercise
        plugin.doDeleteInstance(allocationId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_THREE_TIMES)).getAddressById(Mockito.anyString(),
                Mockito.eq(this.client));
//        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getResourceIdByAddressTag(
//                Mockito.eq(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID), Mockito.eq(allocationId),
//                Mockito.eq(this.client));
//        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getResourceIdByAddressTag(
//                Mockito.eq(AwsV2CloudUtil.AWS_TAG_GROUP_ID), Mockito.eq(allocationId), Mockito.eq(this.client));
//        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doModifyNetworkInterfaceAttributes(
//                Mockito.eq(allocationId), Mockito.eq(groupId), Mockito.eq(networkInterfaceId), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(groupId), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDisassociateAddresses(Mockito.eq(associationId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doReleaseAddresses(Mockito.eq(allocationId),
                Mockito.eq(this.client));
    }
    
    private Address buildAddress() {
        Tag tagAssociationId = buildTag(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID, FAKE_ASSOCIATION_ID);
        Tag tagGroupId = buildTag(AwsV2CloudUtil.AWS_TAG_GROUP_ID, FAKE_GROUP_ID);

        Address address = Address.builder()
                .allocationId(FAKE_ALLOCATION_ID)
                .instanceId(TestUtils.FAKE_INSTANCE_ID)
                .tags(tagAssociationId, tagGroupId)
                .build();

        return address;
    }
    
    private Tag buildTag(String key, String value) {
        Tag tag = Tag.builder()
                .key(key)
                .value(value)
                .build();
        
        return tag;
    }
	
    private PublicIpInstance createPublicIpInstance() {
        String id = FAKE_ALLOCATION_ID;
        String cloudState = AwsV2StateMapper.AVAILABLE_STATE;
        String ip = FAKE_CIDR_ADDRESS;
        return new PublicIpInstance(id, cloudState, ip);
    }
	
    private PublicIpOrder createPublicIpOrder() {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        PublicIpOrder publicIpOrder = this.testUtils.createLocalPublicIpOrder(computeOrder.getInstanceId());
        return publicIpOrder;
    }
	
}
