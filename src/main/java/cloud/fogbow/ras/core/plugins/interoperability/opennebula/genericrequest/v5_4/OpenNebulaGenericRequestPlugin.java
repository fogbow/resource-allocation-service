package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

public class OpenNebulaGenericRequestPlugin implements GenericRequestPlugin<OpenNebulaUser> {
	
	private static final String RESOURCE_POOL_SUFFIX = "Pool";

	@Override
	public FogbowGenericResponse redirectGenericRequest(String genericRequest, OpenNebulaUser cloudUser)
			throws FogbowException {
		
		OneFogbowGenericRequest oneGenericRequest = OneFogbowGenericRequest.fromJson(genericRequest);
		Client client = OpenNebulaClientUtil.createClient(oneGenericRequest.getUrl(), cloudUser.getToken());

		if (oneGenericRequest.getResource() == null || oneGenericRequest.getMethod() == null) {
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		
		Object instance = instantiateResource(oneGenericRequest, client, cloudUser.getToken());
		Parameters parameters = generateParametersMap(oneGenericRequest, instance, client);
		Method method = generateMethod(oneGenericRequest, parameters);
		Object response = invokeGenericMethod(instance, parameters, method);
		return reproduceMessage(response);
	}
	
	protected FogbowGenericResponse reproduceMessage(Object response) {
		String message;
		if (response instanceof OneResponse) {
			OneResponse oneResponse = (OneResponse) response;
			boolean isError = oneResponse.isError();
			message = isError ? oneResponse.getErrorMessage() : oneResponse.getMessage();
			return new OpenNebulaResponse(message, isError);
		} else {
			message = GsonHolder.getInstance().toJson(response);
			return new OpenNebulaResponse(message, false);
		}
	}
	
	protected Object invokeGenericMethod(Object instance, Parameters parameters, Method method)
			throws InvalidParameterException {
		
		Object response = OneGenericMethod.invoke(instance, method, parameters.getValues());
		return response;
	}

	protected Method generateMethod(OneFogbowGenericRequest request, Parameters parameters) {
		
		OneResource oneResource = OneResource.getValueOf(request.getResource());
		Class resourceClassType = oneResource.getClassType();
		// FIXME treat exception here ...
		Method method = OneGenericMethod.generate(resourceClassType, request.getMethod(), parameters.getClasses());
		return method;
	}

	protected Parameters generateParametersMap(OneFogbowGenericRequest request, Object instance, Client client) {
		Parameters parameters = new Parameters();
		if (!request.getResource().endsWith(RESOURCE_POOL_SUFFIX) && !request.getParameters().isEmpty()
				&& instance == null) {
			
			parameters.getClasses().add(Client.class);
			parameters.getValues().add(client);
		}
		for (Map.Entry<String, String> entries : request.getParameters().entrySet()) {
			OneParameter oneParameter = OneParameter.getValueOf(entries.getKey());
			parameters.getClasses().add(oneParameter.getClassType());
			parameters.getValues().add(oneParameter.getValue(entries.getValue()));
		}
		return parameters;
	}

	protected Object instantiateResource(OneFogbowGenericRequest request, Client client, String secret)
			throws InvalidParameterException {

		OneResource oneResource = OneResource.getValueOf(request.getResource());
		Object instance = null;
		if (oneResource.equals(OneResource.CLIENT)) {
			instance = (Client) oneResource.createInstance(secret, request.getUrl());
		} else if (request.getResource().endsWith(RESOURCE_POOL_SUFFIX)) {
			instance = oneResource.createInstance(client);
		} else if (request.getResourceId() != null) {
			if (!request.getResourceId().isEmpty()) {
				int id = parseToInteger(request.getResourceId());
				instance = oneResource.createInstance(id, client);
			}
		}
		return instance;
	}

	protected Integer parseToInteger(String arg) throws InvalidParameterException {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
	}
	
	protected static class Parameters {
		private List<Class> classes;
		private List<Object> values;
		
		public Parameters() {
			this.classes = new ArrayList<>();
			this.values = new ArrayList<>();
		}
		
		public List<Class> getClasses() {
			return classes;
		}

		public List<Object> getValues() {
			return values;
		}
	}
}
