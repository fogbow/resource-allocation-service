package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Volume;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class })
public class AwsV2ComputeQuotaPluginTest {

	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_VOLUME_ID = "fake-volume-id";

	private static final int DISK_DEFAULT_QUOTA_VALUE = 300000;
	private static final int INSTANCES_DEFAULT_QUOTA_VALUE = 20;
	private static final int MEMORY_DEFAULT_QUOTA_VALUE = 627;
	private static final int MEMORY_TX_NANO_QUOTA_VALUE = 512;
	private static final int ONE_VALUE = 1;
	private static final int VOLUME_SIZE_USED_VALUE = 4;

	private AwsV2ComputeQuotaPlugin plugin;

	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2ComputeQuotaPlugin(awsConfFilePath));
	}

	// test case: When calling the getUserQuota method with no running instance, it
	// must return the default instance type values ​​in the available quotas.
	@Test
	public void testGetUserQuotaWithoutRunningInstances() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		List<Instance> instances = null;
		describeInstanceMocked(instances, client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		int instancesQuota = INSTANCES_DEFAULT_QUOTA_VALUE;
		int vcpuQuota = ONE_VALUE * instancesQuota;
		int ramQuota = MEMORY_DEFAULT_QUOTA_VALUE * instancesQuota;
		int diskQuota = DISK_DEFAULT_QUOTA_VALUE;

		// exercise
		ComputeQuota quota = this.plugin.getUserQuota(cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeInstances();

		Assert.assertEquals(instancesQuota, quota.getAvailableQuota().getInstances());
		Assert.assertEquals(vcpuQuota, quota.getAvailableQuota().getvCPU());
		Assert.assertEquals(ramQuota, quota.getAvailableQuota().getRam());
		Assert.assertEquals(diskQuota, quota.getAvailableQuota().getDisk());
	}

	// test case: When calling the getUserQuota method containing a list of
	// instances with two instances of the same type running, it must return the
	// limit of this type of instance minus the two instances in use and calculate
	// other quota availability values based on that balance.
	@Test
	public void testGetUserQuotaWithSameInstancesTypeRunning() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		describeVolumeMocked(client);

		List<Instance> instances = new ArrayList<>();
		instances.add(buildInstance(InstanceType.T2_NANO));
		instances.add(buildInstance(InstanceType.T2_NANO));

		describeInstanceMocked(instances, client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		int instancesQuota = INSTANCES_DEFAULT_QUOTA_VALUE - ONE_VALUE * 2;
		int vcpuQuota = ONE_VALUE * instancesQuota;
		int memoryQuota = MEMORY_TX_NANO_QUOTA_VALUE * instancesQuota;
		int diskQuota = DISK_DEFAULT_QUOTA_VALUE - VOLUME_SIZE_USED_VALUE * 2;

		// exercise
		ComputeQuota quota = this.plugin.getUserQuota(cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeInstances();
		Mockito.verify(client, Mockito.times(2)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));

		Assert.assertEquals(instancesQuota, quota.getAvailableQuota().getInstances());
		Assert.assertEquals(vcpuQuota, quota.getAvailableQuota().getvCPU());
		Assert.assertEquals(memoryQuota, quota.getAvailableQuota().getRam());
		Assert.assertEquals(diskQuota, quota.getAvailableQuota().getDisk());
	}

	// test case: When calling the getUserQuota method containing a list of
	// instances with two instances of different types running, it must return the
	// limit of each instance type minus the instances in use and calculate the
	// other quota availability values based on that balance.
	@Test
	public void testGetUserQuotaWithDifferentInstanceTypesRunning() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		describeVolumeMocked(client);

		List<Instance> instances = new ArrayList<>();
		instances.add(buildInstance(InstanceType.T2_NANO));
		instances.add(buildInstance(InstanceType.T3_NANO));

		describeInstanceMocked(instances, client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		int instancesQuota = INSTANCES_DEFAULT_QUOTA_VALUE - ONE_VALUE;
		int vcpuQuota = ONE_VALUE * instancesQuota;
		int memoryQuota = MEMORY_TX_NANO_QUOTA_VALUE * instancesQuota;
		int diskQuota = DISK_DEFAULT_QUOTA_VALUE - VOLUME_SIZE_USED_VALUE * 2;

		// exercise
		ComputeQuota quota = this.plugin.getUserQuota(cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeInstances();
		Mockito.verify(client, Mockito.times(2)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));

		Assert.assertEquals(instancesQuota, quota.getAvailableQuota().getInstances());
		Assert.assertEquals(vcpuQuota, quota.getAvailableQuota().getvCPU());
		Assert.assertEquals(memoryQuota, quota.getAvailableQuota().getRam());
		Assert.assertEquals(diskQuota, quota.getAvailableQuota().getDisk());
	}

	// case test: When calling the doDescribeInstances method, and an error occurs
	// during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDescribeInstancesUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.describeInstances()).thenThrow(SdkClientException.builder().build());

		// exercise
		this.plugin.doDescribeInstances(client);
	}

	// case test: When calling the doDescribeVolumes method, and an error occurs
	// during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDescribeVolumesUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String volumeId = FAKE_VOLUME_ID;

		// exercise
		this.plugin.doDescribeVolumes(volumeId, client);
	}

	// test case: When calling the loadLinesFromFlavorFile method, without a valid
	// file path, the ConfigurationErrorException will be thrown.
	@Test(expected = ConfigurationErrorException.class) // verify
	public void testLoadLinesFromFlavorFileUnsuccessful() throws FogbowException {
		// set up
		Mockito.doReturn(ANY_VALUE).when(this.plugin).getFlavorsFilePath();

		// exercise
		this.plugin.loadLinesFromFlavorFile();
	}

	private Instance buildInstance(InstanceType instanceType) {
		EbsInstanceBlockDevice ebs = EbsInstanceBlockDevice.builder()
				.volumeId(FAKE_VOLUME_ID)
				.build();
		
		InstanceBlockDeviceMapping blockDeviceMapping = InstanceBlockDeviceMapping.builder()
				.ebs(ebs)
				.build();
		
		return Instance.builder()
				.instanceType(instanceType)
				.blockDeviceMappings(blockDeviceMapping)
				.build();
	}

	private void describeVolumeMocked(Ec2Client client) {
		Volume volume = Volume.builder()
				.volumeId(FAKE_VOLUME_ID)
				.size(VOLUME_SIZE_USED_VALUE)
				.build();
		
		DescribeVolumesResponse response = DescribeVolumesResponse.builder()
				.volumes(volume)
				.build();
		
		Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class))).thenReturn(response);
	}

	private void describeInstanceMocked(List<Instance> instances, Ec2Client client) {
		Reservation reservation;
		if (instances != null && !instances.isEmpty()) {
			reservation = Reservation.builder()
					.instances(instances)
					.build();
		} else {
			reservation = Reservation.builder()
					.build();
		}
		DescribeInstancesResponse response = DescribeInstancesResponse.builder()
				.reservations(reservation)
				.build();
		
		Mockito.when(client.describeInstances()).thenReturn(response);
	}

}
