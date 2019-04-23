package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

public class OpenNebulaGenericRequestPlugin implements GenericRequestPlugin<OpenNebulaUser> {
	
	private static final String RESOURCE_POOL_SUFFIX = "Pool";
	protected static final String URL_REGEX_EXPRESSION = "^(https?://)?([\\w]+.)+(:2633/RPC2)$";

	@Override
	public FogbowGenericResponse redirectGenericRequest(String genericRequest, OpenNebulaUser cloudUser)
			throws FogbowException {

		CreateOneGenericRequest oneGenericRequest = CreateOneGenericRequest.fromJson(genericRequest);
		if (oneGenericRequest.getRequest() == null) {
			throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
		}
		if (!isValidUrl(oneGenericRequest.getUrl())) {
			throw new InvalidParameterException(
					String.format(Messages.Exception.INVALID_URL_S, oneGenericRequest.getUrl()));
		}
		if (!isValidResource(oneGenericRequest.getResource())) {
			throw new InvalidParameterException(
					String.format(Messages.Exception.INVALID_ONE_RESOURCE_S, oneGenericRequest.getResource()));
		}
		if (!isValid(oneGenericRequest.getMethod())) {
			throw new InvalidParameterException(
					String.format(Messages.Exception.INVALID_ONE_METHOD_S, oneGenericRequest.getMethod()));
		}
		Client client = OpenNebulaClientUtil.createClient(oneGenericRequest.getUrl(), cloudUser.getToken());
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
			return new FogbowGenericResponse(message);
		} else {
			message = GsonHolder.getInstance().toJson(response);
			return new FogbowGenericResponse(message);
		}
	}
	
	protected Object invokeGenericMethod(Object instance, Parameters parameters, Method method)
			throws InvalidParameterException, UnexpectedException {
		
		return OneGenericMethod.invoke(instance, method, parameters.getValues());
	}

	protected Method generateMethod(CreateOneGenericRequest request, Parameters parameters) throws UnexpectedException {
		OneResource oneResource = OneResource.getValueOf(request.getResource());
		Class resourceClassType = oneResource.getClassType();
		return OneGenericMethod.generate(resourceClassType, request.getMethod(), parameters.getClasses());
	}

	protected Parameters generateParametersMap(CreateOneGenericRequest request, Object instance, Client client)
			throws InvalidParameterException {

		Parameters parameters = new Parameters();
		if (request.getParameters() != null) {
			if (!request.getResource().endsWith(RESOURCE_POOL_SUFFIX) && !request.getParameters().isEmpty()
					&& instance == null) {

				parameters.getClasses().add(Client.class);
				parameters.getValues().add(client);
			}
			for (Map.Entry<String, String> entries : request.getParameters().entrySet()) {
				if (!isValidParameter(entries.getKey())) {
					throw new InvalidParameterException(
							String.format(Messages.Exception.INVALID_PARAMETER_S, entries.getKey()));
				}
				OneParameter oneParameter = OneParameter.getValueOf(entries.getKey());
				parameters.getClasses().add(oneParameter.getClassType());
				parameters.getValues().add(oneParameter.getValue(entries.getValue()));
			}
		}
		return parameters;
	}

	protected Object instantiateResource(CreateOneGenericRequest request, Client client, String secret)
			throws InvalidParameterException {

		OneResource oneResource = OneResource.getValueOf(request.getResource());
		Object instance = null;
		if (oneResource.equals(OneResource.CLIENT)) {
			instance = (Client) oneResource.createInstance(secret, request.getUrl());
		} else if (request.getResource().endsWith(RESOURCE_POOL_SUFFIX)) {
			instance = oneResource.createInstance(client);
		} else if (request.getResourceId() != null) {
			if (!request.getResourceId().isEmpty()) {
				int id = convertToInteger(request.getResourceId());
				instance = oneResource.createInstance(id, client);
			}
		}
		return instance;
	}

	protected Integer convertToInteger(String arg) throws InvalidParameterException {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(String.format(Messages.Exception.INVALID_RESOURCE_ID_S, arg), e);
		}
	}
	
	protected boolean isValidParameter(String parameter) {
		if (isValid(parameter)) {
			for (OneParameter element : OneParameter.values()) {
				if (element.getName().equals(parameter)) {
					return true;
				}
			}
		}		
		return false;
	}
	
	protected boolean isValidResource(String resource) {
		if (isValid(resource)) {
			for (OneResource element : OneResource.values()) {
				if (element.getName().equals(resource)) {
					return true;
				}
			}
		}		
		return false;
	}

	protected boolean isValidUrl(String url) {
		if (isValid(url)) {
			Pattern pattern = Pattern.compile(URL_REGEX_EXPRESSION);
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
	    }
		return false;
	}

	protected boolean isValid(String arg) {
		if (arg == null || arg.isEmpty()) {
			return false;
		}
		return true;
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
