package cloud.fogbow.ras.core.plugins.interoperability.aws;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2CloudUtil {

    protected void doCreateTagsRequests(String key, String value, String resourceId, Ec2Client client)
            throws UnexpectedException {

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
    
    public static int getAllDisksSize(List<Volume> volumes) {
        int size = 0;
        for (Volume volume : volumes) {
            size += volume.size();
        }
        return size;
    }

    public static List<Volume> getInstanceVolumes(Instance instance, Ec2Client client) throws FogbowException {
        List<Volume> volumes = new ArrayList<>();
        DescribeVolumesResponse response;
        List<String> volumeIds = getVolumeIds(instance);
        for (String volumeId : volumeIds) {
            response = doDescribeVolumes(volumeId, client);
            volumes.addAll(response.volumes());
        }
        return volumes;
    }

    public static DescribeVolumesResponse doDescribeVolumes(String volumeId, Ec2Client client) throws FogbowException {
        DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                .volumeIds(volumeId)
                .build();
        try {
            return client.describeVolumes(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public static List<String> getVolumeIds(Instance instance) {
        List<String> volumeIds = new ArrayList<String>();
        String volumeId;
        for (int i = 0; i < instance.blockDeviceMappings().size(); i++) {
            volumeId = instance.blockDeviceMappings().get(i).ebs().volumeId();
            volumeIds.add(volumeId);
        }
        return volumeIds;
    }

    public static List<Instance> getInstanceReservation(Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = doDescribeInstances(client);
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : response.reservations()) {
            instances.addAll(reservation.instances());
        }
        return instances;
    }

    public static DescribeInstancesResponse doDescribeInstances(Ec2Client client) throws FogbowException {
        try {
            return client.describeInstances();
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
}
