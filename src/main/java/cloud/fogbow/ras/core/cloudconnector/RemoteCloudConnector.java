package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.*;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.images.Image;
import cloud.fogbow.ras.core.models.instances.Instance;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.quotas.Quota;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
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
    public Quota getUserQuota(FederationUser federationUser, ResourceType resourceType) throws FogbowException {
        try {
            RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationMember,
                    this.cloudName, federationUser, resourceType);
            Quota quota = remoteGetUserQuotaRequest.send();
            return quota;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public HashMap<String, String> getAllImages(FederationUser federationUser) throws FogbowException {
        try {
            RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationMember,
                    this.cloudName, federationUser);
            HashMap<String, String> imagesMap = remoteGetAllImagesRequest.send();
            return imagesMap;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public Image getImage(String imageId, FederationUser federationUser) throws FogbowException {
        try {
            RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationMember,
                    this.cloudName, imageId, federationUser);
            Image image = remoteGetImageRequest.send();
            return image;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public GenericRequestResponse genericRequest(GenericRequest genericRequest, FederationUser federationUserToken)
            throws FogbowException {
        try {
            RemoteGenericRequest remoteGenericRequest = new RemoteGenericRequest(this.destinationMember, this.cloudName,
                    genericRequest, federationUserToken);
            GenericRequestResponse genericRequestResponse = remoteGenericRequest.send();
            return genericRequestResponse;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public List<SecurityRule> getAllSecurityRules(Order order, FederationUser federationUser)
            throws FogbowException {
        try {
            RemoteGetAllSecurityRuleRequest remoteGetAllSecurityRuleRequest =
                    new RemoteGetAllSecurityRuleRequest(this.destinationMember, order.getId(), federationUser);
            return remoteGetAllSecurityRuleRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public String requestSecurityRule(Order order, SecurityRule securityRule, FederationUser federationUser)
            throws FogbowException {
        try {
            RemoteCreateSecurityRuleRequest remoteCreateSecurityRuleRequest = new RemoteCreateSecurityRuleRequest(
                    securityRule, federationUser, this.destinationMember, order);
            remoteCreateSecurityRuleRequest.send();
            return null;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, FederationUser federationUser)
            throws FogbowException {
        try {
            RemoteDeleteSecurityRuleRequest remoteDeleteSecurityRuleRequest = new RemoteDeleteSecurityRuleRequest(
                    this.destinationMember, this.cloudName, securityRuleId, federationUser);
            remoteDeleteSecurityRuleRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }
}
