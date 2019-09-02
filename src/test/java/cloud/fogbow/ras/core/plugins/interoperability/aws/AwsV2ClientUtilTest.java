package cloud.fogbow.ras.core.plugins.interoperability.aws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Ec2Client.class })
public class AwsV2ClientUtilTest {

	private static final String ANY_VALUE = "anything";
	private static final String EAST_SOUTH_AMERICA_REGION = "sa-east-1";
	private static final String EMPTY_STRING = "";
	private static final String FAKE_TOKEN_VALUE = "fake-access-key:fake-secret-key";

	// test case: Verifies that the invocation of the build method to create a
	// client was called correctly.
	@Test
	public void testcreateEc2ClientSuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		Ec2ClientBuilder clientBuilder = Mockito.mock(Ec2ClientBuilder.class);

		PowerMockito.mockStatic(Ec2Client.class);
		PowerMockito.when(Ec2Client.builder()).thenReturn(clientBuilder);

		Mockito.when(clientBuilder.credentialsProvider(Mockito.any())).thenReturn(clientBuilder);
		Mockito.when(clientBuilder.region(Mockito.any())).thenReturn(clientBuilder);
		Mockito.when(clientBuilder.build()).thenReturn(client);

		String tokenValue = FAKE_TOKEN_VALUE;
		String regionName = EAST_SOUTH_AMERICA_REGION;

		// exercise
		AwsV2ClientUtil.createEc2Client(tokenValue, regionName);

		// verify
		PowerMockito.verifyStatic(Ec2Client.class, VerificationModeFactory.times(1));
		Ec2Client.builder();

		Mockito.verify(clientBuilder, Mockito.times(1)).credentialsProvider(Mockito.any());
		Mockito.verify(clientBuilder, Mockito.times(1)).region(Mockito.any());
		Mockito.verify(clientBuilder, Mockito.times(1)).build();
	}
	
	// test case: When calling the deleteInstance method, with an invalid token, an
	// UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testcreateEc2ClientUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		String tokenValue = FAKE_TOKEN_VALUE;
		String regionName = EAST_SOUTH_AMERICA_REGION;

		// exercise
		AwsV2ClientUtil.createEc2Client(tokenValue, regionName);
	}
	
	// test case: When calling the validateRegion method with a null region name, an
	// InvalidParameterException will be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithANullRegionName() throws InvalidParameterException {
		// set up
		String regionName = null;

		// exercise
		AwsV2ClientUtil.parseRegion(regionName);
	}
	
	// test case: When calling the validateRegion method with any value in region
	// name, an InvalidParameterException will be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithAInconsistentRegionName() throws InvalidParameterException {
		// set up
		String regionName = ANY_VALUE;

		// exercise
		AwsV2ClientUtil.parseRegion(regionName);
	}
	
	// test case: When calling the validateRegion method with an empty string in
	// region name, an InvalidParameterException will be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testValidateRegionWithAEmptyRegionName() throws InvalidParameterException {
		// set up
		String regionName = EMPTY_STRING;

		// exercise
		AwsV2ClientUtil.parseRegion(regionName);
	}
	
}
