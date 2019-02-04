package cloud.fogbow.ras.core.models.auditing;

import javax.persistence.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import java.sql.Timestamp;

@Entity
@Table(name = "request")
public class AuditableRequest {

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
    private int responseCode;

    public AuditableRequest(Timestamp currentTimestamp, String endpoint, String userId, String tokenProviderId, int responseCode) {
        this.timestamp = currentTimestamp;
        this.endpoint = endpoint;
        this.userId = userId;
        this.tokenProviderId = tokenProviderId;
        this.responseCode = responseCode;
    }

}
