package org.fogbowcloud.ras.core.models.tokens;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.ShibbolethTokenGenerator;
import org.json.JSONException;
import org.json.JSONObject;

// TODO implement test
// TODO logs
public class ShibbolethTokenHolder {

	private static final String SHIBBOLETH_SEPARETOR = ShibbolethTokenGenerator.SHIBBOLETH_SEPARETOR;

	public static final int PARAMETERS_SIZE_TOKEN_VALUE = 7;

	public static final int STR_TOKEN_VALUE_INDEX = 0;
	public static final int TOKEN_PROVIDER_TOKEN_VALUE_INDEX = 1;
	public static final int USER_ID_TOKEN_VALUE_INDEX = 2;
	public static final int USER_NAME_TOKEN_VALUE_INDEX = 3;
	public static final int SAML_ATTRIBUTES_TOKEN_VALUE_INDEX = 4;
	public static final int EXPIRATION_TIME_TOKEN_VALUE_INDEX = 5;
	public static final int SIGNATURE_TOKEN_VALUE_INDEX = 6;

	public static String createRawToken(String tokenValue, String tokenProvider, String userId, String userName,
			String samlAttributes, String expirationTime) {

		int parametersWithoutSignature = PARAMETERS_SIZE_TOKEN_VALUE - 1;
		String[] parameters = new String[parametersWithoutSignature];
		parameters[STR_TOKEN_VALUE_INDEX] = tokenValue;
		parameters[TOKEN_PROVIDER_TOKEN_VALUE_INDEX] = tokenProvider;
		parameters[USER_ID_TOKEN_VALUE_INDEX] = userId;
		parameters[USER_NAME_TOKEN_VALUE_INDEX] = userName;
		parameters[SAML_ATTRIBUTES_TOKEN_VALUE_INDEX] = samlAttributes;
		parameters[EXPIRATION_TIME_TOKEN_VALUE_INDEX] = expirationTime;

		return StringUtils.join(parameters, SHIBBOLETH_SEPARETOR);
	}

	public static ShibbolethToken createShibbolethToken(String tokenValue) throws CreateTokenException {
		String tokenValueSlices[] = tokenValue.split(SHIBBOLETH_SEPARETOR);
		return createShibbolethToken(tokenValueSlices);
	}

	protected static ShibbolethToken createShibbolethToken(String[] tokenValueSlices) throws CreateTokenException {
		String tokenValue = tokenValueSlices[STR_TOKEN_VALUE_INDEX];
		String tokenProvider = tokenValueSlices[TOKEN_PROVIDER_TOKEN_VALUE_INDEX];
		String userId = tokenValueSlices[USER_ID_TOKEN_VALUE_INDEX];
		String userName = tokenValueSlices[USER_NAME_TOKEN_VALUE_INDEX];
		String samlAttrSttr = tokenValueSlices[SAML_ATTRIBUTES_TOKEN_VALUE_INDEX];
		Map<String, String> samlAttributes = getSamlAttributes(samlAttrSttr);
		long expirationTime = getExpirationTime(tokenValueSlices);
		String signature = tokenValueSlices[SIGNATURE_TOKEN_VALUE_INDEX];
		return new ShibbolethToken(tokenProvider, tokenValue, userId, userName, samlAttributes, expirationTime,
				signature);
	}

	public static String normalizeSamlAttribute(Map<String, String> samlAttributes) {
		JSONObject samlAttributesJsonObject = new JSONObject(samlAttributes);
		return samlAttributesJsonObject.toString();
	}
	
	public static String normalizeExpirationTime(long expirationTime) {
		return String.valueOf(expirationTime);
	}
	
	public static String generateTokenValue(String rawToken, String rawTokenSignature) {
		String[] parameters = new String[] {
				rawToken,
				rawTokenSignature
		};
		return StringUtils.join(parameters, SHIBBOLETH_SEPARETOR);
	}
	
	protected static Map<String, String> getSamlAttributes(String samlAttrSttr) throws CreateTokenException {
		try {
			return toMap(new JSONObject(samlAttrSttr));
		} catch (Exception e) {
			throw new CreateTokenException(e.getMessage());
		}
	}

	protected static long getExpirationTime(String[] tokenValueSlices) throws CreateTokenException {
		try {
			String expirationTimeStr = tokenValueSlices[EXPIRATION_TIME_TOKEN_VALUE_INDEX];
			return Long.parseLong(expirationTimeStr);
		} catch (Exception e) {
			throw new CreateTokenException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> toMap(JSONObject jsonObject) throws JSONException {
		Map<String, String> map = new HashMap<String, String>();

		Iterator<String> keysItr = jsonObject.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			String value = jsonObject.optString(key);

			map.put(key, value);
		}
		return map;
	}
	
	public static class CreateTokenException extends Exception {
	    private static final long serialVersionUID = 1L;

	    // TODO use Message class
	    public CreateTokenException(String errorMsg) {
	        super("Is not possible create the Shibboleth token: " + errorMsg);
	    }

	}

}