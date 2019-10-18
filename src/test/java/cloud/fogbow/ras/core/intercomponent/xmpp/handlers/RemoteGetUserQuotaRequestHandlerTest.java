package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetUserQuotaRequest;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
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

import java.security.InvalidParameterException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetUserQuotaRequestHandlerTest {
    private PacketSender packetSender;
    private RemoteFacade remoteFacade;

    private static final String PROVIDING_MEMBER = "providingmember";
    private static final String REQUESTING_MEMBER = "requestingmember";

    private RemoteGetUserQuotaRequestHandler remoteGetUserQuotaRequestHandler;

    private static final String EXPECTED_QUOTA = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <query xmlns=\"remoteGetUserQuota\">\n"
            + "    <userQuota>{\"totalQuota\":{\"instances\":1,\"vCPU\":1,\"ram\":1,\"disk\":1},"
            + "\"usedQuota\":{\"instances\":1,\"vCPU\":1,\"ram\":1,\"disk\":1},"
            + "\"availableQuota\":{\"instances\":0,\"vCPU\":0,\"ram\":0,\"disk\":0}}"
            + "</userQuota>\n"
            + "    <userQuotaClassName>cloud.fogbow.ras.api.http.response.quotas.ComputeQuota</userQuotaClassName>\n"
            + "  </query>\n</iq>";

    private static final String IQ_ERROR_RESPONSE = "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Error while sending message to remote RAS.</text>\n"
            + "  </error>\n</iq>";

    @Before
    public void setUp() throws InvalidParameterException {
        this.remoteGetUserQuotaRequestHandler = new RemoteGetUserQuotaRequestHandler();
        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);

        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);
        this.packetSender = Mockito.mock(PacketSender.class);

        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);
    }

    // test case: When the handle method is called passing an IQ request, it must return the User Quota from that.
    @Test
    public void testWithValidIQ() throws Exception {
        // set up
        Quota expectedQuota = this.getQuota();

        Mockito.doReturn(expectedQuota)
                .when(this.remoteFacade)
                .getUserQuota(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        IQ iq = RemoteGetUserQuotaRequest.marshal(PROVIDING_MEMBER, "default", this.createFederationUser(), ResourceType.COMPUTE);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

        // verify
        String expected = String.format(EXPECTED_QUOTA, iq.getID(), SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + PROVIDING_MEMBER, REQUESTING_MEMBER);

        Mockito.verify(this.remoteFacade, Mockito.times(1))
                .getUserQuota(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testUpdateResponseWhenExceptionIsThrown() throws Exception {
        Mockito.when(this.remoteFacade
                .getUserQuota(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenThrow(new RemoteCommunicationException());

        IQ iq = RemoteGetUserQuotaRequest.marshal(PROVIDING_MEMBER, "default", this.createFederationUser(), ResourceType.COMPUTE);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1))
                .getUserQuota(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

        String expected = String.format(IQ_ERROR_RESPONSE, iq.getID(), SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + PROVIDING_MEMBER, REQUESTING_MEMBER);
        Assert.assertEquals(expected, result.toString());
    }

    private Quota getQuota() {
        // set up
        int vCPU = 1;
        int ram = 1;
        int instances = 1;
        int disk = 1;

        ComputeAllocation totalQuota = new ComputeAllocation(vCPU, ram, instances, disk);
        ComputeAllocation usedQuota = new ComputeAllocation(vCPU, ram, instances, disk);
        return new ComputeQuota(totalQuota, usedQuota);
    }

    private SystemUser createFederationUser() {
        return new SystemUser("fake-user-id", "fake-user-name", "fake-token-provider"
        );
    }
}
