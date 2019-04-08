package cloud.fogbow.ras.core.models;

import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.ras.core.models.orders.Order;

import java.util.Objects;

public class RasOperation extends FogbowOperation {

    private Operation operationType;
    private ResourceType resourceType;
    private String cloudName;
    private String genericRequest;
    private Order order;

    public RasOperation(Operation operationType, ResourceType resourceType, String cloudName) {
        this.operationType = operationType;
        this.resourceType = resourceType;
        this.cloudName = cloudName;
    }

    public RasOperation(Operation operationType, ResourceType resourceType, String cloudName, String genericRequest) {
        this.operationType = operationType;
        this.resourceType = resourceType;
        this.cloudName = cloudName;
        this.genericRequest = genericRequest;
    }

    public RasOperation(Operation operationType, ResourceType resourceType, String cloudName, Order order) {
        this.operationType = operationType;
        this.resourceType = resourceType;
        this.cloudName = cloudName;
        this.order = order;
    }

    public RasOperation(Operation operationType, ResourceType resourceType) {
        this.operationType = operationType;
        this.resourceType = resourceType;
    }

    public Operation getOperationType() {
        return operationType;
    }

    public void setOperationType(Operation operationType) {
        this.operationType = operationType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getGenericRequest() {
        return genericRequest;
    }

    public void setGenericRequest(String genericRequest) {
        this.genericRequest = genericRequest;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RasOperation operation = (RasOperation) o;
        return operationType == operation.operationType &&
                resourceType == operation.resourceType &&
                Objects.equals(cloudName, operation.cloudName) &&
                Objects.equals(order, operation.order) &&
                Objects.equals(genericRequest, operation.genericRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType, resourceType, cloudName, order, genericRequest);
    }
}
