package org.fogbowcloud.manager.core.intercomponent.xmpp;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class ExceptionToErrorConditionTranslator {
    private static final Logger LOGGER = Logger.getLogger(ExceptionToErrorConditionTranslator.class);

    public static void updateErrorCondition(IQ response, Throwable e) {
        // FogbowManagerExceptions are part of the business logic and should be logged in debug level
        // Other exceptions mean a possible bug in the code, and should be logged in error level
        if (e instanceof FogbowManagerException) {
            LOGGER.debug(e.getMessage(), e);
        } else {
            LOGGER.error(e.getMessage(), e);
        }
        PacketError error = new PacketError(ExceptionToErrorConditionTranslator.mapExceptionToCondition(e));
        if (e.getMessage() != null) {
            error.setText(e.getMessage());
        } else {
            error.setText("Unexpected exception" + e.toString());
        }
        response.setError(error);
    }

    private static PacketError.Condition mapExceptionToCondition(Throwable e) {
        if (e instanceof UnauthorizedRequestException) {
            return PacketError.Condition.forbidden;
        } else if (e instanceof UnauthenticatedUserException) {
            return PacketError.Condition.not_authorized;
        } else if (e instanceof InvalidParameterException) {
            return PacketError.Condition.bad_request;
        } else if (e instanceof InstanceNotFoundException) {
            return PacketError.Condition.item_not_found;
        } else if (e instanceof QuotaExceededException) {
            return PacketError.Condition.conflict;
        } else if (e instanceof NoAvailableResourcesException) {
            return PacketError.Condition.not_acceptable;
        } else if (e instanceof UnavailableProviderException) {
            return PacketError.Condition.remote_server_not_found;
        } else if (e instanceof UnexpectedException) {
            return PacketError.Condition.internal_server_error;
        } else {
            return PacketError.Condition.undefined_condition;
        }
    }
}
