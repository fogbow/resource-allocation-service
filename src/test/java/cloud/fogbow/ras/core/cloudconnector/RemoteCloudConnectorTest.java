package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

// TODO - Finish tests implementation
@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudConnectorFactory.class,
        DatabaseManager.class,
        PacketSenderHolder.class,
        XmppErrorConditionToExceptionTranslator.class})
public class RemoteCloudConnectorTest extends TestUtils {

    private RemoteCloudConnector remoteCloudConnector;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(RemoteCloudConnector.class);

    @Before
    public void setUp() {
        this.remoteCloudConnector = new RemoteCloudConnector(TestUtils.ANY_VALUE, TestUtils.ANY_VALUE);
    }

    // test case: When calling the deleteInstance method and everything happens OK,
    // it must verify if It do not throw exception.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        ComputeOrder order = this.createLocalComputeOrder();
        PacketSender packetSender = buildPacketSender();
        IQ IQResponse = Mockito.mock(IQ.class);
        Mockito.when(packetSender.syncSendPacket(Mockito.any(Packet.class))).thenReturn(IQResponse);

        // exercise
        this.remoteCloudConnector.deleteInstance(order);

        // verify
        this.loggerTestChecking.verifyIfEmpty();
    }

    // test case: When calling the deleteInstance method and occurs a generic exception in the communication,
    // it must verify if It do not throw exception.
    @Test
    public void testDeleteInstanceFailWhenThrowAGenericException() throws Exception {
        // set up
        ComputeOrder order = this.createLocalComputeOrder();
        PacketSender packetSender = buildPacketSender();
        IQ IQResponse = Mockito.mock(IQ.class);
        Mockito.when(packetSender.syncSendPacket(Mockito.any(Packet.class))).thenReturn(IQResponse);

        String exceptionMessageExpected = TestUtils.ANY_VALUE;
        Exception exception = new Exception(exceptionMessageExpected);
        PowerMockito.mockStatic(XmppErrorConditionToExceptionTranslator.class);
        PowerMockito.doThrow(exception).when(XmppErrorConditionToExceptionTranslator.class);
        XmppErrorConditionToExceptionTranslator.handleError(Mockito.eq(IQResponse), Mockito.eq(order.getProvider()));

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(exceptionMessageExpected);

        // exercise
        this.remoteCloudConnector.deleteInstance(order);
    }

    private PacketSender buildPacketSender() {
        PacketSender packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        PowerMockito.when(PacketSenderHolder.getPacketSender()).thenReturn(packetSender);
        return packetSender;
    }

}
