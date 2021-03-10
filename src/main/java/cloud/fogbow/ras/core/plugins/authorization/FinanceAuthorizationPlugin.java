package cloud.fogbow.ras.core.plugins.authorization;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationClient;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationParameters;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class FinanceAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {
	private RemoteAuthorizationClient authorizationClient;
	
	public FinanceAuthorizationPlugin() throws URISyntaxException {
		String financeServiceAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_URL_KEY);
		String financeServicePort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_PORT_KEY);
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
					getRemoteAuthorizationParameters(systemUser.getId(), getOperationParameters(operation));
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

	private HashMap<String, String> getOperationParameters(RasOperation operation) {
		HashMap<String, String> operationParameters = new HashMap<String, String>();
		
		// FIXME constant
		operationParameters.put("operationType", operation.getOperationType().getValue());
		operationParameters.put("resourceType", operation.getResourceType().getValue());
		return operationParameters;
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
		private static final String USER_ID_KEY = "userId";
		private static final String OPERATION_PARAMETERS_KEY = "operationParameters";
		private String userId;
		private String operationParameters;
		
		// Maybe refactor this into a factory in the future
		static FinanceAuthorizationParameters getRemoteAuthorizationParameters(String userId, 
				Map<String, String> operationParameters) {
			return new FinanceAuthorizationParameters(userId, operationParameters);
		}
		
		public FinanceAuthorizationParameters(String userId, Map<String, String> operationParameters) {
			Gson gson = new Gson();
			this.userId = userId;
			this.operationParameters = gson.toJson(operationParameters);
		}

	    public Map<String, String> asRequestBody() {
	        HashMap<String, String> body = new HashMap<String, String>();
	        body.put(USER_ID_KEY, this.userId);
	        body.put(OPERATION_PARAMETERS_KEY, this.operationParameters);
	        return body;
	    }
	}
}
