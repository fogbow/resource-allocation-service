package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.*;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class RemoteCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    private String destinationMember;
    private String cloudName;

    public RemoteCloudConnector(String memberId, String cloudName) {
        this.destinationMember = memberId;
        this.cloudName = cloudName;
    }

    public RemoteCloudConnector(String memberId) {
        this(memberId, "");
    }

    @Override
    public String requestInstance(Order order) throws FogbowException {
        try {
            RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(order);
            remoteCreateOrderRequest.send();
            // At the requesting member, the instance Id should be null, since the instance
            // was not created at the requesting member's cloud.
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
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowException {
        try {
            RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
            Instance instance = remoteGetOrderRequest.send();
            return instance;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser, ResourceType resourceType) throws FogbowException {
        try {
            RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationMember,
                    this.cloudName, systemUser, resourceType);
            Quota quota = remoteGetUserQuotaRequest.send();
            return quota;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public HashMap<String, String> getAllImages(SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationMember,
                    this.cloudName, systemUser);
            HashMap<String, String> imagesMap = remoteGetAllImagesRequest.send();
            return imagesMap;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public Image getImage(String imageId, SystemUser systemUser) throws FogbowException {
        try {
            RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationMember,
                    this.cloudName, imageId, systemUser);
            Image image = remoteGetImageRequest.send();
            return image;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public FogbowGenericResponse genericRequest(FogbowGenericRequest genericRequest, SystemUser systemUserToken)
            throws FogbowException {
        try {
            RemoteGenericRequest remoteGenericRequest = new RemoteGenericRequest(this.destinationMember, this.cloudName,
                    genericRequest, systemUserToken);
            FogbowGenericResponse fogbowGenericResponse = remoteGenericRequest.send();
            return fogbowGenericResponse;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public List<SecurityRule> getAllSecurityRules(Order order, SystemUser systemUser)
            throws FogbowException {
        try {
            RemoteGetAllSecurityRuleRequest remoteGetAllSecurityRuleRequest =
                    new RemoteGetAllSecurityRuleRequest(this.destinationMember, order.getId(), systemUser);
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
                    securityRule, systemUser, this.destinationMember, order);
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
                    this.destinationMember, this.cloudName, securityRuleId, systemUser);
            remoteDeleteSecurityRuleRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }
}
