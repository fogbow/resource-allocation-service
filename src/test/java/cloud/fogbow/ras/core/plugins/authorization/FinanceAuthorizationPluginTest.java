package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.RemoteAuthorizationClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.authorization.FinanceAuthorizationPlugin.FinanceAuthorizationParameters;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FinanceAuthorizationParameters.class})
public class FinanceAuthorizationPluginTest {

	private RasOperation operation;
	private SystemUser user;
	private RemoteAuthorizationClient authorizationClient;
	private String userId = "userId";
	private String userName = "userName";
	private String providerId = "provider";
	private FinanceAuthorizationParameters financeAuthorizationParameters;
	private String financeServiceAddress;
	private String financeServicePort;
	
	// test case: When calling the method isAuthorized, it must create the parameters
	// correctly and call the RemoteAuthorizationClient to perform the request
	// to the remote service. Also, if the returned value by the RemoteAuthorizationClient
	// is true, then the method must also return true.
	@Test
	public void testIsAuthorized() throws URISyntaxException, FogbowException {
		setUpUserIsAuthorized();
		
		FinanceAuthorizationPlugin plugin = new FinanceAuthorizationPlugin(authorizationClient);
		
		assertTrue(plugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the method isAuthorized, it must create the parameters
	// correctly and call the RemoteAuthorizationClient to perform the request
	// to the remote service. Also, if the returned value by the RemoteAuthorizationClient
	// is false, then the method must throw an UnauthorizedRequestException. 
	@Test(expected = UnauthorizedRequestException.class)
	public void testIsAuthorizedUserIsNotAuthorized() throws URISyntaxException, FogbowException {
		setUpUserIsNotAuthorized();
		
		FinanceAuthorizationPlugin plugin = new FinanceAuthorizationPlugin(authorizationClient);
		
		plugin.isAuthorized(user, operation);
	}
	
	// test case: When calling the method isAuthorized, it must create the parameters
	// correctly and call the RemoteAuthorizationClient to perform the request
	// to the remote service. Also, if the RemoteAuthorizationClient throws an
	// InternalServerErrorException, the method must catch the exception and 
	// throw an UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class)
	public void testIsAuthorizedRequestThrowsInternalServerError() throws URISyntaxException, FogbowException {
		setUpAuthorizationError(new InternalServerErrorException());
		
		FinanceAuthorizationPlugin plugin = new FinanceAuthorizationPlugin(authorizationClient);
		
		plugin.isAuthorized(user, operation);
	}

	// test case: When calling the method isAuthorized, it must create the parameters
	// correctly and call the RemoteAuthorizationClient to perform the request
	// to the remote service. Also, if the RemoteAuthorizationClient throws a
	// FogbowException, the method must catch the exception and throw an UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class)
	public void testIsAuthorizedRequestThrowsFogbowException() throws URISyntaxException, FogbowException {
		setUpAuthorizationError(new FogbowException("message"));
		
		FinanceAuthorizationPlugin plugin = new FinanceAuthorizationPlugin(authorizationClient);
		
		plugin.isAuthorized(user, operation);
	}
	
	private void setUpUserIsAuthorized() throws URISyntaxException, FogbowException {
		user = new SystemUser(userId, userName, providerId);
		operation = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, providerId, userId);
		setUpRequestParameters();
		setUpResponse(true);
	}
	
	private void setUpUserIsNotAuthorized() throws URISyntaxException, FogbowException {
		user = new SystemUser(userId, userName, providerId);
		operation = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, providerId, userId);
		setUpRequestParameters();
		setUpResponse(false);
	}
	
	private void setUpAuthorizationError(Exception exception) throws URISyntaxException, FogbowException {
		user = new SystemUser(userId, userName, providerId);
		operation = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, providerId, userId);
		setUpRequestParameters();
		setUpErrorResponse(exception);
	}

	private void setUpRequestParameters() throws URISyntaxException, FogbowException {
		financeAuthorizationParameters = Mockito.mock(FinanceAuthorizationParameters.class);
		
		PowerMockito.mockStatic(FinanceAuthorizationParameters.class);
		BDDMockito.given(FinanceAuthorizationParameters.getRemoteAuthorizationParameters(user, operation, 
				financeServiceAddress, financeServicePort)).
		willReturn(financeAuthorizationParameters);
	}
	
	private void setUpResponse(boolean authorized) throws URISyntaxException, FogbowException {
		authorizationClient = Mockito.mock(RemoteAuthorizationClient.class);
		Mockito.when(authorizationClient.doAuthorizationRequest(financeAuthorizationParameters)).thenReturn(authorized);		
	}
	
	private void setUpErrorResponse(Exception exception) throws URISyntaxException, FogbowException {
		authorizationClient = Mockito.mock(RemoteAuthorizationClient.class);
		Mockito.when(authorizationClient.doAuthorizationRequest(financeAuthorizationParameters)).thenThrow(exception);
	}
}
