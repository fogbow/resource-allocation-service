package cloud.fogbow.ras.core.plugins.interoperability.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2CloudUtilTest extends BaseUnitTests {

    private static final String FAKE_ALLOCATION_ID = "fake-allocation-id";
    private static final String FAKE_CIDR = "0.0.0.0/0";
    private static final String FAKE_GROUP_DESCRIPTION = "fake-description";
    private static final String FAKE_GROUP_NAME = "fake-group-name";
    private static final String FAKE_RESOURCE_ID = "compute_id";
    private static final String FAKE_TAG_KEY = "fake-tag-key";
    private static final String FAKE_TAG_VALUE = "fake-tag-value";
    private static final String FAKE_VPC_ID = "fake-vpc-id";
    private static final String TCP_PROTOCOL = "tcp";
    
    private static final int SSH_DEFAULT_PORT = 22;
    
    private Ec2Client client;

    @Before
    public void setup() throws FogbowException{
        super.setup();
        testUtils.mockReadOrdersFromDataBase();
        this.client = this.testUtils.getAwsMockedClient();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is null
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetImagesFromWithNullResponse() throws InstanceNotFoundException {
        //setup
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        DescribeImagesResponse response = null;
        //exercise
        AwsV2CloudUtil.getImagesFrom(response);
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is empty
    @Test(expected = InstanceNotFoundException.class)
    public void testGetImagesFromWithEmptyResponse() throws InstanceNotFoundException {
        //setup
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        DescribeImagesResponse response = DescribeImagesResponse.builder().images(new ArrayList<>()).build();
        //exercise
        AwsV2CloudUtil.getImagesFrom(response);
    }

    //test case: test if the method returns the expected image
    @Test
    public void testGetImagesFromWithValidResponse() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        Image image = Image.builder().imageId(TestUtils.FAKE_IMAGE_ID).build();
        DescribeImagesResponse response = DescribeImagesResponse.builder().images(image).build();
        //exercise/verify
        Assert.assertEquals(image, AwsV2CloudUtil.getImagesFrom(response));
    }

    //test case: test if the method make the expected call
    @Test
    public void testDoDescribeImagesRequest() throws FogbowException {
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeImagesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Mockito.when(this.client.describeImages(Mockito.any(DescribeImagesRequest.class)))
            .thenReturn(DescribeImagesResponse.builder().build());
        //exercise
        AwsV2CloudUtil.doDescribeImagesRequest(DescribeImagesRequest.builder().build(), this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeImages(Mockito.any(DescribeImagesRequest.class));
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is null
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetVolumeFromWithNullResponse() throws InstanceNotFoundException {
        //setup
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        DescribeVolumesResponse response = null;
        //exercise
        AwsV2CloudUtil.getVolumeFrom(response);
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is empty
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetVolumeFromWithEmptyResponse() throws InstanceNotFoundException {
        //setup
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(new ArrayList<>()).build();
        //exercise
        AwsV2CloudUtil.getVolumeFrom(response);
    }

    //test case: test if the method returns the expected instance
    @Test
    public void testGetVolumeFromWithValidResponse() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        Volume volume = Volume.builder().volumeId(TestUtils.FAKE_VOLUME_ID).build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(volume).build();
        //exercise/verify
        Assert.assertEquals(volume, AwsV2CloudUtil.getVolumeFrom(response));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDoDescribeVolumesRequest() throws FogbowException {
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Mockito.when(this.client.describeVolumes(Mockito.any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().build());
        //exercise
        AwsV2CloudUtil.doDescribeVolumesRequest(DescribeVolumesRequest.builder().build(), this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));
    }

    //test case: check if the method makes the expected call.
    @Test
    public void testCreateTagsRequest() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD, Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Tag tag = Tag.builder()
            .key(FAKE_TAG_KEY)
            .value(FAKE_TAG_VALUE)
            .build();
        CreateTagsRequest request = CreateTagsRequest.builder()
            .resources(FAKE_RESOURCE_ID)
            .tags(tag)
            .build();
        CreateTagsResponse response = CreateTagsResponse.builder().build();
        Mockito.when(this.client.createTags(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.createTagsRequest(FAKE_RESOURCE_ID, FAKE_TAG_KEY, FAKE_TAG_VALUE, this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createTags(Mockito.eq(request));
    }

    //test case: check if the method makes the expected call.
    @Test
    public void testDoDeleteSecurityGroup() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD, Mockito.any(), Mockito.any());
        DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
            .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
            .build();
        DeleteSecurityGroupResponse response = DeleteSecurityGroupResponse.builder().build();
        Mockito.when(this.client.deleteSecurityGroup(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.doDeleteSecurityGroup(TestUtils.FAKE_SECURITY_GROUP_ID, this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityGroup(Mockito.eq(request));
    }

    //test case: test if the method makes the expected call
    @Test
    public void testCreateSecurityGroup() throws FogbowException{
        //setup
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
            .description(FAKE_GROUP_DESCRIPTION)
            .groupName(FAKE_GROUP_NAME)
            .vpcId(FAKE_VPC_ID)
            .build();
        CreateSecurityGroupResponse response = CreateSecurityGroupResponse.builder().groupId(TestUtils.FAKE_SECURITY_GROUP_ID).build();
        Mockito.when(this.client.createSecurityGroup(Mockito.eq(request))).thenReturn(response);
        Mockito.when(AwsV2CloudUtil.createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenCallRealMethod();
        //exercise
        AwsV2CloudUtil.createSecurityGroup(FAKE_VPC_ID, FAKE_GROUP_NAME, FAKE_GROUP_DESCRIPTION, this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(Mockito.eq(request));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDoAuthorizeSecurityGroupIngressInSuccessCase() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD, Mockito.any(), Mockito.any());
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
            .cidrIp(FAKE_CIDR)
            .fromPort(SSH_DEFAULT_PORT)
            .toPort(SSH_DEFAULT_PORT)
            .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
            .ipProtocol(TCP_PROTOCOL)
            .build();
        AuthorizeSecurityGroupIngressResponse response = AuthorizeSecurityGroupIngressResponse.builder().build();
        Mockito.when(this.client.authorizeSecurityGroupIngress(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).authorizeSecurityGroupIngress(Mockito.eq(request));
    }

    //test case: check if the method makes the right calls in failure case
    @Test
    public void testDoAuthorizeSecurityGroupIngressInFailureCase() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD, Mockito.any(), Mockito.any());
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
            .cidrIp(FAKE_CIDR)
            .fromPort(SSH_DEFAULT_PORT)
            .toPort(SSH_DEFAULT_PORT)
            .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
            .ipProtocol(TCP_PROTOCOL)
            .build();
        Mockito.when(this.client.authorizeSecurityGroupIngress(Mockito.eq(request))).thenThrow(SdkException.class);
        try {
            //exercise
            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, this.client);
        } catch (UnexpectedException ex) { }
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).authorizeSecurityGroupIngress(Mockito.eq(request));
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(TestUtils.FAKE_SECURITY_GROUP_ID), Mockito.eq(this.client));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDescribeInstance() throws FogbowException{
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeInstanceById(Mockito.any(), Mockito.any())).thenCallRealMethod();
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
            .instanceIds(FAKE_RESOURCE_ID)
            .build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();
        Mockito.when(this.client.describeInstances(Mockito.eq(describeInstancesRequest))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.doDescribeInstanceById(FAKE_RESOURCE_ID, this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeInstances(Mockito.eq(describeInstancesRequest));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDescribeInstances() throws FogbowException{
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeInstances(Mockito.any())).thenCallRealMethod();
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder().build();
        Mockito.when(this.client.describeInstances()).thenReturn(describeInstancesResponse);
        //exercise
        AwsV2CloudUtil.doDescribeInstances(this.client);
        //verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeInstances();
    }

    //test case: check if the method throws InstanceNotFoundException when there is no reservation
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetInstanceReservationWithNoReservation() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceFrom(Mockito.any())).thenCallRealMethod();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(new ArrayList<>()).build();
        //exercise
        AwsV2CloudUtil.getInstanceFrom(response);
    }

    //test case: check if the method throws InstanceNotFoundException when there is no instance
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetInstanceReservationWithNoInstance() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceFrom(Mockito.any())).thenCallRealMethod();
        Reservation reservation = Reservation.builder().instances(new ArrayList<>()).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        //exercise
        AwsV2CloudUtil.getInstanceFrom(response);
    }

    //test case: check if the method returns the expected instance when the response is ok
    @Test
    public void testGetInstanceReservationWithAValidResponse() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceFrom(Mockito.any())).thenCallRealMethod();
        Instance instance = Instance.builder().instanceId(FAKE_RESOURCE_ID).build();
        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        //exercise/verify
        Assert.assertEquals(instance, AwsV2CloudUtil.getInstanceFrom(response));
    }

    //test case: test if the method makes the expected call.
    @Test
    public void testGetInstanceVolumes() throws FogbowException {
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.any(), Mockito.any())).thenCallRealMethod();
        List<Volume> volumes = new ArrayList<>();
        volumes.add(Volume.builder().volumeId("volume1").build());
        volumes.add(Volume.builder().volumeId("volume2").build());
        volumes.add(Volume.builder().volumeId("volume3").build());
        List<String> volumeIds = new ArrayList<>();
        volumeIds.add("volume1");
        volumeIds.add("volume2");
        volumeIds.add("volume3");
        Instance instance = Instance.builder().instanceId(FAKE_RESOURCE_ID).build();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        BDDMockito.given(AwsV2CloudUtil.getVolumeIds(Mockito.any(Instance.class))).willReturn(volumeIds);
        BDDMockito.given(AwsV2CloudUtil.getInstanceVolumes(Mockito.any(Instance.class), Mockito.any(Ec2Client.class))).willCallRealMethod();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(volumes).build();
        BDDMockito.given(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(DescribeVolumesRequest.class), Mockito.any(Ec2Client.class))).willReturn(response);
        //exercise
        AwsV2CloudUtil.getInstanceVolumes(instance, this.client);
        //verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(volumeIds.size()));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(DescribeVolumesRequest.class), Mockito.eq(this.client));
    }
    
    // test case: When calling the getAddressById method, it must verify
    // that is call was successful.
    @Test
    public void testGetAddressById() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.getAddressById(Mockito.any(), Mockito.any())).thenCallRealMethod();

        String allocationId = FAKE_ALLOCATION_ID;
        DescribeAddressesRequest request = DescribeAddressesRequest.builder()
                .allocationIds(allocationId)
                .build();

        DescribeAddressesResponse response = buildDescribeAddresses();
        Mockito.when(AwsV2CloudUtil.doDescribeAddressesRequests(Mockito.eq(request), Mockito.eq(this.client)))
                .thenReturn(response);
        
        Address address = buildAddress();
        Mockito.when(AwsV2CloudUtil.getAddressFrom(Mockito.eq(response))).thenReturn(address);

        // exercise
        AwsV2CloudUtil.getAddressById(allocationId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeAddressesRequests(Mockito.eq(request), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getAddressFrom(Mockito.eq(response));
    }
    
    // test case: When calling the getAddressFrom method, it must returned a
    // expected address.
    @Test
    public void testGetAddressFromResponse() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.getAddressFrom(Mockito.any())).thenCallRealMethod();

        DescribeAddressesResponse response = buildDescribeAddresses();

        Address expected = buildAddress();

        // exercise
        Address address = AwsV2CloudUtil.getAddressFrom(response);

        // verify
        Assert.assertEquals(expected, address);
    }
    
    // test case: When calling the getAddressFrom method with a null response, an
    // InstanceNotFoundException will be thrown.
    @Test
    public void testGetAddressFromNullResponse() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.getAddressFrom(Mockito.any())).thenCallRealMethod();

        DescribeAddressesResponse response = null;

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            AwsV2CloudUtil.getAddressFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getAddressFrom method with a response containing
    // an empty addresses list, an InstanceNotFoundException will be thrown.
    @Test
    public void testGetAddressFromEmptyResponse() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.getAddressFrom(Mockito.any())).thenCallRealMethod();

        Address[] addresses = {};
        DescribeAddressesResponse response = DescribeAddressesResponse.builder()
                .addresses(Arrays.asList(addresses))
                .build();
        
        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            AwsV2CloudUtil.getAddressFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doDescribeAddressesRequests method, it must
    // verify that is call was successful.
    @Test
    public void testDoDescribeAddressesRequests() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.doDescribeAddressesRequests(Mockito.any(), Mockito.any())).thenCallRealMethod();

        DescribeAddressesRequest request = DescribeAddressesRequest.builder().allocationIds(FAKE_ALLOCATION_ID).build();

        DescribeAddressesResponse response = buildDescribeAddresses();
        Mockito.doReturn(response).when(this.client).describeAddresses(Mockito.eq(request));

        // exercise
        AwsV2CloudUtil.doDescribeAddressesRequests(request, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeAddresses(Mockito.eq(request));
    }
    
    // test case: When calling the doDescribeAddressesRequests method, and an error
    // occurs during the request, an UnexpectedException will be thrown.
    @Test
    public void testdoDescribeAddressesRequestsFail() throws Exception {
        // set up
        Mockito.when(AwsV2CloudUtil.doDescribeAddressesRequests(Mockito.any(), Mockito.any())).thenCallRealMethod();

        DescribeAddressesRequest request = DescribeAddressesRequest.builder()
                .allocationIds(FAKE_ALLOCATION_ID)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).describeAddresses(Mockito.eq(request));
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            AwsV2CloudUtil.doDescribeAddressesRequests(request, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: ...
    @Test
    public void test() {
        // set up

        // exercise

        // verify
    }
    
    private DescribeAddressesResponse buildDescribeAddresses() {
        DescribeAddressesResponse response = DescribeAddressesResponse.builder()
                .addresses(buildAddress())
                .build();

        return response;
    }

    private Address buildAddress() {
        Tag tagGroupId = Tag.builder()
                .key(AwsV2CloudUtil.AWS_TAG_GROUP_ID)
                .value(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        Address address = Address.builder()
                .allocationId(FAKE_ALLOCATION_ID)
                .instanceId(TestUtils.FAKE_INSTANCE_ID)
                .tags(tagGroupId)
                .build();
        
        return address;
    }

}