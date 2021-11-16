package cloud.fogbow.ras.core.intercomponent.xmpp;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class XmppExceptionToErrorConditionTranslator {
    private static final Logger LOGGER = Logger.getLogger(XmppExceptionToErrorConditionTranslator.class);

    public static void updateErrorCondition(IQ response, Throwable e) {
        // Exceptions other than FogbowException are possible bugs in the code, and should be logged in error level
        if (!(e instanceof FogbowException)) {
            LOGGER.error(e.getMessage(), e);
        }
        PacketError error = new PacketError(mapExceptionToCondition(e));
        if (e.getMessage() != null) {
            error.setText(e.getMessage());
        } else {
            error.setText(String.format(Messages.Log.UNEXPECTED_ERROR_WITH_MESSAGE_S, e.toString()));
        }
        response.setError(error);
    }

    private static PacketError.Condition mapExceptionToCondition(Throwable e) {
        if (e.getClass() == UnauthorizedRequestException.class) {
            return PacketError.Condition.forbidden;
        } else if (e.getClass() == UnauthenticatedUserException.class) {
            return PacketError.Condition.not_authorized;
        } else if (e.getClass() == InvalidParameterException.class) {
            return PacketError.Condition.bad_request;
        } else if (e.getClass() == InstanceNotFoundException.class) {
            return PacketError.Condition.item_not_found;
        } else if (e.getClass() == UnacceptableOperationException.class) {
            return PacketError.Condition.not_acceptable;
        } else if (e.getClass() == UnavailableProviderException.class) {
            return PacketError.Condition.remote_server_not_found;
        } else if (e.getClass() == ConfigurationErrorException.class) {
            return PacketError.Condition.conflict;
        } else if (e.getClass() == InternalServerErrorException.class) {
            return PacketError.Condition.internal_server_error;
        } else if (e.getClass() == NotImplementedOperationException.class) {
            return PacketError.Condition.feature_not_implemented;
        }
        return PacketError.Condition.undefined_condition;
    }
}
