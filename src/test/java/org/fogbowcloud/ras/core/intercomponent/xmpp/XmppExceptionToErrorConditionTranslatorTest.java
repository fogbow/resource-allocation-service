package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.fogbowcloud.ras.core.exceptions.*;
import org.junit.Assert;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;


public class XmppExceptionToErrorConditionTranslatorTest {

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // forbidden when the exception is equal to UnauthorizedRequestException
    @Test
    public void testUpdateErrorWhenUnauthorizedRequestException() {
        // set up
        IQ response = new IQ();
        Throwable e = new UnauthorizedRequestException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.forbidden);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // not_authorized when the exception is equal to UnauthenticatedUserException
    @Test
    public void testUpdateErrorWhenUnauthenticatedUserException() {
        // set up
        IQ response = new IQ();
        Throwable e = new UnauthenticatedUserException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.not_authorized);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // bad_request when the exception is equal to InvalidParameterException
    @Test
    public void testUpdateErrorWhenInvalidParameterException() {
        // set up
        IQ response = new IQ();
        Throwable e = new InvalidParameterException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.bad_request);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // item_not_found when the exception is equal to InstanceNotFoundException
    @Test
    public void testUpdateErrorWhenInstanceNotFoundException() {
        // set up
        IQ response = new IQ();
        Throwable e = new InstanceNotFoundException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.item_not_found);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // conflict when the exception is equal to QuotaExceededException
    @Test
    public void testUpdateErrorWhenQuotaExceededException() {
        // set up
        IQ response = new IQ();
        Throwable e = new QuotaExceededException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.conflict);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // not_acceptable when the exception is equal to NoAvailableResourcesException
    @Test
    public void testUpdateErrorWhenNoAvailableResourcesException() {
        // set up
        IQ response = new IQ();
        Throwable e = new NoAvailableResourcesException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.not_acceptable);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // remote_server_not_found when the exception is equal to UnavailableProviderException
    @Test
    public void testUpdateErrorWhenUnavailableProviderException() {
        // set up
        IQ response = new IQ();
        Throwable e = new UnavailableProviderException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.remote_server_not_found);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // internal_server_error when the exception is equal to UnexpectedException
    @Test
    public void testUpdateErrorWhenUnexpectedException() {
        // set up
        IQ response = new IQ();
        Throwable e = new UnexpectedException();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.internal_server_error);
    }

    // test case: checks if "updateErrorCondition" sets PacketError condition to
    // undefined_condition when the exception is equal to Exception
    @Test
    public void testUpdateErrorWhenUndefinedException() {
        // set up
        IQ response = new IQ();
        Throwable e = new Exception();
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(response.getError().getCondition(), PacketError.Condition.undefined_condition);
    }

    // test case: checks if the PacketError condition message is equal to the exception
    @Test
    public void testUpdateErrorWhenExceptionMessageIsNotEqualToNull() {
        // set up
        IQ response = new IQ();
        String message = "minha-mensagem";
        Throwable e = new Throwable(message);
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(message, response.getError().getText());
    }

    // test case: checks if the PacketError condition message is correct when there is no message in exception
    @Test
    public void testUpdateErrorWhenExceptionMessageIsEqualToNull() {
        // set up
        IQ response = new IQ();
        Throwable e = new Throwable();
        String message = "Unexpected exception error: " + e.toString() + ".";
        // exercise
        XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        // verify
        Assert.assertEquals(message, response.getError().getText());
    }

}
