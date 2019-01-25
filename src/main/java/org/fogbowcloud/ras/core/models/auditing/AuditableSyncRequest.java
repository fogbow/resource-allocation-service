package org.fogbowcloud.ras.core.models.auditing;

import org.fogbowcloud.ras.core.models.ResourceType;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "sync_request")
public class AuditableSyncRequest {

    @Id
    @GeneratedValue
    private int id;

    @Column
    private Timestamp timestamp;

    @Column
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @Column
    @Enumerated(EnumType.STRING)
    private Operation operation;

    public AuditableSyncRequest(Timestamp timestamp, ResourceType resourceType, Operation operation) {
        this.timestamp = timestamp;
        this.resourceType = resourceType;
        this.operation = operation;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Operation getOperation() {
        return operation;
    }

    public enum Operation {
        CREATE, GET, DELETE
    }

}
