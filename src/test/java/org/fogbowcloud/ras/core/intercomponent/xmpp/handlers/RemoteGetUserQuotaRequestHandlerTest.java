package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteGetUserQuotaRequest;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetUserQuotaRequestHandlerTest {
    private PacketSender packetSender;
    private RemoteFacade remoteFacade;

    private static final String FED_USER_ID = "fake-id";
    private static final String PROVIDING_MEMBER = "providingmember";

    private RemoteGetUserQuotaRequestHandler remoteGetUserQuotaRequestHandler;

    private static final String EXPECTED_QUOTA = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n"
            + "  <query xmlns=\"remoteGetUserQuota\">\n"
            + "    <userQuota>{\"totalQuota\":{\"vCPU\":1,\"ram\":1,\"instances\":1},"
            + "\"usedQuota\":{\"vCPU\":1,\"ram\":1,\"instances\":1},"
            + "\"availableQuota\":{\"vCPU\":0,\"ram\":0,\"instances\":0}}"
            + "</userQuota>\n"
            + "    <userQuotaClassName>org.fogbowcloud.ras.core.models.quotas.ComputeQuota</userQuotaClassName>\n"
            + "  </query>\n</iq>";

    private static final String IQ_ERROR_RESPONSE = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exception: java.lang.Exception</text>\n"
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
                .getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any());

        IQ iq = RemoteGetUserQuotaRequest.marshal(PROVIDING_MEMBER, this.createFederationUserToken(), ResourceType.COMPUTE);

        // exercise
        IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

        // verify
        String expected = String.format(EXPECTED_QUOTA, iq.getID(), PROVIDING_MEMBER);

        Mockito.verify(this.remoteFacade, Mockito.times(1))
                .getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testUpdateResponseWhenExceptionIsThrown() throws Exception {
        Mockito.when(this.remoteFacade
                .getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenThrow(new Exception());

        IQ iq = RemoteGetUserQuotaRequest.marshal(PROVIDING_MEMBER, this.createFederationUserToken(), ResourceType.COMPUTE);

        // exercise
        IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1))
                .getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

        String expected = String.format(IQ_ERROR_RESPONSE, iq.getID(), PROVIDING_MEMBER);
        Assert.assertEquals(expected, result.toString());
    }

    private Quota getQuota() {
        // set up
        int vCPU = 1;
        int ram = 1;
        int instances = 1;

        ComputeAllocation totalQuota = new ComputeAllocation(vCPU, ram, instances);
        ComputeAllocation usedQuota = new ComputeAllocation(vCPU, ram, instances);
        return new ComputeQuota(totalQuota, usedQuota);
    }

    private FederationUserToken createFederationUserToken() {
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");
        return federationUserToken;
    }
}