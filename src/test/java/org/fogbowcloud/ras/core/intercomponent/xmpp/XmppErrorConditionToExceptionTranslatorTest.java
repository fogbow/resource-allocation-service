package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.junit.Assert;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class XmppErrorConditionToExceptionTranslatorTest {

    private final String memberId = "memberId";
    private final String messageError = "message-error";

    //test case: checks if "handleError" is properly forwading "UnauthorizedRequestException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
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

    //test case: checks if "handleError" is properly forwading "UnauthenticatedUserException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
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

    //test case: checks if "handleError" is properly forwading "InvalidParameterExceptionException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
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

    //test case: checks if "handleError" is properly forwading "InstanceNotFoundException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
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

    //test case: checks if "handleError" is properly forwading "QuotaExceededException" from
    //"throwException" when the packet error condition is equals to "conflict". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsQuotaExceededException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.conflict, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (QuotaExceededException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwading "NoAvailableResourcesException" from
    //"throwException" when the packet error condition is equals to "not_acceptable". In addition, it checks
    //if its message error is correct
    @Test
    public void testHandleErrorThrowsNoAvailableResourcesException() {
        //set up
        IQ iq = new IQ();
        PacketError packetError = new PacketError(PacketError.Condition.not_acceptable, null, this.messageError);
        iq.setError(packetError);

        try {
            //exercise
            XmppErrorConditionToExceptionTranslator.handleError(iq, memberId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (NoAvailableResourcesException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwading "UnavailableProviderException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
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

    //test case: checks if "handleError" is properly forwading "UnexpectedException" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (UnexpectedException e) {
            //verify: if the message is correct
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwading "Exception" from
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
            XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
            //verify: if some exception occurred
            Assert.fail();
        } catch (Exception e) {
            //verify: if the message is correct
            Assert.assertEquals(e.getClass(), Exception.class);
            Assert.assertEquals(this.messageError, e.getMessage());
        } catch (Throwable e) {
            //verify: if some exception different from the expected exception occurred
            Assert.fail();
        }
    }

    //test case: checks if "handleError" is properly forwading "UnavailableProviderException" when the response is null
    @Test
    public void testHandleErrorThrowsUnavailableProviderExceptionWhenResponseIsNull() throws Exception {
        try {
            // exercise
            XmppErrorConditionToExceptionTranslator.handleError(null, this.memberId);
            // verify
            Assert.fail();
        } catch (UnavailableProviderException e) {
            String messageExpected = String.format(Messages.Exception.UNABLE_RETRIEVE_RESPONSE_FROM_PROVIDING_MEMBER,
                    this.memberId);
            Assert.assertEquals(messageExpected, e.getMessage());
        }
    }

    //test case: checks if nothing happens if there is no error in response
    @Test
    public void testHandleErrorWhenThereIsNoError() throws Exception {
        //set up
        IQ iq = new IQ();
        //exercise//verify
        XmppErrorConditionToExceptionTranslator.handleError(iq, this.memberId);
    }

}
