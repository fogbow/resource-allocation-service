package cloud.fogbow.ras.core.plugins.interoperability.aws;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

@PrepareForTest({AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class})
public class AwsV2CloudUtilTest extends BaseUnitTests {

    private String FAKE_IMAGE_ID = "fake-img-id";
    private String FAKE_VOLUME_ID = "fake-volume-id";
    private String FAKE_RESOURCE_ID = "compute_id";
    private String FAKE_TAG_KEY = "fake-tag-key";
    private String FAKE_TAG_VALUE = "fake-tag-value";
    private String FAKE_GROUP_ID = "fake-group-id";
    private String FAKE_GROUP_NAME = "group-name";
    private String FAKE_CIDR = "0.0.0.0/0";
    private String FAKE_VPC_ID = "fake-vpc-id";
    private String FAKE_GROUP_DESCRIPTION = "fake-description";
    private int SSH_DEFAULT_PORT = 22;
    private String TCP_PROTOCOL = "tcp";

    @Before
    public void setup() throws FogbowException{
        super.setup();
        testUtils.mockReadOrdersFromDataBase();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is null
    @Test(expected = cloud.fogbow.common.exceptions.InstanceNotFoundException.class)//verify
    public void testGetImagesFromWithNullResponse() throws cloud.fogbow.common.exceptions.InstanceNotFoundException {
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
        Image image = Image.builder().imageId(FAKE_IMAGE_ID).build();
        DescribeImagesResponse response = DescribeImagesResponse.builder().images(image).build();
        //exercise/verify
        Assert.assertEquals(image, AwsV2CloudUtil.getImagesFrom(response));
    }

    //test case: test if the method make the expected call
    @Test
    public void testDoDescribeImagesRequest() throws FogbowException {
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeImagesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class)))
            .thenReturn(DescribeImagesResponse.builder().build());
        //exercise
        AwsV2CloudUtil.doDescribeImagesRequest(DescribeImagesRequest.builder().build(), client);
        //verify
        Mockito.verify(client, Mockito.times(1)).describeImages(Mockito.any(DescribeImagesRequest.class));
    }

    //test case: check if the method throws an InstanceNotFoundException when the response is null
    @Test(expected = cloud.fogbow.common.exceptions.InstanceNotFoundException.class)//verify
    public void testGetVolumeFromWithNullResponse() throws cloud.fogbow.common.exceptions.InstanceNotFoundException {
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
        Volume volume = Volume.builder().volumeId(FAKE_VOLUME_ID).build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(volume).build();
        //exercise/verify
        Assert.assertEquals(volume, AwsV2CloudUtil.getVolumeFrom(response));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDoDescribeVolumesRequest() throws FogbowException {
        //setup
        Mockito.when(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().build());
        //exercise
        AwsV2CloudUtil.doDescribeVolumesRequest(DescribeVolumesRequest.builder().build(), client);
        //verify
        Mockito.verify(client, Mockito.times(1)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));
    }

    //test case: check if the method makes the expected call.
    @Test
    public void testCreateTagsRequest() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, "createTagsRequest", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Tag tag = Tag.builder()
            .key(FAKE_TAG_KEY)
            .value(FAKE_TAG_VALUE)
            .build();
        CreateTagsRequest request = CreateTagsRequest.builder()
            .resources(FAKE_RESOURCE_ID)
            .tags(tag)
            .build();
        CreateTagsResponse response = CreateTagsResponse.builder().build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.createTags(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.createTagsRequest(FAKE_RESOURCE_ID, FAKE_TAG_KEY, FAKE_TAG_VALUE, client);
        //verify
        Mockito.verify(client, Mockito.times(1)).createTags(Mockito.eq(request));
    }

    //test case: check if the method makes the expected call.
    @Test
    public void testDoDeleteSecurityGroup() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, "doDeleteSecurityGroup", Mockito.any(), Mockito.any());
        DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
            .groupId(FAKE_GROUP_ID)
            .build();
        DeleteSecurityGroupResponse response = DeleteSecurityGroupResponse.builder().build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.deleteSecurityGroup(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.doDeleteSecurityGroup(FAKE_GROUP_ID, client);
        //verify
        Mockito.verify(client, Mockito.times(1)).deleteSecurityGroup(Mockito.eq(request));
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
        CreateSecurityGroupResponse response = CreateSecurityGroupResponse.builder().groupId(FAKE_GROUP_ID).build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.createSecurityGroup(Mockito.eq(request))).thenReturn(response);
        Mockito.when(AwsV2CloudUtil.createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenCallRealMethod();
        //exercise
        AwsV2CloudUtil.createSecurityGroup(FAKE_VPC_ID, FAKE_GROUP_NAME, FAKE_GROUP_DESCRIPTION, client);
        //verify
        Mockito.verify(client, Mockito.times(1)).createSecurityGroup(Mockito.eq(request));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDoAuthorizeSecurityGroupIngressInSuccessCase() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, "doAuthorizeSecurityGroupIngress", Mockito.any(), Mockito.any());
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
            .cidrIp(FAKE_CIDR)
            .fromPort(SSH_DEFAULT_PORT)
            .toPort(SSH_DEFAULT_PORT)
            .groupId(FAKE_GROUP_ID)
            .ipProtocol(TCP_PROTOCOL)
            .build();
        AuthorizeSecurityGroupIngressResponse response = AuthorizeSecurityGroupIngressResponse.builder().build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.authorizeSecurityGroupIngress(Mockito.eq(request))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
        //verify
        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupIngress(Mockito.eq(request));
    }

    //test case: check if the method makes the right calls in failure case
    @Test
    public void testDoAuthorizeSecurityGroupIngressInFailureCase() throws Exception{
        //setup
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, "doAuthorizeSecurityGroupIngress", Mockito.any(), Mockito.any());
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
            .cidrIp(FAKE_CIDR)
            .fromPort(SSH_DEFAULT_PORT)
            .toPort(SSH_DEFAULT_PORT)
            .groupId(FAKE_GROUP_ID)
            .ipProtocol(TCP_PROTOCOL)
            .build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.authorizeSecurityGroupIngress(Mockito.eq(request))).thenThrow(SdkException.class);
        try {
            //exercise
            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
        } catch (UnexpectedException ex) { }
        //verify
        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupIngress(Mockito.eq(request));
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(1));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(FAKE_GROUP_ID), Mockito.eq(client));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDescribeInstance() throws FogbowException{
        //setup
        Mockito.when(AwsV2CloudUtil.describeInstance(Mockito.any(), Mockito.any())).thenCallRealMethod();
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
            .instanceIds(FAKE_RESOURCE_ID)
            .build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.describeInstances(Mockito.eq(describeInstancesRequest))).thenReturn(response);
        //exercise
        AwsV2CloudUtil.describeInstance(FAKE_RESOURCE_ID, client);
        //verify
        Mockito.verify(client, Mockito.times(1)).describeInstances(Mockito.eq(describeInstancesRequest));
    }

    //test case: check if the method makes the expected call
    @Test
    public void testDescribeInstances() throws FogbowException{
        //setup
        Mockito.when(AwsV2CloudUtil.describeInstances(Mockito.any())).thenCallRealMethod();
        Ec2Client client = testUtils.getAwsMockedClient();
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder().build();
        Mockito.when(client.describeInstances()).thenReturn(describeInstancesResponse);
        //exercise
        AwsV2CloudUtil.describeInstances(client);
        //verify
        Mockito.verify(client, Mockito.times(1)).describeInstances();
    }

    //test case: check if the method throws InstanceNotFoundException when there is no reservation
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetInstanceReservationWithNoReservation() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(new ArrayList<>()).build();
        //exercise
        AwsV2CloudUtil.getInstanceReservation(response);
    }

    //test case: check if the method throws InstanceNotFoundException when there is no instance
    @Test(expected = InstanceNotFoundException.class)//verify
    public void testGetInstanceReservationWithNoInstance() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        Reservation reservation = Reservation.builder().instances(new ArrayList<>()).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        //exercise
        AwsV2CloudUtil.getInstanceReservation(response);
    }

    //test case: check if the method returns the expected instance when the response is ok
    @Test
    public void testGetInstanceReservationWithAValidResponse() throws InstanceNotFoundException{
        //setup
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        Instance instance = Instance.builder().instanceId(FAKE_RESOURCE_ID).build();
        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        //exercise/verify
        Assert.assertEquals(instance, AwsV2CloudUtil.getInstanceReservation(response));
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
        Ec2Client client = testUtils.getAwsMockedClient();
        //exercise
        AwsV2CloudUtil.getInstanceVolumes(instance, client);
        //verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(volumeIds.size()));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(DescribeVolumesRequest.class), Mockito.eq(client));
    }

}