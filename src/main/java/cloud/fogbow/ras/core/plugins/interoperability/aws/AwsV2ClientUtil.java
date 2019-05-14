package cloud.fogbow.ras.core.plugins.interoperability.aws;

import org.apache.log4j.Logger;

import cloud.fogbow.common.constants.AwsConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class AwsV2ClientUtil {

	private static final Logger LOGGER = Logger.getLogger(AwsV2ClientUtil.class);
	private static final int ACCESS_KEY_ID_TOKEN_INDEX = 0;
	private static final int SECRET_KEY_ID_TOKEN_INDEX = 1;

	public static Ec2Client createEc2Client(String tokenValue, String selectedRegion)
			throws InvalidParameterException, UnexpectedException {

		Region region = Region.of(selectedRegion);
		if (!Region.regions().contains(region)) {
			throw new InvalidParameterException();
		}

		String[] token = tokenValue.split(AwsConstants.TOKEN_VALUE_SEPARATOR);
		String accessKeyId = token[ACCESS_KEY_ID_TOKEN_INDEX];
		String secretKeyId = token[SECRET_KEY_ID_TOKEN_INDEX];

		Ec2Client client;
		try {
			AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretKeyId);
			client = Ec2Client.builder()
					.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
					.region(region)
					.build();

			return client;
		} catch (SdkClientException e) {
			LOGGER.error(Messages.Error.ERROR_WHILE_CREATING_CLIENT, e);
			throw new UnexpectedException();
		}
	}
}
