package org.fogbowcloud.manager.core.intercomponent.xmpp;

import org.fogbowcloud.manager.core.exceptions.*;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class ErrorConditionToExceptionTranslator {

    public static void handleError(IQ response, String memberId) throws Exception {
        if (response == null) {
            String message = "Unable to retrieve the response from providing member: " + memberId;
            throw new UnavailableProviderException(message);
        } else if (response.getError() != null) {
            PacketError.Condition condition = response.getError().getCondition();
            String message = response.getError().getText();
            ErrorConditionToExceptionTranslator.throwException(condition, message);
        }
    }

    private static void throwException(PacketError.Condition condition, String message) throws Exception {
        switch(condition) {
            case forbidden:
                throw new UnauthorizedRequestException(message);
            case not_authorized:
                throw new UnauthenticatedUserException(message);
            case bad_request:
                throw new InvalidParameterException(message);
            case item_not_found:
                throw new InstanceNotFoundException(message);
            case conflict:
                throw new QuotaExceededException(message);
            case not_acceptable:
                throw new NoAvailableResourcesException(message);
            case remote_server_not_found:
                throw new UnavailableProviderException(message);
            case internal_server_error:
                throw new UnexpectedException(message);
            default:
                throw new Exception(message);
        }
    }
}
