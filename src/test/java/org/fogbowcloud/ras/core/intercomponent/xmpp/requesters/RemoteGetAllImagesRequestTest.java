package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IQMatcher;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.HashMap;
import java.util.Map;

public class RemoteGetAllImagesRequestTest {

    private final String PROVIDER = "provider";

    private PacketSender packetSender;
    private RemoteGetAllImagesRequest remoteGetAllImagesRequest;

    private HashMap<String, String> imagesMap;
    private FederationUserToken federationUserToken;

    private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);

    @Before
    public void setUp() throws InvalidParameterException {
        this.federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");


        this.remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(PROVIDER, federationUserToken);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.init(packetSender);

        this.imagesMap = new HashMap<String, String>();
        this.imagesMap.put("key-1", "value-1");
        this.imagesMap.put("key-2", "value-2");
        this.imagesMap.put("key-3", "value-3");
    }

    // test case: checks if IQ attributes is according to both RemoteGetAllImagesRequestTest constructor parameters
    // and remote get all images request rules. In addition, it checks if the image map from a possible response is
    // properly created and returned by the "send" method
    @Test
    public void testSend() throws Exception {
        // set up
        IQ response = getImagesResponse(this.imagesMap, this.imagesMap.getClass().getName());
        Mockito.doReturn(response).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteGetAllImagesRequest.send();

        // verify
        // as IQ does not implement equals we need a matcher
        IQ expectedIQ = RemoteGetAllImagesRequest.marshal(PROVIDER, federationUserToken);
        IQMatcher matcher = new IQMatcher(expectedIQ);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
    }

    // test case: checks if "send" is properly forwading UnavailableProviderException thrown by
    // "XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        // set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    // test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by
    // "XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        // set up
        IQ iqResponse = new IQ();
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));
        iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    // test case: checks if "send" is properly forwading UnexpectedException thrown by
    // "getImageFromResponse" when the images map class name from the IQ response is undefined (wrong or not found)
    @Test(expected = UnexpectedException.class)
    public void testSendWhenImageClassIsUndefined() throws Exception {
        // set up
        IQ iqResponse = getImagesMapIQResponseWithWrongClass(this.imagesMap);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    private IQ getImagesResponse(Map<String, String> imagesMap,
                                 String className) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGES_MAP.toString());

        Element imagesMapClassNameElement = queryEl
                .addElement(IqElement.IMAGES_MAP_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(className);

        imagesMapElement.setText(new Gson().toJson(imagesMap));
        return iqResponse;
    }

    private IQ getImagesMapIQResponseWithWrongClass(Map<String, String> imagesMap) {
        return getImagesResponse(imagesMap, "wrongClassName");
    }
}
