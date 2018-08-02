package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmpp.packet.IQ;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetAllImagesRequestHandlerTest {

    private static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n" +
            "  <query xmlns=\"remoteGetAllImages\">\n" +
            "    <imagesMap>{\"image-id1\":\"%s\",\"image-id2\":\"%s\"}</imagesMap>\n" +
            "    <imagesMapClassName>java.util.HashMap</imagesMapClassName>\n" +
            "  </query>\n" +
            "</iq>";

    private static final String TAG_RESULT_ERRO =
            "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n" +
                    "  <error code=\"500\" type=\"wait\">\n" +
                    "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n" +
                    "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Fogbow Manager exception</text>\n" +
                    "  </error>\n" +
                    "</iq>";

    private String IMAGE_NAME = "image-name";

    private RemoteGetAllImagesRequestHandler remoteGetAllImagesRequestHandler;
    private PacketSender packetSender;

    private Order order;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() throws InvalidParameterException {
        this.remoteGetAllImagesRequestHandler = new RemoteGetAllImagesRequestHandler();

        this.packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("user-name", "fogbow");

        FederationUser federationUser = new FederationUser("fake-id", attributes);

        this.order = new ComputeOrder();
        order.setProvidingMember("providingmember");
        order.setFederationUser(federationUser);
    }

    // test case: When call the handle method passing a valid IQ object, it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        //set up
        Map<String, String> images = new HashMap<>();
        images.put("image-id1", IMAGE_NAME.concat("1"));
        images.put("image-id2", IMAGE_NAME.concat("2"));

        Mockito.doReturn(images).when(this.remoteFacade).getAllImages(Mockito.anyString(), Mockito.any(FederationUser.class));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        //verify
        String iqId = iq.getID();
        String orderProvidingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_IQ, iqId, orderProvidingMember,
                IMAGE_NAME.concat("1"), IMAGE_NAME.concat("2"));
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        //set up
        Mockito.doThrow(new FogbowManagerException()).when(this.remoteFacade).getAllImages(
                Mockito.anyString(), Mockito.any(FederationUser.class));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        //verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).getAllImages(
                Mockito.anyString(), Mockito.any(FederationUser.class));

        String iqId = iq.getID();
        String orderProvidingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_ERRO, iqId, orderProvidingMember);
        Assert.assertEquals(expected, result.toString());
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.order.getProvidingMember());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(this.order.getProvidingMember());

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.order.getFederationUser()));

        return iq;
    }
}
