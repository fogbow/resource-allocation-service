package cloud.fogbow.ras.core.plugins.mapper;

import static org.junit.Assert.assertEquals;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.internal.LinkedTreeMap;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;

@PrepareForTest({ HttpRequestClient.class, ServiceAsymmetricKeysHolder.class, 
    PublicKeysHolder.class, TokenProtector.class })
@RunWith(PowerMockRunner.class)
public class MapperClientTest {
    private static final String MAPPER_URL = "http://0.0.0.0";
    private static final String MAPPER_PORT = "8080";
    private static final String MAPPER_PUBLIC_KEY_SUFFIX = "publicKey";
    private static final String MAPPER_MAP_SUFFIX = "map";
    private static final String CLOUD_NAME = "cloudName";
    private static final String FEDERATION = "federation";
    private static final String BASE_ENDPOINT = MAPPER_URL + ":" + MAPPER_PORT + "/" + MAPPER_MAP_SUFFIX;
    private static final String REQUEST_ENDPOINT = BASE_ENDPOINT + "/" + FEDERATION + "/" + CLOUD_NAME;
    private static final String TOKEN = "token";
    private static final String REWRAP_TOKEN = "rewrapToken";
    private static final String CREDENTIAL_KEY_1 = "key1";
    private static final String CREDENTIAL_VALUE_1 = "value1";
    private static final String CREDENTIAL_KEY_2 = "key2";
    private static final String CREDENTIAL_VALUE_2 = "value2";
    private static final String RESPONSE_STRING = String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}", 
            CREDENTIAL_KEY_1, CREDENTIAL_VALUE_1, CREDENTIAL_KEY_2, CREDENTIAL_VALUE_2);
    
    private Map<String, String> headers;
    private Map<String, String> body;
    private LinkedTreeMap<String, String> credentials;
    private RSAPrivateKey privateKey;
    private RSAPublicKey mapperPublicKey;
    private HttpResponse response;
    private MapperClient mapperClient;
    
    @Before
    public void setUp() throws FogbowException {
        headers = new HashMap<String, String>();
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, REWRAP_TOKEN);
        
        body = new HashMap<String, String>();
        
        credentials = new LinkedTreeMap<>();
        credentials.put(CREDENTIAL_KEY_1, CREDENTIAL_VALUE_1);
        credentials.put(CREDENTIAL_KEY_2, CREDENTIAL_VALUE_2);
        
        privateKey = Mockito.mock(RSAPrivateKey.class);
        mapperPublicKey = Mockito.mock(RSAPublicKey.class);
        
        ServiceAsymmetricKeysHolder keysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.when(keysHolder.getPrivateKey()).thenReturn(privateKey);
        
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(keysHolder);
        
        PowerMockito.mockStatic(PublicKeysHolder.class);
        BDDMockito.given(PublicKeysHolder.getPublicKey(
                MAPPER_URL, MAPPER_PORT, MAPPER_PUBLIC_KEY_SUFFIX)).willReturn(mapperPublicKey);
        
        PowerMockito.mockStatic(TokenProtector.class);
        BDDMockito.given(TokenProtector.rewrap(privateKey, mapperPublicKey, TOKEN, 
                FogbowConstants.TOKEN_STRING_SEPARATOR)).willReturn(REWRAP_TOKEN);
        
        response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getHttpCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(response.getContent()).thenReturn(RESPONSE_STRING);
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(
                HttpMethod.GET, REQUEST_ENDPOINT, headers, body)).willReturn(response);
    }
    
    @Test
    public void testGetCredentials() throws FogbowException {
        mapperClient = new MapperClient(MAPPER_URL, MAPPER_PORT, MAPPER_PUBLIC_KEY_SUFFIX, MAPPER_MAP_SUFFIX, CLOUD_NAME);
        
        HashMap<String, String> returnedCredentials = mapperClient.getCredentials(TOKEN, FEDERATION);
        
        assertEquals(2, returnedCredentials.size());
        assertEquals(CREDENTIAL_VALUE_1, returnedCredentials.get(CREDENTIAL_KEY_1));
        assertEquals(CREDENTIAL_VALUE_2, returnedCredentials.get(CREDENTIAL_KEY_2));
    }
    
    @Test(expected = UnavailableProviderException.class)
    public void testGetCredentialsErrorCode() throws FogbowException {
        response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getHttpCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Mockito.when(response.getContent()).thenReturn("");
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(
                HttpMethod.GET, REQUEST_ENDPOINT, headers, body)).willReturn(response);
        
        mapperClient = new MapperClient(MAPPER_URL, MAPPER_PORT, MAPPER_PUBLIC_KEY_SUFFIX, MAPPER_MAP_SUFFIX, CLOUD_NAME);
        
        mapperClient.getCredentials(TOKEN, FEDERATION);
    }
}
