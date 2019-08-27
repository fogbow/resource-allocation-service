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

    @Test(expected = cloud.fogbow.common.exceptions.InstanceNotFoundException.class)
    public void testGetImagesFromWithNullResponse() throws cloud.fogbow.common.exceptions.InstanceNotFoundException {
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        DescribeImagesResponse response = null;
        AwsV2CloudUtil.getImagesFrom(response);
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetImagesFromWithEmptyResponse() throws InstanceNotFoundException {
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        DescribeImagesResponse response = DescribeImagesResponse.builder().images(new ArrayList<>()).build();
        AwsV2CloudUtil.getImagesFrom(response);
    }

    @Test
    public void testGetImagesFromWithValidResponse() throws InstanceNotFoundException{
        Mockito.when(AwsV2CloudUtil.getImagesFrom(Mockito.any())).thenCallRealMethod();
        Image image = Image.builder().imageId(FAKE_IMAGE_ID).build();
        DescribeImagesResponse response = DescribeImagesResponse.builder().images(image).build();
        Assert.assertEquals(image, AwsV2CloudUtil.getImagesFrom(response));
    }

    @Test
    public void testDoDescribeImagesRequest() throws FogbowException {
        Mockito.when(AwsV2CloudUtil.doDescribeImagesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class)))
            .thenReturn(DescribeImagesResponse.builder().build());
        AwsV2CloudUtil.doDescribeImagesRequest(DescribeImagesRequest.builder().build(), client);
        Mockito.verify(client, Mockito.times(1)).describeImages(Mockito.any(DescribeImagesRequest.class));
    }

    @Test(expected = cloud.fogbow.common.exceptions.InstanceNotFoundException.class)
    public void testGetVolumeFromWithNullResponse() throws cloud.fogbow.common.exceptions.InstanceNotFoundException {
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        DescribeVolumesResponse response = null;
        AwsV2CloudUtil.getVolumeFrom(response);
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetVolumeFromWithEmptyResponse() throws InstanceNotFoundException {
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(new ArrayList<>()).build();
        AwsV2CloudUtil.getVolumeFrom(response);
    }

    @Test
    public void testGetVolumeFromWithValidResponse() throws InstanceNotFoundException{
        Mockito.when(AwsV2CloudUtil.getVolumeFrom(Mockito.any())).thenCallRealMethod();
        Volume volume = Volume.builder().volumeId(FAKE_VOLUME_ID).build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(volume).build();
        Assert.assertEquals(volume, AwsV2CloudUtil.getVolumeFrom(response));
    }

    @Test
    public void testDoDescribeVolumesRequest() throws FogbowException {
        Mockito.when(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Ec2Client client = testUtils.getAwsMockedClient();
        Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().build());
        AwsV2CloudUtil.doDescribeVolumesRequest(DescribeVolumesRequest.builder().build(), client);
        Mockito.verify(client, Mockito.times(1)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));
    }

    @Test
    public void testCreateTagsRequest() throws Exception{
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

        AwsV2CloudUtil.createTagsRequest(FAKE_RESOURCE_ID, FAKE_TAG_KEY, FAKE_TAG_VALUE, client);

        Mockito.verify(client, Mockito.times(1)).createTags(Mockito.eq(request));
    }

    @Test
    public void testDoDeleteSecurityGroup() throws Exception{
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, "doDeleteSecurityGroup", Mockito.any(), Mockito.any());

        DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
            .groupId(FAKE_GROUP_ID)
            .build();

        DeleteSecurityGroupResponse response = DeleteSecurityGroupResponse.builder().build();

        Ec2Client client = testUtils.getAwsMockedClient();

        Mockito.when(client.deleteSecurityGroup(Mockito.eq(request))).thenReturn(response);

        AwsV2CloudUtil.doDeleteSecurityGroup(FAKE_GROUP_ID, client);

        Mockito.verify(client, Mockito.times(1)).deleteSecurityGroup(Mockito.eq(request));
    }

    @Test
    public void testCreateSecurityGroup() throws FogbowException{
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
            .description(FAKE_GROUP_DESCRIPTION)
            .groupName(FAKE_GROUP_NAME)
            .vpcId(FAKE_VPC_ID)
            .build();

        CreateSecurityGroupResponse response = CreateSecurityGroupResponse.builder().groupId(FAKE_GROUP_ID).build();

        Ec2Client client = testUtils.getAwsMockedClient();

        Mockito.when(client.createSecurityGroup(Mockito.eq(request))).thenReturn(response);
        Mockito.when(AwsV2CloudUtil.createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenCallRealMethod();
        AwsV2CloudUtil.createSecurityGroup(FAKE_VPC_ID, FAKE_GROUP_NAME, FAKE_GROUP_DESCRIPTION, client);

        Mockito.verify(client, Mockito.times(1)).createSecurityGroup(Mockito.eq(request));
    }

    @Test
    public void testDoAuthorizeSecurityGroupIngressInSuccessCase() throws Exception{
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

        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);

        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupIngress(Mockito.eq(request));
    }

    @Test
    public void testDoAuthorizeSecurityGroupIngressInFailureCase() throws Exception{
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
            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
        } catch (UnexpectedException ex) { }


        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupIngress(Mockito.eq(request));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(1));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(FAKE_GROUP_ID), Mockito.eq(client));
    }

    @Test
    public void testDescribeInstance() throws FogbowException{
        Mockito.when(AwsV2CloudUtil.describeInstance(Mockito.any(), Mockito.any())).thenCallRealMethod();

        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
            .instanceIds(FAKE_RESOURCE_ID)
            .build();

        DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();

        Ec2Client client = testUtils.getAwsMockedClient();

        Mockito.when(client.describeInstances(Mockito.eq(describeInstancesRequest))).thenReturn(response);

        AwsV2CloudUtil.describeInstance(FAKE_RESOURCE_ID, client);

        Mockito.verify(client, Mockito.times(1)).describeInstances(Mockito.eq(describeInstancesRequest));
    }

    @Test
    public void testDescribeInstances() throws FogbowException{
        Mockito.when(AwsV2CloudUtil.describeInstances(Mockito.any())).thenCallRealMethod();

        Ec2Client client = testUtils.getAwsMockedClient();
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder().build();

        Mockito.when(client.describeInstances()).thenReturn(describeInstancesResponse);

        AwsV2CloudUtil.describeInstances(client);

        Mockito.verify(client, Mockito.times(1)).describeInstances();
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceReservationWithNoReservation() throws InstanceNotFoundException{
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(new ArrayList<>()).build();
        AwsV2CloudUtil.getInstanceReservation(response);
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceReservationWithNoInstance() throws InstanceNotFoundException{
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        Reservation reservation = Reservation.builder().instances(new ArrayList<>()).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        AwsV2CloudUtil.getInstanceReservation(response);
    }

    @Test
    public void testGetInstanceReservationWithAValidResponse() throws InstanceNotFoundException{
        Mockito.when(AwsV2CloudUtil.getInstanceReservation(Mockito.any())).thenCallRealMethod();
        Instance instance = Instance.builder().instanceId(FAKE_RESOURCE_ID).build();
        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();
        Assert.assertEquals(instance, AwsV2CloudUtil.getInstanceReservation(response));
    }

    @Test
    public void testGetInstanceVolumes() throws FogbowException {
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

        AwsV2CloudUtil.getInstanceVolumes(instance, client);

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(volumeIds.size()));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.any(DescribeVolumesRequest.class), Mockito.eq(client));
    }

}