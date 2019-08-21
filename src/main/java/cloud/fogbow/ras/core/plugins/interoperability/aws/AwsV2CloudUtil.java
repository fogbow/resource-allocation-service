package cloud.fogbow.ras.core.plugins.interoperability.aws;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

public class AwsV2CloudUtil {

    public static final Logger LOGGER = Logger.getLogger(AwsV2CloudUtil.class);
    public static final Object SECURITY_GROUP_RESOURCE = "Security Groups";
    public static final String AWS_TAG_GROUP_ID = "groupId";
    public static final String TCP_PROTOCOL = "tcp";
    public static final String AWS_TAG_NAME = "Name";
    public static final Integer SSH_DEFAULT_PORT = 22;
    
    public static Image getImagesFrom(DescribeImagesResponse response) throws FogbowException {
        if (response != null && !response.images().isEmpty()) {
            return response.images().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }
    
    public static DescribeImagesResponse doDescribeImagesRequest(DescribeImagesRequest request, Ec2Client client)
            throws FogbowException {
        try {
            return client.describeImages(request);
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
    
    public static Volume getVolumeFrom(DescribeVolumesResponse response) throws FogbowException {
        if (response != null && !response.volumes().isEmpty()) {
            return response.volumes().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }
    
    public static DescribeVolumesResponse doDescribeVolumesRequest(Ec2Client client, DescribeVolumesRequest request)
            throws FogbowException {
        try {
            return client.describeVolumes(request);
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
    
    public static void createTagsRequest(String resourceId, String key, String value, Ec2Client client)
            throws FogbowException {

        Tag tag = Tag.builder()
                .key(key)
                .value(value)
                .build();

        CreateTagsRequest request = CreateTagsRequest.builder()
                .resources(resourceId)
                .tags(tag)
                .build();
        try {
            client.createTags(request);
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static void doDeleteSecurityGroup(String groupId, Ec2Client client) throws FogbowException {
        DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
                .groupId(groupId)
                .build();
        try {
            client.deleteSecurityGroup(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SECURITY_GROUP_RESOURCE, groupId), e);
            throw new UnexpectedException();
        }
    }

    public static String createSecurityGroup(Ec2Client client, String vpcId, String groupName, String description) throws FogbowException {
        try {
            CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                    .description(description)
                    .groupName(groupName)
                    .vpcId(vpcId)
                    .build();
            
            CreateSecurityGroupResponse response = client.createSecurityGroup(request);
            return response.groupId();
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static void doAuthorizeSecurityGroupIngress(Ec2Client client, AuthorizeSecurityGroupIngressRequest request)
            throws FogbowException {
        try {
            client.authorizeSecurityGroupIngress(request);
        } catch (SdkException e) {
            AwsV2CloudUtil.doDeleteSecurityGroup(request.groupId(), client);
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static DescribeInstancesResponse describeInstance(String instanceId, Ec2Client client)
            throws FogbowException {

        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        try {
            return client.describeInstances(describeInstancesRequest);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static DescribeInstancesResponse describeInstances(Ec2Client client) throws FogbowException {
        try {
            return client.describeInstances();
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static Instance getInstanceReservation(DescribeInstancesResponse response) throws InstanceNotFoundException {
        if (!response.reservations().isEmpty()) {
            Reservation reservation = response.reservations().listIterator().next();
            if (!reservation.instances().isEmpty()) {
                return reservation.instances().listIterator().next();
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    public static List<Volume> getInstanceVolumes(Instance instance, Ec2Client client) throws FogbowException {
        List<Volume> volumes = new ArrayList<>();
        DescribeVolumesRequest request;
        DescribeVolumesResponse response;

        List<String> volumeIds = AwsV2CloudUtil.getVolumeIds(instance);
        for (String volumeId : volumeIds) {
            request = DescribeVolumesRequest.builder().volumeIds(volumeId).build();
            response = AwsV2CloudUtil.doDescribeVolumesRequest(client, request);
            volumes.addAll(response.volumes());
        }
        return volumes;
    }

    public static List<String> getVolumeIds(Instance instance) {
        List<String> volumeIds = new ArrayList<String>();
        for (int i = 0; i < instance.blockDeviceMappings().size(); i++) {
            volumeIds.add(instance.blockDeviceMappings().get(i).ebs().volumeId());
        }
        return volumeIds;
    }
    
}
