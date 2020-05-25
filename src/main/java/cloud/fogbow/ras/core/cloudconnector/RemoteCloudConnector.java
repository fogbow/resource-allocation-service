package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.OnGoingOperationException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.*;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import org.apache.log4j.Logger;

import java.util.List;

public class RemoteCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    private String destinationProvider;
    private String cloudName;

    public RemoteCloudConnector(String providerId, String cloudName) {
        this.destinationProvider = providerId;
        this.cloudName = cloudName;
    }

    @Override
    public String requestInstance(Order order) throws FogbowException {
        try {
            RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(order);
            remoteCreateOrderRequest.send();
            // At the requesting provider, the instance Id should be null, since the instance
            // was not created at the requesting provider's cloud.
            return null;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteInstance(Order order) throws FogbowException {
        try {
            RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(order);
            remoteDeleteOrderRequest.send();
        } catch (OnGoingOperationException e) {
            // This may happen if a previous delete was partially completed (successfully completed at the
            // remote provider), but with a failure in the communication when retuning the status to the local
            // provider.
            LOGGER.warn(String.format(Messages.Warn.INSTANCE_S_ALREADY_DELETED_S, order.getId()));
            return;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new RemoteCommunicationException(exceptionMessage, e);
        }
    }

    @Override
    public OrderInstance getInstance(Order order) throws FogbowException {
        try {
            RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
            OrderInstance instance = remoteGetOrderRequest.send();
            return instance;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationProvider,
                    this.cloudName, systemUser);
            Quota quota = remoteGetUserQuotaRequest.send();
            return quota;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public List<ImageSummary> getAllImages(SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationProvider,
                    this.cloudName, systemUser);
            List<ImageSummary> imagesSummaryList = remoteGetAllImagesRequest.send();
            return imagesSummaryList;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public ImageInstance getImage(String imageId, SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationProvider,
                    this.cloudName, imageId, systemUser);
            ImageInstance imageInstance = remoteGetImageRequest.send();
            return imageInstance;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public List<SecurityRuleInstance> getAllSecurityRules(Order order, SystemUser systemUser)
            throws FogbowException {
        try {
            RemoteGetAllSecurityRuleRequest remoteGetAllSecurityRuleRequest =
                    new RemoteGetAllSecurityRuleRequest(this.destinationProvider, order.getId(), systemUser);
            return remoteGetAllSecurityRuleRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser)
            throws FogbowException {
        try {
            RemoteCreateSecurityRuleRequest remoteCreateSecurityRuleRequest = new RemoteCreateSecurityRuleRequest(
                    securityRule, systemUser, this.destinationProvider, order);
            remoteCreateSecurityRuleRequest.send();
            return null;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, SystemUser systemUser)
            throws FogbowException {
        try {
            RemoteDeleteSecurityRuleRequest remoteDeleteSecurityRuleRequest = new RemoteDeleteSecurityRuleRequest(
                    this.destinationProvider, this.cloudName, securityRuleId, systemUser);
            remoteDeleteSecurityRuleRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }
}
