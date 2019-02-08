package cloud.fogbow.ras.core.models.auditing;

import cloud.fogbow.ras.core.models.StorableBean;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "request")
public class AuditableRequest extends StorableBean {

    private static final String ENDPOINT_COLUMN_NAME = "endpoint";
    private static final String USER_ID_COLUMN_NAME = "user_id";
    private static final String TOKEN_PROVIDER_ID_COLUMN_NAME = "token_provider_id";

    private static final int TOKEN_PROVIDER_ID_MAX_SIZE = 255;
    private static final int USER_ID_MAX_SIZE = 255;
    private static final int ENDPOINT_MAX_SIZE = 1024;

    @Transient
    private final Logger LOGGER = Logger.getLogger(AuditableRequest.class);

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Timestamp timestamp;

    @Size(max = ENDPOINT_MAX_SIZE)
    @Column(name = ENDPOINT_COLUMN_NAME)
    private String endpoint;

    @Size(max = USER_ID_MAX_SIZE)
    @Column(name = USER_ID_COLUMN_NAME)
    private String userId;

    @Size(max = TOKEN_PROVIDER_ID_MAX_SIZE)
    @Column(name = TOKEN_PROVIDER_ID_COLUMN_NAME)
    private String tokenProviderId;

    @Column
    private int responseCode;

    public AuditableRequest(Timestamp currentTimestamp, String endpoint, String userId, String tokenProviderId, int responseCode) {
        this.timestamp = currentTimestamp;
        this.endpoint = endpoint;
        this.userId = userId;
        this.tokenProviderId = tokenProviderId;
        this.responseCode = responseCode;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @PrePersist
    private void checkColumnsSizes() {
        this.endpoint = treatValue(this.endpoint, ENDPOINT_COLUMN_NAME, ENDPOINT_MAX_SIZE);
        this.userId = treatValue(this.userId, USER_ID_COLUMN_NAME, USER_ID_MAX_SIZE);
        this.tokenProviderId = treatValue(this.tokenProviderId, TOKEN_PROVIDER_ID_COLUMN_NAME, TOKEN_PROVIDER_ID_MAX_SIZE);
    }
}
