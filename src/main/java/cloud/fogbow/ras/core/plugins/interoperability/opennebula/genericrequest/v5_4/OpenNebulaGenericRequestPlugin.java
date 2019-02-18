package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenNebulaGenericRequestPlugin implements GenericRequestPlugin<CloudToken> {
	private OpenNebulaClientFactory factory;

	public OpenNebulaGenericRequestPlugin(String confFilePath) {
		this.factory = new OpenNebulaClientFactory(confFilePath);
	}

	@Override
	public GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token)
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
			response = (OneResponse) OneGenericMethod.invoke(method, values);
		}

		return this.getOneGenericRequestResponse(response);
	}

	private OpenNebulaGenericRequestResponse getOneGenericRequestResponse(OneResponse oneResponse) {
		return null;
	}

}
