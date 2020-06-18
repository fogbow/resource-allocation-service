package cloud.fogbow.ras.core.plugins.interoperability.aws;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import org.apache.log4j.Logger;

import cloud.fogbow.common.constants.AwsConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class AwsV2ClientUtil {

	private static final Logger LOGGER = Logger.getLogger(AwsV2ClientUtil.class);
	private static final int ACCESS_KEY_ID_TOKEN_INDEX = 0;
	private static final int SECRET_KEY_ID_TOKEN_INDEX = 1;

	public static Ec2Client createEc2Client(String tokenValue, String regionName)
			throws InvalidParameterException, InternalServerErrorException {

		String[] token = tokenValue.split(AwsConstants.TOKEN_VALUE_SEPARATOR);
		String accessKeyId = token[ACCESS_KEY_ID_TOKEN_INDEX];
		String secretKeyId = token[SECRET_KEY_ID_TOKEN_INDEX];
    
		Region region = parseRegion(regionName);

		Ec2Client client;
		try {
			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretKeyId);
			StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);
			client = Ec2Client.builder()
					.credentialsProvider(credentialsProvider)
					.region(region)
					.build();

			return client;
		} catch (Throwable e) {
			LOGGER.error(Messages.Log.ERROR_WHILE_CREATING_CLIENT, e);
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	protected static Region parseRegion(String regionName) throws InvalidParameterException {
		Region region;
		if (regionName != null && !regionName.isEmpty()) {
			region = Region.of(regionName);
			if (Region.regions().contains(region)) {
				return region;
			}
		}
		throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, regionName));
	}

}
