package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2PublicIpPluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_ALLOCATION_ID = "fake-allocation-id";
    private static final String FAKE_ASSOCIATION_ID = "fake-association-id";
    private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
    private static final String FAKE_DEFAULT_SECURITY_GROUP_ID = "fake-security-group-id";
    private static final String FAKE_DEFAULT_VPC_ID = "fake-vpc-id";
    private static final String FAKE_GROUP_ID = "fake-group-id";
    private static final String FAKE_NETWORK_INTERFACE_ID = "fake-network-interface-id";
    private static final String FAKE_SUBNET_ID = "fake-subnet-id";
	
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
                .doRequestInstance(Mockito.eq(order), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.eq(order), Mockito.eq(this.client));
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
        String defaultGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Address address = buildAddress();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(address).when(AwsV2CloudUtil.class, TestUtils.GET_ADDRESS_BY_ID_METHOD,
                Mockito.eq(allocationId), Mockito.eq(this.client));

        Mockito.doReturn(FAKE_ASSOCIATION_ID).when(this.plugin).getAssociationIdFrom(Mockito.eq(address.tags()));

        PowerMockito.doReturn(FAKE_GROUP_ID).when(AwsV2CloudUtil.class, TestUtils.GET_GROUP_ID_FROM_METHOD,
                Mockito.eq(address.tags()));

        Mockito.doNothing().when(plugin).doModifyNetworkInterfaceAttributes(Mockito.eq(allocationId),
                Mockito.eq(defaultGroupId), Mockito.eq(address.networkInterfaceId()), Mockito.eq(this.client));

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD,
                Mockito.anyString(), Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doDisassociateAddresses(Mockito.anyString(), Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doReleaseAddresses(Mockito.eq(allocationId), Mockito.eq(this.client));

        // exercise
        plugin.doDeleteInstance(allocationId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getAddressById(Mockito.eq(allocationId), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAssociationIdFrom(Mockito.eq(address.tags()));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getGroupIdFrom(Mockito.eq(address.tags()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doModifyNetworkInterfaceAttributes(
                Mockito.eq(allocationId), Mockito.eq(defaultGroupId), Mockito.eq(address.networkInterfaceId()),
                Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.anyString(), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDisassociateAddresses(Mockito.anyString(),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doReleaseAddresses(Mockito.eq(allocationId),
                Mockito.eq(this.client));
    }
    
    // Test case: When calling the doDeleteInstance method, and an unexpected error
    // occurs, it must verify if an UnexpectedException has been thrown and after
    // that, the DisassociateAddresses and doReleaseAddresses methods must also be
    // invoked.
    @Test
    public void testDoDeleteInstanceFail() throws Exception {
        // setup
        String allocationId = FAKE_ALLOCATION_ID;
        String defaultGroupId = FAKE_DEFAULT_SECURITY_GROUP_ID;
        Address address = buildAddress();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(address).when(AwsV2CloudUtil.class, TestUtils.GET_ADDRESS_BY_ID_METHOD,
                Mockito.eq(allocationId), Mockito.eq(this.client));

        Mockito.doReturn(FAKE_ASSOCIATION_ID).when(this.plugin).getAssociationIdFrom(Mockito.eq(address.tags()));

        PowerMockito.doReturn(FAKE_GROUP_ID).when(AwsV2CloudUtil.class, TestUtils.GET_GROUP_ID_FROM_METHOD,
                Mockito.eq(address.tags()));
        
        Mockito.doNothing().when(plugin).doModifyNetworkInterfaceAttributes(Mockito.eq(allocationId),
                Mockito.eq(defaultGroupId), Mockito.eq(address.networkInterfaceId()), Mockito.eq(this.client));

        PowerMockito.doThrow(new UnexpectedException()).when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD, Mockito.anyString(),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doDisassociateAddresses(Mockito.anyString(), Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doReleaseAddresses(Mockito.eq(allocationId), Mockito.eq(this.client));

        try {
            // exercise
            plugin.doDeleteInstance(allocationId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDisassociateAddresses(Mockito.anyString(),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doReleaseAddresses(Mockito.eq(allocationId),
                    Mockito.eq(this.client));
        }
    }
    
    // test case: When calling the getAssociationIdFrom method with a valid tags
    // from a address, it must return a expected association ID.
    @Test
    public void testGetAssociationIdFromTags() throws FogbowException {
        // set up
        List<Tag> tags = buildAddress().tags();
        
        String expected = FAKE_ASSOCIATION_ID;
        
        // exercise
        String associationId = this.plugin.getAssociationIdFrom(tags);
        
        // verify
        Assert.assertEquals(expected, associationId);
    }
    
    // test case: When calling the getAssociationIdFrom method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testGetAssociationIdFromTagsFail() throws FogbowException {
        // set up
        Tag[] tags = { Tag.builder().key(TestUtils.ANY_VALUE).build() };

        String expected = Messages.Exception.UNEXPECTED_ERROR;

        try {
            // exercise
            this.plugin.getAssociationIdFrom(Arrays.asList(tags));
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }

    }
    
    // test case: When calling the doDisassociateAddresses method, it must verify
    // that is call was successful.
    @Test
    public void testDoDisassociateAddresses() throws FogbowException {
        // set up
        String associationId = FAKE_ASSOCIATION_ID;
        
        DisassociateAddressRequest request = DisassociateAddressRequest.builder()
                .associationId(associationId)
                .build();
        
        DisassociateAddressResponse response = DisassociateAddressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).disassociateAddress(Mockito.eq(request));

        // exercise
        this.plugin.doDisassociateAddresses(associationId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).disassociateAddress(Mockito.eq(request));
    }
    
    // test case: When calling the doDisassociateAddresses method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoDisassociateAddressesFail() throws FogbowException {
        // set up
        String associationId = FAKE_ASSOCIATION_ID;

        DisassociateAddressRequest request = DisassociateAddressRequest.builder()
                .associationId(associationId)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).disassociateAddress(Mockito.eq(request));
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doDisassociateAddresses(associationId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetInstance() throws Exception {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        Address address = buildAddress();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(address).when(AwsV2CloudUtil.class, TestUtils.GET_ADDRESS_BY_ID_METHOD,
                Mockito.eq(allocationId), Mockito.eq(this.client));
        
        // exercise
        this.plugin.doGetInstance(allocationId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getAddressById(Mockito.eq(allocationId), Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildPublicIpInstance(address);
    }
    
    // test case: When calling the doGetInstance method with null instance ID, it
    // must verify that was returned an error state.
    @Test
    public void testSetPublicIpInstanceStateWithNullInstanceId() {
        // set up
        Address address = Address.builder().instanceId(null).build();
        
        String expected = AwsV2StateMapper.ERROR_STATE;

        // exercise
        String state = this.plugin.setPublicIpInstanceState(address);

        // verify
        Assert.assertEquals(expected, state);
    }
    
    // test case: When calling the doRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        PublicIpOrder order = createPublicIpOrder();
        String allocationId = FAKE_ALLOCATION_ID;
        String groupId = FAKE_GROUP_ID;
        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;

        Mockito.doReturn(allocationId).when(this.plugin).doAllocateAddresses(Mockito.eq(this.client));
        
        Mockito.doReturn(groupId).when(this.plugin).handleSecurityIssues(Mockito.eq(allocationId),
                Mockito.eq(this.client));
        
        Mockito.doReturn(networkInterfaceId).when(this.plugin).getInstanceNetworkInterfaceId(Mockito.eq(order.getComputeId()),
                Mockito.eq(this.client));
        
        Mockito.doNothing().when(this.plugin).doModifyNetworkInterfaceAttributes(Mockito.eq(allocationId),
                Mockito.eq(groupId), Mockito.eq(networkInterfaceId), Mockito.eq(this.client));
        
        Mockito.doNothing().when(this.plugin).doAssociateAddress(Mockito.eq(allocationId),
                Mockito.eq(networkInterfaceId), Mockito.eq(this.client));
        
        // exercise
        this.plugin.doRequestInstance(order, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doAllocateAddresses(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).handleSecurityIssues(Mockito.eq(allocationId),
                Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getInstanceNetworkInterfaceId(Mockito.eq(order.getComputeId()), Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doModifyNetworkInterfaceAttributes(
                Mockito.eq(allocationId), Mockito.eq(groupId), Mockito.eq(networkInterfaceId), Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doAssociateAddress(Mockito.eq(allocationId),
                Mockito.eq(networkInterfaceId), Mockito.eq(this.client));
    }
    
    // test case: When calling the doAssociateAddress method, it must verify
    // that is call was successful.
    @Test
    public void testDoAssociateAddress() throws Exception {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String associationId = FAKE_ASSOCIATION_ID;
        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;

        AssociateAddressRequest request = AssociateAddressRequest.builder()
                .allocationId(allocationId)
                .networkInterfaceId(networkInterfaceId)
                .build();

        AssociateAddressResponse response = AssociateAddressResponse.builder()
                .associationId(associationId)
                .build();

        Mockito.doReturn(response).when(this.client).associateAddress(Mockito.eq(request));
        
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD,
                Mockito.eq(allocationId), Mockito.eq(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID),
                Mockito.eq(associationId), Mockito.eq(this.client));

        // exercise
        this.plugin.doAssociateAddress(allocationId, networkInterfaceId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).associateAddress(Mockito.eq(request));
        
        PowerMockito.verifyStatic(AwsV2CloudUtil.class);
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(allocationId),
                Mockito.eq(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID), Mockito.eq(associationId),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the doAssociateAddress method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoAssociateAddressFail() throws Exception {
     // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).associateAddress(Mockito.any(AssociateAddressRequest.class));
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        try {
            // exercise
            this.plugin.doAssociateAddress(allocationId, networkInterfaceId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doAssociateAddress method, it must verify
    // that is call was successful.
    @Test
    public void testDoModifyNetworkInterfaceAttributes() throws FogbowException {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String groupId = FAKE_GROUP_ID;
        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;
        
        ModifyNetworkInterfaceAttributeRequest request = ModifyNetworkInterfaceAttributeRequest.builder()
                .groups(groupId)
                .networkInterfaceId(networkInterfaceId)
                .build();
        
        ModifyNetworkInterfaceAttributeResponse response = ModifyNetworkInterfaceAttributeResponse.builder().build();
        Mockito.doReturn(response).when(this.client).modifyNetworkInterfaceAttribute(Mockito.eq(request));

        // exercise
        this.plugin.doModifyNetworkInterfaceAttributes(allocationId, groupId, networkInterfaceId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).modifyNetworkInterfaceAttribute(Mockito.eq(request));
    }
    
    // test case: When calling the doModifyNetworkInterfaceAttributes method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has been
    // thrown and after that, the AwsV2CloudUtil.doDeleteSecurityGroup and
    // doReleaseAddresses methods must also be invoked.
    @Test
    public void testDoModifyNetworkInterfaceAttributesFail() throws Exception {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String groupId = FAKE_GROUP_ID;
        String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client)
                .modifyNetworkInterfaceAttribute(Mockito.any(ModifyNetworkInterfaceAttributeRequest.class));
        
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD, Mockito.eq(groupId), Mockito.eq(this.client));
        
        Mockito.doNothing().when(this.plugin).doReleaseAddresses(Mockito.eq(groupId), Mockito.eq(this.client));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doModifyNetworkInterfaceAttributes(allocationId, groupId, networkInterfaceId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());

            PowerMockito.verifyStatic(AwsV2CloudUtil.class);
            AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(groupId), Mockito.eq(this.client));

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doReleaseAddresses(Mockito.eq(allocationId),
                    Mockito.eq(this.client));
        }
    }
    
    // test case: When calling the doReleaseAddresses method, it must verify
    // that is call was successful.
    @Test
    public void testDoReleaseAddresses() throws FogbowException {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;

        ReleaseAddressRequest request = ReleaseAddressRequest.builder()
                .allocationId(allocationId)
                .build();

        ReleaseAddressResponse response = ReleaseAddressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).releaseAddress(Mockito.eq(request));

        // exercise
        this.plugin.doReleaseAddresses(allocationId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).releaseAddress(Mockito.eq(request));
    }
    
    // test case: When calling the doReleaseAddresses method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoReleaseAddressesFail() throws FogbowException {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).releaseAddress(Mockito.any(ReleaseAddressRequest.class));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        try {
            // exercise
            this.plugin.doReleaseAddresses(allocationId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getInstanceNetworkInterfaceId method, it must
    // verify that is call was successful.
    @Test
    public void testGetInstanceNetworkInterfaceId() throws Exception {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, TestUtils.DO_DESCRIBE_INSTANCE_BY_ID_METHOD, Mockito.eq(instanceId),
                Mockito.eq(this.client));

        Instance instance = Instance.builder().instanceId(instanceId).build();
        Mockito.doReturn(instance).when(this.plugin).getInstanceReservation(Mockito.eq(response));

        InstanceNetworkInterface networkInterface = buildNetworkInterface();
        Mockito.doReturn(networkInterface).when(this.plugin).selectNetworkInterfaceFrom(Mockito.eq(instance));

        // exercise
        this.plugin.getInstanceNetworkInterfaceId(instanceId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class);
        AwsV2CloudUtil.doDescribeInstanceById(Mockito.eq(instanceId), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getInstanceReservation(Mockito.eq(response));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).selectNetworkInterfaceFrom(Mockito.eq(instance));
    }
    
    // test case: When calling the selectNetworkInterfaceFrom method from a instance
    // network interface with a sub-net ID different of default, it must return this
    // network interface.
    @Test
    public void testSelectNetworkInterfaceFromInstance() {
        // set up
        Instance instance = buildInstance();

        InstanceNetworkInterface expected = buildNetworkInterface();

        // exercise
        InstanceNetworkInterface networkInterface = this.plugin.selectNetworkInterfaceFrom(instance);

        // verify
        Assert.assertEquals(expected, networkInterface);
    }
    
    // test case:  When calling the selectNetworkInterfaceFrom method from a instance
    // network interface with a default sub-net ID, it must return this
    // network interface.
    @Test
    public void testSelectNetworkInterfaceFromInstanceWithSubnetIdDefault() {
        // set up
        String defaultSubnetId = FAKE_SUBNET_ID;
        Instance instance = buildInstance(defaultSubnetId);

        InstanceNetworkInterface expected = instance.networkInterfaces().listIterator().next();

        // exercise
        InstanceNetworkInterface networkInterface = this.plugin.selectNetworkInterfaceFrom(instance);

        // verify
        Assert.assertEquals(expected, networkInterface);
    }
    
    // test case: When calling the getInstanceReservation method with a response
    // containing a valid instance reservation, it must return the instance
    // contained in this reservation.
    @Test
    public void testGetInstanceReservation() throws FogbowException {
        // set up
        DescribeInstancesResponse response = buildDescribeInstancesResponse();

        Instance expected = buildInstance();

        // exercise
        Instance instance = this.plugin.getInstanceReservation(response);

        // verify
        Assert.assertEquals(expected, instance);
    }

    // test case: When calling the getInstanceReservation method and return a null
    // response, it must verify that an InstanceNotFoundException has been thrown.
    @Test
    public void testGetInstanceReservationWithNullResponse() throws FogbowException {
        // set up
        DescribeInstancesResponse response = null;

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;
        try {
            // exercise
            this.plugin.getInstanceReservation(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getInstanceReservation method and return a empty
    // response, it must verify that an InstanceNotFoundException has been thrown.
    @Test
    public void testGetInstanceReservationWithEmptyResponse() throws FogbowException {
        // set up
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;
        try {
            // exercise
            this.plugin.getInstanceReservation(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getInstanceReservation method and return a
    // response with an empty reservation, it must verify that an
    // InstanceNotFoundException has been thrown.
    @Test
    public void testGetInstanceReservationEmpty() throws FogbowException {
        // set up
        Reservation reservation = Reservation.builder().build();
        DescribeInstancesResponse response = buildDescribeInstancesResponse(reservation);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;
        try {
            // exercise
            this.plugin.getInstanceReservation(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the handleSecurityIssues method, it must
    // verify that is call was successful.
    @Test
    public void testHandleSecurityIssues() throws Exception {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String defaultVpcId = FAKE_DEFAULT_VPC_ID;
        String description = AwsV2PublicIpPlugin.SECURITY_GROUP_DESCRIPTION;
        String groupId = FAKE_GROUP_ID;
        String groupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + allocationId;
        String tagKey = AwsV2CloudUtil.AWS_TAG_GROUP_ID;

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(AwsV2PublicIpPlugin.DEFAULT_DESTINATION_CIDR)
                .fromPort(AwsV2PublicIpPlugin.SSH_DEFAULT_PORT)
                .toPort(AwsV2PublicIpPlugin.SSH_DEFAULT_PORT)
                .groupId(groupId)
                .ipProtocol(AwsV2PublicIpPlugin.TCP_PROTOCOL)
                .build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.CREATE_SECURITY_GROUP_METHOD,
                Mockito.eq(defaultVpcId), Mockito.eq(groupName), Mockito.eq(description), Mockito.eq(this.client));

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD,
                Mockito.eq(allocationId), Mockito.eq(tagKey), Mockito.eq(groupId), Mockito.eq(this.client));

        // exercise
        this.plugin.handleSecurityIssues(allocationId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createSecurityGroup(Mockito.eq(defaultVpcId), Mockito.eq(groupName), Mockito.eq(description),
                Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(Mockito.eq(request), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(allocationId), Mockito.eq(tagKey), Mockito.eq(groupId),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the handleSecurityIssues method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown and
    // after that, the doReleaseAddresses methods must also be invoked.
    @Test
    public void testHandleSecurityIssuesFail() throws Exception {
        // set up
        String allocationId = FAKE_ALLOCATION_ID;
        String defaultVpcId = FAKE_DEFAULT_VPC_ID;
        String description = AwsV2PublicIpPlugin.SECURITY_GROUP_DESCRIPTION;
        String groupId = FAKE_GROUP_ID;
        String groupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + allocationId;

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(AwsV2PublicIpPlugin.DEFAULT_DESTINATION_CIDR)
                .fromPort(AwsV2PublicIpPlugin.SSH_DEFAULT_PORT)
                .toPort(AwsV2PublicIpPlugin.SSH_DEFAULT_PORT)
                .groupId(groupId)
                .ipProtocol(AwsV2PublicIpPlugin.TCP_PROTOCOL)
                .build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.CREATE_SECURITY_GROUP_METHOD,
                Mockito.eq(defaultVpcId), Mockito.eq(groupName), Mockito.eq(description), Mockito.eq(this.client));

        UnexpectedException exception = new UnexpectedException();
        PowerMockito.doThrow(exception).when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));
        
        Mockito.doNothing().when(this.plugin).doReleaseAddresses(Mockito.eq(allocationId), Mockito.eq(this.client));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.handleSecurityIssues(allocationId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doReleaseAddresses(Mockito.eq(allocationId),
                    Mockito.eq(this.client));
        }
    }
    
    // test case: When calling the doAllocateAddresses method, it must
    // verify that is call was successful.
    @Test
    public void testDoAllocateAddresses() throws FogbowException {
        // set up
        AllocateAddressRequest request = AllocateAddressRequest.builder()
                .domain(DomainType.VPC)
                .build();

        AllocateAddressResponse response = buildAllocateAddressResponse();
        Mockito.doReturn(response).when(this.client).allocateAddress(Mockito.eq(request));

        // exercise
        this.plugin.doAllocateAddresses(this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).allocateAddress(Mockito.eq(request));
    }

    // test case: When calling the doAllocateAddresses method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoAllocateAddressesFail() throws FogbowException {
        // set up
        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).allocateAddress(Mockito.any(AllocateAddressRequest.class));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        try {
            // exercise
            this.plugin.doAllocateAddresses(this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    private AllocateAddressResponse buildAllocateAddressResponse() {
        String allocationId = FAKE_ALLOCATION_ID;
        
        AllocateAddressResponse response = AllocateAddressResponse.builder()
                .allocationId(allocationId)
                .build();
        
        return response;
    }
    
    private DescribeInstancesResponse buildDescribeInstancesResponse(Reservation... reservations) {
        Reservation reservation = reservations.length > 0 ? reservations[0]
                : buildReservation();

        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(reservation)
                .build();

        return response;
    }

    private Reservation buildReservation(Instance... instances) {
        Instance instance = instances.length > 0 ? instances[0] : buildInstance();

        Reservation reservation = Reservation.builder()
                .instances(instance)
                .build();

        return reservation;
    }
    
    private Instance buildInstance(String... subnetIds) {
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        
        Instance instance = Instance.builder()
                .instanceId(instanceId)
                .networkInterfaces(buildNetworkInterface(subnetIds))
                .build();
        
        return instance;
    }

    private InstanceNetworkInterface buildNetworkInterface(String... subnetIds) {
        String subnetId = subnetIds.length > 0 ? subnetIds[0] : TestUtils.FAKE_NETWORK_ID;

        InstanceNetworkInterface networkInterface = InstanceNetworkInterface.builder()
                .subnetId(subnetId)
                .build();

        return networkInterface;
    }
    
    private Address buildAddress() {
        Tag tagAssociationId = buildTag(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID, FAKE_ASSOCIATION_ID);
        Tag tagGroupId = buildTag(AwsV2CloudUtil.AWS_TAG_GROUP_ID, FAKE_GROUP_ID);

        Address address = Address.builder()
                .allocationId(FAKE_ALLOCATION_ID)
                .instanceId(TestUtils.FAKE_INSTANCE_ID)
                .networkInterfaceId(FAKE_NETWORK_INTERFACE_ID)
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
