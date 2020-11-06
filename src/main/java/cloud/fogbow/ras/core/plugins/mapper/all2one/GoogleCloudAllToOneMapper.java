package cloud.fogbow.ras.core.plugins.mapper.all2one;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.cloudidp.googlecloud.GoogleCloudIdentityProviderPlugin;

public class GoogleCloudAllToOneMapper extends GenericAllToOneSystemToCloudMapper<GoogleCloudUser, SystemUser> {

	private GoogleCloudIdentityProviderPlugin identityProviderPlugin;

	public GoogleCloudAllToOneMapper(String mapperConfFilePath) throws FatalErrorException {
		super(mapperConfFilePath);
		this.identityProviderPlugin = new GoogleCloudIdentityProviderPlugin();
	}

	public GoogleCloudUser getCloudUser(Map<String, String> credentials) throws FogbowException {
		String privateKeyPath = credentials.get(GoogleCloudConstants.Identity.PRIVATE_KEY_PATH);
		String privateKey = this.getPrivateKey(privateKeyPath);
		credentials.put(GoogleCloudConstants.Identity.PRIVATE_KEY, privateKey);
		return identityProviderPlugin.getCloudUser(credentials);
	}

	private String getPrivateKey(String path) throws FogbowException {
		try {
			FileInputStream in = new FileInputStream(path);
			String privateKey = "";
			int c;
			while ((c = in.read()) != -1) {
				privateKey += (char) c;
			}
			return privateKey;
		} catch (IOException e) {
			throw new FogbowException(e.getMessage());
		}
	}
}
