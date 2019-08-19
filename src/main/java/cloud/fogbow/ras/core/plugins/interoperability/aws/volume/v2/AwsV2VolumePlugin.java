package cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2;

import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.FogbowCloudUtil;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2VolumePlugin implements VolumePlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2VolumePlugin.class);
	private static final String RESOURCE_NAME = "Volume";
	
	private String region;
	private String zone;
	
	public AwsV2VolumePlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.zone = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_AVAILABILITY_ZONE_KEY);
	}
	
	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
		
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String name = FogbowCloudUtil.defineInstanceName(volumeOrder.getName());
		
		CreateVolumeRequest request = CreateVolumeRequest.builder()
			.size(volumeOrder.getVolumeSize())
			.availabilityZone(this.zone)
			.build();

		return doCreateVolumeRequest(client, request, name);
	}

	@Override
	public VolumeInstance getInstance(VolumeOrder volumeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, volumeOrder.getInstanceId()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		
		DescribeVolumesRequest request = DescribeVolumesRequest.builder()
			.volumeIds(volumeOrder.getInstanceId())
			.build();

		DescribeVolumesResponse response = AwsV2CloudUtil.doDescribeVolumesRequest(request, client);
		return mountVolumeInstance(response);
	}
	
	@Override
	public void deleteInstance(VolumeOrder volumeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, volumeOrder.getInstanceId()));
		
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String volumeId = volumeOrder.getInstanceId();
		
		DeleteVolumeRequest request = DeleteVolumeRequest.builder()
			.volumeId(volumeOrder.getInstanceId())
			.build();
		
		try {
		    client.deleteVolume(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME, volumeId), e);
			throw new UnexpectedException();
		}
	}
	
	private String doCreateVolumeRequest(Ec2Client client, CreateVolumeRequest request, String name) throws FogbowException {
        String volumeId;
        try {
            CreateVolumeResponse response = client.createVolume(request);
            volumeId = response.volumeId();
            AwsV2CloudUtil.createTagsRequest(volumeId, AwsV2CloudUtil.AWS_TAG_NAME, name, client);
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
        return volumeId;
    }

    protected VolumeInstance mountVolumeInstance(DescribeVolumesResponse response) throws FogbowException {
        Volume volume = AwsV2CloudUtil.getVolumeFrom(response);
        String id = volume.volumeId();
        String cloudState = volume.stateAsString();
        String name = volume.tags().listIterator().next().value();
        Integer size = volume.size();
        return new VolumeInstance(id, cloudState, name, size);
    }
	
}
