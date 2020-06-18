package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.connectivity.HttpErrorConditionToFogbowExceptionMapper;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.ERROR_CODE_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.ERROR_TEXT_KEY_JSON;

public class CloudStackErrorResponse {

    @SerializedName(ERROR_CODE_KEY_JSON)
    private Integer errorCode;

    @SerializedName(ERROR_TEXT_KEY_JSON)
    private String errorText;

    public void checkErrorExistence() throws FogbowException {
        if (this.errorCode != null) {
            throw HttpErrorConditionToFogbowExceptionMapper.map(this.errorCode.intValue(), this.errorText);
        }
    }

    public String getErrorText() {
        return errorText;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
