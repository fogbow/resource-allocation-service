package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import static cloud.fogbow.common.constants.CloudStackConstants.ERROR_CODE_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.ERROR_TEXT_KEY_JSON;

public class CloudStackErrorResponse {

    @SerializedName(ERROR_CODE_KEY_JSON)
    private Integer errorCode;

    @SerializedName(ERROR_TEXT_KEY_JSON)
    private String errorText;

    public void checkErrorExistence() throws HttpResponseException {
        if (this.errorCode != null) {
            throw new HttpResponseException(this.errorCode, this.errorText);
        }
    }

    public String getErrorText() {
        return errorText;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
