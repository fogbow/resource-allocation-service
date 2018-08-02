package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import java.util.HashMap;
import java.util.Map;
 import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.images.Image;
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
 

public class RemoteGetImageRequestTest {

 	private RemoteGetImageRequest remoteGetImageRequest;
	private PacketSender packetSender;
	private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
	private FederationUser federationUser;
	
	private final String provider = "provider";
	private final String imageId = "imageId";
	
 	@Before
	public void setUp() throws InvalidParameterException {
 		Map<String, String> attributes = new HashMap<String, String>();
 		attributes.put("user-name", "user-name");
 		this.federationUser = new FederationUser("federation-user-id", attributes);
 		
		this.remoteGetImageRequest = new RemoteGetImageRequest(provider, imageId, federationUser);
		this.packetSender = Mockito.mock(PacketSender.class);
		PacketSenderHolder.init(packetSender);
	}
 	
 	//test case: checks if IQ attributes is according to both RemoteGetImageRequest constructor parameters 
 	//and remote get image request rules. In addition, it checks if the image from a possible response is 
 	//properly created and returned by the "send" method
	@Test
	public void testSend() throws Exception {
		//set up
		Image image = new Image("image-id", "image-name", 10, 20, 30, "status");
		IQ iqResponse = getImageIqResponse(image);
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());
 		String federationUserJson = new Gson().toJson(this.federationUser);

 		//exercise
		Image responseImage = this.remoteGetImageRequest.send();
		
 		//verify
		IQ iq = argIQ.getValue();
		Assert.assertEquals(IQ.Type.get.toString(), iq.getType().toString());
		Assert.assertEquals(this.provider, iq.getTo().toString());
		
		Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
		Assert.assertEquals(RemoteMethod.REMOTE_GET_IMAGE.toString(), iqElementQuery.getNamespaceURI());
		
		String iqQueryMemberId = iqElementQuery.element(IqElement.MEMBER_ID.toString()).getText();
		Assert.assertEquals(this.provider, iqQueryMemberId);
		
		String iqQueryImageId = iqElementQuery.element(IqElement.IMAGE_ID.toString()).getText();
		Assert.assertEquals(this.imageId, iqQueryImageId);
		
		String iqQueryUser = iqElementQuery.element(IqElement.FEDERATION_USER.toString()).getText();
		Assert.assertEquals(federationUserJson, iqQueryUser);
		
		Assert.assertEquals(image, responseImage);
	}
	
	//test case: Check if "send" is properly forwading UnavailableProviderException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
	@Test (expected = UnavailableProviderException.class)
	public void testSendWhenResponseIsNull() throws Exception {
		//set up
		Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());
 		
		//exercise/verify
		this.remoteGetImageRequest.send();
	}
	
	//test case: Check if "send" is properly forwading UnauthorizedRequestException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
	@Test (expected = UnauthorizedRequestException.class)
	public void testSendWhenResponseReturnsForbidden() throws Exception {
		//set up
		IQ iqResponse = new IQ();
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
		iqResponse.setError(new PacketError(PacketError.Condition.forbidden));
		
		//exercise/verify
		this.remoteGetImageRequest.send();
	}
	
	private IQ getImageIqResponse(Image image) {
		IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_IMAGE.toString());
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());
        Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText(image.getClass().getName());
        imageElement.setText(new Gson().toJson(image));
		return iqResponse;
	}
}
