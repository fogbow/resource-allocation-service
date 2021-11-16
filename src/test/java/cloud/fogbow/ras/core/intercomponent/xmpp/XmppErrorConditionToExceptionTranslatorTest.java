package cloud.fogbow.ras.core.intercomponent.xmpp;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.Messages;
import org.junit.Assert;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class XmppErrorConditionToExceptionTranslatorTest {

    private final String providerId = "providerId";
    private final String messageError = "message-error";

    //test case: checks if "handleError" is properly forwarding "UnauthorizedRequestException" from
    //"throwException" when the packet error condition is equals to "forbidden". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsUnauthorizedRequestException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.forbidden, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "UnauthenticatedUserException" from
    //"throwException" when the packet error condition is equals to "not_authorized". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsUnauthenticatedUserException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.not_authorized, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "InvalidParameterException" from
    //"throwException" when the packet error condition is equals to "bad_request". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsInvalidParameterExceptionException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.bad_request, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (InvalidParameterException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "ConfigurationErrorException" from
    //"throwException" when the packet error condition is equals to "conflict". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsConfigurationErrorException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.conflict, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (ConfigurationErrorException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "InstanceNotFoundException" from
    //"throwException" when the packet error condition is equals to "item_not_found". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsInstanceNotFoundException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.item_not_found, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "UnacceptableOperationException" from
    //"throwException" when the packet error condition is equals to "not_acceptable". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsUnacceptableOperationException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.not_acceptable, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (UnacceptableOperationException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "UnavailableProviderException" from
    //"throwException" when the packet error condition is equals to "remote_server_not_found". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsUnavailableProviderException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.remote_server_not_found, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (UnavailableProviderException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }
    
    //test case: checks if "handleError" is properly forwarding "NotImplementedOperationException" from
    //"throwException" when the packet error condition is equals to "feature_not_implemented". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsNotImplementedOperationException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.feature_not_implemented, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (NotImplementedOperationException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "InternalServerErrorException" from
    //"throwException" when the packet error condition is equals to "internal_server_error". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsUnexpectedException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.internal_server_error, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (InternalServerErrorException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "FogbowException" from
    //"throwException" when the packet error condition is not equals to any switch case attribute.
    //In addition, it checks if its message error is correct
    @Test
    public void testHandleErrorThrowsException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.undefined_condition, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (FogbowException e) {
            //verify: if the message is correct
            Assert.assertEquals(e.getClass(), FogbowException.class);
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwarding "UnavailableProviderException" when the response is null
    @Test
    public void testHandleErrorThrowsUnavailableProviderExceptionWhenResponseIsNull() throws Exception {
        try {
            // exercise
            XmppErrorConditionToExceptionTranslator.handleError(null, this.providerId);
            // verify
            Assert.fail();
        } catch (UnavailableProviderException e) {
            String messageExpected = String.format(Messages.Exception.UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDER_S,
                    this.providerId);
            Assert.assertEquals(messageExpected, e.getMessage());
        }
    }

    //test case: checks if nothing happens if there is no error in response
    @Test
    public void testHandleErrorWhenThereIsNoError() throws Exception {
        //set up
        IQ iq = new IQ();
        //exercise//verify
        XmppErrorConditionToExceptionTranslator.handleError(iq, this.providerId);
    }

}
