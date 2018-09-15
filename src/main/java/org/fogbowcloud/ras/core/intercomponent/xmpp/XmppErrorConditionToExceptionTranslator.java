package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class XmppErrorConditionToExceptionTranslator {

    public static void handleError(IQ response, String memberId) throws Exception {
        if (response == null) {
            throw new UnavailableProviderException(String.format(Messages.Exception.UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDING_MEMBER, memberId));
        } else if (response.getError() != null) {
            PacketError.Condition condition = response.getError().getCondition();
            String message = response.getError().getText();
            XmppErrorConditionToExceptionTranslator.throwException(condition, message);
        }
    }

    private static void throwException(PacketError.Condition condition, String message) throws Exception {
        switch (condition) {
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
