package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaResponse;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaFogbowGenericRequest;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

public class OpenNebulaGenericRequestPlugin implements GenericRequestPlugin<OpenNebulaFogbowGenericRequest, OpenNebulaUser> {
	
	private static final String RESOURCE_POOL_SUFFIX = "Pool";

	@Override
	public FogbowGenericResponse redirectGenericRequest(OpenNebulaFogbowGenericRequest genericRequest, OpenNebulaUser cloudUser)
			throws FogbowException {
        
		Client client = OpenNebulaClientUtil.createClient(genericRequest.getUrl(), cloudUser.getToken());

		OpenNebulaFogbowGenericRequest request = (OpenNebulaFogbowGenericRequest) genericRequest;
		if (request.getOneResource() == null || request.getOneMethod() == null) {
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		
		// Generate class type of resource
		OneResource oneResource = OneResource.getValueOf(request.getOneResource());
		Class resourceClassType = oneResource.getClassType();

		// Instantiate a resource
		Object instance = null;
		if (request.getOneResource().endsWith(RESOURCE_POOL_SUFFIX)) {
			instance = oneResource.createInstance(client);
		} else if (request.getResourceId() != null) {
			instance = oneResource.createInstance(Integer.parseInt(request.getResourceId()), client);
		} else if (oneResource.equals(OneResource.CLIENT)) {
			instance = (Client) oneResource.createInstance(cloudUser.getToken(), request.getUrl());
		}
		
		// Working with map of parameters
        List classes = new ArrayList<>();
		List values = new ArrayList<>();
		if (request.getResourceId() != null 
				&& !request.getOneResource().endsWith(RESOURCE_POOL_SUFFIX) 
				&& !request.getParameters().isEmpty()) {
			
			classes.add(Client.class);
			values.add(client);
		}
		for (Map.Entry<String, String> entries : request.getParameters().entrySet()) {
			OneParameter oneParameter = OneParameter.getValueOf(entries.getKey());
			classes.add(oneParameter.getClassType());
			values.add(oneParameter.getValue(entries.getValue()));
		}
		
		// Generate generic method
		Method method = OneGenericMethod.generate(resourceClassType, request.getOneMethod(), classes);
		
		// Invoke generic method
		Object object = OneGenericMethod.invoke(instance, method, values);
		
		// Requesting a response
		String message;
		if (object instanceof OneResponse) {
			OneResponse oneResponse = (OneResponse) object;
			boolean isError = oneResponse.isError();
			message = isError ? oneResponse.getErrorMessage() : oneResponse.getMessage();
			return new OpenNebulaResponse(message, isError);
		} else {
			message = GsonHolder.getInstance().toJson(object);
			return new OpenNebulaResponse(message, false);
		}
	}
}
