package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;

public class OpenNebulaGenericRequestPlugin implements GenericRequestPlugin<CloudToken, OpenNebulaGenericRequest> {
	private OpenNebulaClientFactory factory;

	@Override
	public GenericRequestResponse redirectGenericRequest(OpenNebulaGenericRequest genericRequest, CloudToken token)
			throws FogbowException {
        OpenNebulaGenericRequest request = (OpenNebulaGenericRequest) genericRequest;

        if (request.getOneResource() == null || request.getMethod() == null) {
        	throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}

		Client client = this.factory.createClient(token.getTokenValue());
		OneResourse oneResourse = OneResourse.getValueOf(request.getOneResource());
		Class resourceClassType = oneResourse.getClassType();
		Object instance = null;

        if (request.getResourceId() != null) {
			instance = oneResourse.createInstance(Integer.parseInt(request.getResourceId()), client);
		}

		List classes = new ArrayList<>();
		List values = new ArrayList<>();

		for(Map.Entry<String, String> entry : request.getParams().entrySet()) {
			OneParameter oneParameter = OneParameter.getValueOf(entry.getKey());
			classes.add(oneParameter.getClassType());
			values.add(oneParameter.getValue(entry.getValue()));
		}

		Method method = OneGenericMethod.generate(resourceClassType, request.getMethod(), classes);
		OneResponse response = null;

		if (instance != null) {
			response = (OneResponse) OneGenericMethod.invoke(instance, method, values);
		} else {
			classes.add(0, Client.class);
			values.add(0, client);

			// Reassigning method with updated classes list.
			method = OneGenericMethod.generate(resourceClassType, request.getMethod(), classes);
			response = (OneResponse) OneGenericMethod.invoke(method, values);
		}

		return this.getOneGenericRequestResponse(response);
	}

	private OpenNebulaGenericRequestResponse getOneGenericRequestResponse(OneResponse oneResponse) {
	    String message = oneResponse.isError() ? oneResponse.getErrorMessage() : oneResponse.getMessage();

	    OpenNebulaGenericRequestResponse response = new OpenNebulaGenericRequestResponse(message, oneResponse.getErrorMessage(),
				oneResponse.getIntMessage(), oneResponse.isError(), oneResponse.getBooleanMessage());

		return response;
	}

}
