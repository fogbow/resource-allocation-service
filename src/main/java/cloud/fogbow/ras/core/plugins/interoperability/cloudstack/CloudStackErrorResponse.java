package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

public class CloudStackErrorResponse {

    // TODO(chico) - Refactor: add this constants in the CloudStackConstants class
    private static final String ERROR_CODE_KEY_JSON = "errorcode";
    private static final String ERROR_TEXT_KEY_JSON = "errortext";

    @SerializedName(ERROR_CODE_KEY_JSON)
    private Integer errorCode;

    @SerializedName(ERROR_TEXT_KEY_JSON)
    private String errorText;

    public void checkErrorExistence() throws HttpResponseException {
        if (this.errorCode != null) {
            throw new HttpResponseException(this.errorCode, this.errorText);
        }
    }

}
