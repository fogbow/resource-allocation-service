package cloud.fogbow.ras.core.models.auditing;

import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "request")
public class AuditableRequest {

    private static final String USER_ID_COLUMN_NAME = "user_id";
    private static final String SYSTEM_IDENTITY_PROVIDER_ID_COLUMN_NAME = "identity_provider_id";
    private static final String RESPONSE_COLUMN_NAME = "response";

    private static final int SYSTEM_IDENTITY_PROVIDER_ID_MAX_SIZE = 255;
    private static final int USER_ID_MAX_SIZE = 255;
    private static final int RESPONSE_MAX_SIZE = 255;

    @Transient
    private final Logger LOGGER = Logger.getLogger(AuditableRequest.class);

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Timestamp timestamp;

    @Enumerated(EnumType.STRING)
    @Column
    private Operation operation;

    @Enumerated(EnumType.STRING)
    @Column
    private ResourceType resourceType;

    @Size(max = USER_ID_MAX_SIZE)
    @Column(name = USER_ID_COLUMN_NAME)
    private String userId;

    @Size(max = SYSTEM_IDENTITY_PROVIDER_ID_MAX_SIZE)
    @Column(name = SYSTEM_IDENTITY_PROVIDER_ID_COLUMN_NAME)
    private String identityProviderId;

    @Size(max = RESPONSE_MAX_SIZE)
    @Column(name = RESPONSE_COLUMN_NAME)
    private String response;

    public AuditableRequest() {

    }

    public AuditableRequest(Timestamp timestamp, Operation operation, ResourceType resourceType, String userId, String identityProviderId, String response) {
        this.timestamp = timestamp;
        this.operation = operation;
        this.resourceType = resourceType;
        this.userId = userId;
        this.identityProviderId = identityProviderId;
        this.response = response;
    }
}
