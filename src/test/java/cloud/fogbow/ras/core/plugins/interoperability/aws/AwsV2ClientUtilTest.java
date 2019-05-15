package cloud.fogbow.ras.core.plugins.interoperability.aws;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsBasicCredentials.class, StaticCredentialsProvider.class, Ec2Client.class })
public class AwsV2ClientUtilTest {

	// test case: ...
	@Ignore
	@Test
	public void testcreateEc2ClientSuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		String tokenValue = "fake-access-key:fake-secret-key";
		String regionName = "sa-east-1";

		Ec2Client ec2client = Mockito.mock(Ec2Client.class);
		PowerMockito.mock(Ec2Client.class);
		Mockito.when(Ec2Client.builder().credentialsProvider(Mockito.any(StaticCredentialsProvider.class))
				.region(Mockito.eq(Region.AWS_GLOBAL)).build()).thenReturn(ec2client);

		// exercise
		Ec2Client client = AwsV2ClientUtil.createEc2Client(tokenValue, regionName);

		// verify
		Assert.assertNotNull(client);
	}
	
	// test case: ...
	@Test(expected = UnexpectedException.class) // verify
	public void testcreateEc2ClientUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		String tokenValue = "fake-access-key:fake-secret-key";
		String regionName = "sa-east-1";

		// exercise
		AwsV2ClientUtil.createEc2Client(tokenValue, regionName);
	}
	
	// test case: ...
	@Test (expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithANullRegionName() throws InvalidParameterException, UnexpectedException {
		// set up
		String regionName = null;

		// exercise
		AwsV2ClientUtil.validateRegion(regionName);
	}
	
	// test case: ...
	@Test(expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithAInconsistentRegionName() throws InvalidParameterException, UnexpectedException {
		// set up
		String regionName = "anything";

		// exercise
		AwsV2ClientUtil.validateRegion(regionName);
	}
	
	// test case: ...
	@Test(expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithAEmptyRegionName() throws InvalidParameterException, UnexpectedException {
		// set up
		String regionName = "";

		// exercise
		AwsV2ClientUtil.validateRegion(regionName);
	}
	
}
