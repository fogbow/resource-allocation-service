package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.xmpp.handlers.RemoteTakeSnapshotRequestHandler;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.*;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
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

    public Order getRemoteOrder(Order localOrder) throws FogbowException {
        try {
            RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(localOrder);
            Order remoteOrder = remoteGetOrderRequest.send();
            return remoteOrder;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public String requestInstance(Order order) throws FogbowException {
        try {
            RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(order);
            remoteCreateOrderRequest.send();
            // At the requesting provider, the instance Id should be null, since the instance
            // was not created at the requesting provider's cloud.
            return null;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public void deleteInstance(Order order) throws FogbowException {
        try {
            RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(order);
            remoteDeleteOrderRequest.send();
        } catch (InstanceNotFoundException e) {
            LOGGER.info(Messages.Exception.INSTANCE_NOT_FOUND);
            throw e;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new FogbowException(exceptionMessage);
        }
    }

    @Override
    public OrderInstance getInstance(Order order) throws FogbowException {
        try {
            RemoteGetInstanceRequest remoteGetInstanceRequest = new RemoteGetInstanceRequest(order);
            OrderInstance instance = remoteGetInstanceRequest.send();
            return instance;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationProvider,
                    this.cloudName, systemUser);
            Quota quota = remoteGetUserQuotaRequest.send();
            return quota;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public List<ImageSummary> getAllImages(SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationProvider,
                    this.cloudName, systemUser);
            List<ImageSummary> imagesSummaryList = remoteGetAllImagesRequest.send();
            return imagesSummaryList;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public ImageInstance getImage(String imageId, SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationProvider,
                    this.cloudName, imageId, systemUser);
            ImageInstance imageInstance = remoteGetImageRequest.send();
            return imageInstance;
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, SystemUser systemUser) throws FogbowException {
        try {
            RemoteTakeSnapshotRequest remoteTakeSnapshotRequest = new RemoteTakeSnapshotRequest(computeOrder, name,
                    systemUser);
            remoteTakeSnapshotRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public List<SecurityRuleInstance> getAllSecurityRules(Order order, SystemUser systemUser)
            throws FogbowException {
        try {
            RemoteGetAllSecurityRuleRequest remoteGetAllSecurityRuleRequest =
                    new RemoteGetAllSecurityRuleRequest(this.destinationProvider, order.getId(), systemUser);
            return remoteGetAllSecurityRuleRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
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
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException {
        try {
            RemoteDeleteSecurityRuleRequest remoteDeleteSecurityRuleRequest = new RemoteDeleteSecurityRuleRequest(
                    this.destinationProvider, this.cloudName, securityRuleId, systemUser);
            remoteDeleteSecurityRuleRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new FogbowException(e.getMessage());
        }
    }

    @Override
    public void pauseComputeInstance(Order order) throws FogbowException {
        try {
            RemotePauseOrderRequest remotePauseOrderRequest = new RemotePauseOrderRequest(order);
            remotePauseOrderRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new FogbowException(exceptionMessage);
        }
    }

    @Override
    public void hibernateComputeInstance(Order order) throws FogbowException {
        try {
            RemoteHibernateOrderRequest remoteHibernateOrderRequest = new RemoteHibernateOrderRequest(order);
            remoteHibernateOrderRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new FogbowException(exceptionMessage);
        }
    }

    @Override
    public void stopComputeInstance(Order order) throws FogbowException {
        try {
            RemoteStopOrderRequest remoteStopOrderRequest = new RemoteStopOrderRequest(order);
            remoteStopOrderRequest.send();
        } catch (FogbowException e) {
            LOGGER.error(e.toString(), e);
            throw e;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new FogbowException(exceptionMessage);
        }
    }

    @Override
    public void resumeComputeInstance(Order order) throws FogbowException {
        try {
            RemoteResumeOrderRequest remoteResumeOrderRequest = new RemoteResumeOrderRequest(order);
            remoteResumeOrderRequest.send();
        } catch (FogbowException e) {
        	LOGGER.error(e.toString(), e);
        	throw e;
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            LOGGER.error(exceptionMessage, e);
            throw new FogbowException(exceptionMessage);
        }
    }
}
