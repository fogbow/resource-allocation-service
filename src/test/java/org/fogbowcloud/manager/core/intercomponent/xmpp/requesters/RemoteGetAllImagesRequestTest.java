package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteGetAllImagesRequestTest {
	
 	private RemoteGetAllImagesRequest remoteGetAllImagesRequest;
	private PacketSender packetSender;
	private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
	private FederationUser federationUser;
	HashMap<String, String> imagesMap;
	private final String provider = "provider";
	
 	@Before
	public void setUp() throws InvalidParameterException {
 		Map<String, String> attributes = new HashMap<String, String>();
 		attributes.put("user-name", "user-name");
 		this.federationUser = new FederationUser("federation-user-id", attributes);
 		
		this.remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(provider, federationUser);
		this.packetSender = Mockito.mock(PacketSender.class);
		PacketSenderHolder.init(packetSender);
		
		this.imagesMap = new HashMap<String, String>();
		this.imagesMap.put("key-1", "value-1");
		this.imagesMap.put("key-2", "value-2");
		this.imagesMap.put("key-3", "value-3");
	}
 	
 	//test case: checks if IQ attributes is according to both RemoteGetAllImagesRequestTest constructor parameters 
 	//and remote get all images request rules. In addition, it checks if the image map from a possible response is 
 	//properly created and returned by the "send" method
	@Test
	public void testSend() throws Exception {
		//set up
		IQ iqResponse = getImagesMapIQResponse(this.imagesMap);
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());
 		String federationUserJson = new Gson().toJson(this.federationUser);

 		//exercise
		HashMap<String, String> responseImagesMap = this.remoteGetAllImagesRequest.send();
		
 		//verify
		IQ iq = argIQ.getValue();
		Assert.assertEquals(IQ.Type.get.toString(), iq.getType().toString());
		Assert.assertEquals(this.provider, iq.getTo().toString());
		
		Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
		Assert.assertEquals(RemoteMethod.REMOTE_GET_ALL_IMAGES.toString(), iqElementQuery.getNamespaceURI());
		
		String iqQueryMemberId = iqElementQuery.element(IqElement.MEMBER_ID.toString()).getText();
		Assert.assertEquals(this.provider, iqQueryMemberId);
		
		String iqQueryUser = iqElementQuery.element(IqElement.FEDERATION_USER.toString()).getText();
		Assert.assertEquals(federationUserJson, iqQueryUser);
		
		Assert.assertEquals(this.imagesMap, responseImagesMap);
	}
	
	//test case: checks if "send" is properly forwading UnavailableProviderException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
	@Test (expected = UnavailableProviderException.class)
	public void testSendWhenResponseIsNull() throws Exception {
		//set up
		Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());
 		
		//exercise/verify
		this.remoteGetAllImagesRequest.send();
	}
	
	//test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
	@Test (expected = UnauthorizedRequestException.class)
	public void testSendWhenResponseReturnsForbidden() throws Exception {
		//set up
		IQ iqResponse = new IQ();
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
		iqResponse.setError(new PacketError(PacketError.Condition.forbidden));
		
		//exercise/verify
		this.remoteGetAllImagesRequest.send();
	}
	
	//test case: checks if "send" is properly forwading UnexpectedException thrown by 
	//"getImageFromResponse" when the image class map name from the IQ response is undefined (wrong or not found)
	@Test(expected = UnexpectedException.class)
	public void testSendWhenImageClassIsUndefined() throws Exception {
		//set up
		IQ iqResponse = getImagesMapIQResponseWithWrongClass(this.imagesMap);
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());

 		//exercise/verify
		this.remoteGetAllImagesRequest.send();
	}
	
	private IQ getImagesMapIQResponse(Map<String, String> imagesMap) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(imagesMap.getClass().getName());

        imagesMapElement.setText(new Gson().toJson(imagesMap));
        return iqResponse;
	}
	
	private IQ getImagesMapIQResponseWithWrongClass(Map<String, String> imagesMap) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
        imagesMapClassNameElement.setText("wrong-class-name");

        imagesMapElement.setText(new Gson().toJson(imagesMap));
        return iqResponse;
	}
}
