package cloud.fogbow.ras.core.plugins.authorization;

import java.net.URISyntaxException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationClient;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationParameters;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class FinanceAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {
	private RemoteAuthorizationClient authorizationClient;
	private String financeServiceAddress;
	private String financeServicePort;
	
	public FinanceAuthorizationPlugin() throws URISyntaxException, FogbowException {
		this.financeServiceAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_URL_KEY);
		this.financeServicePort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_PORT_KEY);
		String authorizedEndpoint = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_AUTHORIZED_ENDPOINT);
		
		authorizationClient = new RemoteAuthorizationClient(financeServiceAddress, financeServicePort, authorizedEndpoint);
	}
	
	public FinanceAuthorizationPlugin(RemoteAuthorizationClient authorizationClient) {
		this.authorizationClient = authorizationClient;
	}
	
	@Override
	public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
		boolean authorized = false;

		try {
			FinanceAuthorizationParameters params = FinanceAuthorizationParameters.
					getRemoteAuthorizationParameters(systemUser, operation, this.financeServiceAddress, 
							this.financeServicePort);
			authorized = this.authorizationClient.doAuthorizationRequest(params);
		} catch (InternalServerErrorException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		} catch (URISyntaxException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		} catch (FogbowException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		}

		if (!authorized) {
			throw new UnauthorizedRequestException(Messages.Exception.USER_IS_NOT_FINANCIALLY_AUTHORIZED);
		}
			
		return authorized;
	}

	@Override
	public void setPolicy(String policy) throws ConfigurationErrorException {
		// Ignore
	}

	@Override
	public void updatePolicy(String policy) throws ConfigurationErrorException {
		// Ignore
	}
	
	static class FinanceAuthorizationParameters implements RemoteAuthorizationParameters {
		private static final String USER_TOKEN_KEY = "userToken";
		private static final String OPERATION_KEY = "operation";

		private SystemUser systemUser;
		private RasOperation operation;
		private RSAPublicKey fsPublickey;
		
		// Maybe refactor this into a factory in the future
		static FinanceAuthorizationParameters getRemoteAuthorizationParameters(SystemUser systemUser,
				RasOperation operation, String financeServiceAddress, String financeServicePort) throws FogbowException {
			return new FinanceAuthorizationParameters(systemUser, operation, financeServiceAddress, 
					financeServicePort);
		}

		public FinanceAuthorizationParameters(SystemUser systemUser, RasOperation operation, String financeServiceAddress, 
				String financeServicePort) throws FogbowException {
			String publicKeyEndpoint = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_PUBLIC_KEY_ENDPOINT_KEY);
			this.fsPublickey = PublicKeysHolder.getPublicKey(financeServiceAddress, financeServicePort, publicKeyEndpoint);
			
	    	this.systemUser = systemUser;
	    	this.operation = operation;
	    }
		
		public Map<String, String> asRequestBody() throws InternalServerErrorException {
			RSAPrivateKey rasPrivateKey = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
			String token = AuthenticationUtil.createFogbowToken(systemUser, rasPrivateKey, fsPublickey);
			
	        HashMap<String, String> body = new HashMap<String, String>();
	        body.put(USER_TOKEN_KEY, token);
	        body.put(OPERATION_KEY, new Gson().toJson(this.operation));
	        
	        return body;
	    }
	}
}
