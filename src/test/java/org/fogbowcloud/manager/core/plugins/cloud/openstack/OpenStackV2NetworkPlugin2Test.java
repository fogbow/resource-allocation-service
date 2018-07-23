package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackV2NetworkPlugin2Test {

	private static final String DEFAULT_TENANT_ID = "tenant_id";
	
	private HttpClient client;
	private Token defaultToken;
	private HttpRequestClientUtil httpRequestClientUtil;
	private OpenStackV2NetworkPlugin2 openStackV2NetworkPlugin2;
	
	@Before
	public void setUp() {
	    HomeDir.getInstance().setPath("src/test/resources/private");
		this.openStackV2NetworkPlugin2 = Mockito.spy(new OpenStackV2NetworkPlugin2());

		this.client = Mockito.mock(HttpClient.class);
		this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
		this.openStackV2NetworkPlugin2.setClient(this.httpRequestClientUtil);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(OpenStackV2NetworkPlugin2.TENANT_ID, DEFAULT_TENANT_ID);
		this.defaultToken = new Token("accessId", new Token.User("user", "user"), new Date(), attributes);
	}

	@After
	public void validate() {
		Mockito.validateMockitoUsage();
	}
	
	// test case: success case
	@Test 
	public void deleteInstanceTest() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		String networkId = "networkId";
		Token token = this.defaultToken;
		String sufixEndpointNetwork = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_NETWORK + 
				File.separator + networkId;
		String sufixEndpointSecurityGroup = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + 
				File.separator + networkId;
				
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(token));
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.endsWith(sufixEndpointSecurityGroup), Mockito.eq(token));
		
		// exercise
		this.openStackV2NetworkPlugin2.deleteInstance(networkId, token);
		
		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointSecurityGroup) , Mockito.eq(this.defaultToken));		
	}

	// test case: throws an exception when try to delete the network
	@Test(expected=FogbowManagerException.class)
	public void deleteInstanceExceptionNetworkDelectionTest() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		String networkId = "networkId";
		Token token = this.defaultToken;
		String sufixEndpointNetwork = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_NETWORK + 
				File.separator + networkId;
		String sufixEndpointSecurityGroup = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + 
				File.separator + networkId;
				
		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "")).when(this.httpRequestClientUtil)
			 	.doDeleteRequest(Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(token));
		
		// exercise
		this.openStackV2NetworkPlugin2.deleteInstance(networkId, token);
		
		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(0)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointSecurityGroup) , Mockito.eq(this.defaultToken));
	}
	
	// test case: throws an exception when try to delete the security group
	@Test(expected=FogbowManagerException.class)
	public void deleteInstanceExceptionSecurityGroupDelectionTest() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		String networkId = "networkId";
		Token token = this.defaultToken;
		String sufixEndpointNetwork = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_NETWORK + 
				File.separator + networkId;
		String sufixEndpointSecurityGroup = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + 
				File.separator + networkId;
				
		// network delection ok
		Mockito.doNothing().when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(token));
		// security group delection not ok		
		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "")).when(this.httpRequestClientUtil)
	 			.doDeleteRequest(Mockito.endsWith(sufixEndpointSecurityGroup), Mockito.eq(token));
		
		// exercise
		this.openStackV2NetworkPlugin2.deleteInstance(networkId, token);
		
		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointSecurityGroup) , Mockito.eq(this.defaultToken));		
	}	
	
	// test case: throws a "notFoundInstance" exception and continue try to delete the security group
	@Test
	public void deleteInstanceNotFoundNetworkExceptionTest() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		String networkId = "networkId";
		Token token = this.defaultToken;
		String sufixEndpointNetwork = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_NETWORK + 
				File.separator + networkId;
		String sufixEndpointSecurityGroup = OpenStackV2NetworkPlugin2.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + 
				File.separator + networkId;
				
		// network delection not ok and return nof found
		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, "")).when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(token));
		// security group delection ok		
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.endsWith(sufixEndpointSecurityGroup), Mockito.eq(token));
		
		// exercise
		this.openStackV2NetworkPlugin2.deleteInstance(networkId, token);
		
		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(sufixEndpointSecurityGroup) , Mockito.eq(this.defaultToken));		
	}		

}