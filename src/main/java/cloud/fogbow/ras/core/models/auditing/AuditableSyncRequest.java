package cloud.fogbow.ras.core.models.auditing;

import cloud.fogbow.ras.core.models.ResourceType;

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
    private final String endpoint;

    @Column
    private final String userId;

    @Column
    private final String tokenProviderId;

    @Column
    private final String tokenValue;

    @Column
    private String response;

    public AuditableSyncRequest(Timestamp currentTimestamp, String endpoint, String userId, String tokenProviderId, String tokenValue, String response) {
        this.timestamp = currentTimestamp;
        this.endpoint = endpoint;
        this.userId = userId;
        this.tokenProviderId = tokenProviderId;
        this.tokenValue = tokenValue;
        this.response = response;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public enum Operation {
        CREATE, GET, DELETE
    }

}
